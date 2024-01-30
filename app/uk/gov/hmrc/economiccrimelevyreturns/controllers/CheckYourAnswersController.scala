/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionKeys._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{EmailSubmissionError, ResponseError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.{EmailService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReturnPdfView, CheckYourAnswersView, ErrorTemplate}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import java.util.Base64
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  getReturnData: DataRetrievalAction,
  returnsService: ReturnsService,
  sessionService: SessionService,
  emailService: EmailService,
  amendReturnPdfView: AmendReturnPdfView,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  appConfig: AppConfig
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  private def eclDetails()(implicit request: ReturnDataRequest[_]): SummaryList = SummaryListViewModel(
    rows = Seq(
      EclReferenceNumberSummary.row(),
      RelevantAp12MonthsSummary.row(),
      RelevantApLengthSummary.row(),
      UkRevenueSummary.row(),
      AmlRegulatedActivitySummary.row(),
      AmlRegulatedActivityLengthSummary.row(),
      CalculatedBandSummary.row(),
      AmountDueSummary.row()
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  private def contactDetails()(implicit request: ReturnDataRequest[_]): SummaryList = SummaryListViewModel(
    rows = Seq(
      ContactNameSummary.row(),
      ContactRoleSummary.row(),
      ContactEmailSummary.row(),
      ContactNumberSummary.row()
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  private def amendReasonDetails()(implicit request: ReturnDataRequest[_]): SummaryList = SummaryListViewModel(
    rows = Seq(
      AmendReasonSummary.row()
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def onPageLoad: Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    (for {
      errors <- returnsService.getReturnValidationErrors(request.internalId)(hc).asResponseError
    } yield errors)
      .fold(
        error => Future.successful(routeError(error)),
        {
          case Some(_) => Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
          case None    =>
            viewHtmlOrError().fold(
              error => routeError(error),
              view => Ok(view)
            )
        }
      )
      .flatten
  }

  def onSubmit: Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)

    (for {
      viewHtml <- viewHtmlOrError()
    } yield viewHtml)
      .fold(
        error => Future.successful(routeError(error)),
        viewHtml => {
          val base64EncodedHtmlView: String = base64EncodeHtmlView(viewHtml.body)

          (for {
            pdfViewHtml <- pdfViewHtmlOrError
          } yield pdfViewHtml)
            .fold(
              error => Future.successful(routeError(error)),
              pdfViewHtml => {

                val updatedReturn = request.eclReturn.copy(
                  base64EncodedNrsSubmissionHtml = Some(base64EncodedHtmlView),
                  base64EncodedDmsSubmissionHtml = pdfViewHtml
                )

                (for {
                  _        <- returnsService.upsertReturn(eclReturn = updatedReturn).asResponseError
                  response <- returnsService.submitReturn(request.internalId).asResponseError
                  _         = sendConfirmationMail(request.eclReturn, response)
                  _        <- returnsService.deleteReturn(request.internalId).asResponseError
                  _         = sessionService.delete(request.internalId)
                } yield response).fold(
                  _ => Redirect(routes.NotableErrorController.answersAreInvalid()),
                  response => getRedirectionRoute(request, response)
                )
              }
            )
            .flatten
        }
      )
      .flatten
  }

  private def pdfViewHtmlOrError()(implicit request: ReturnDataRequest[AnyContent]): EitherT[Future, ResponseError, Option[String]] = {
    request.eclReturn.returnType match {
      case Some(AmendReturn)     => {

      }
      case Some(FirstTimeReturn) =>
        EitherT[Future, ResponseError, Option[String]](Future.successful(Right(None)))
    }
  }

  private def sendConfirmationMail(eclReturn: EclReturn, response: SubmitEclReturnResponse)(implicit
    messages: Messages,
    hc: HeaderCarrier
  ): EitherT[Future, EmailSubmissionError, Unit] =
    eclReturn.returnType match {
      case Some(FirstTimeReturn) => emailService.sendReturnSubmittedEmail(eclReturn, response.chargeReference)
      case Some(AmendReturn)     => emailService.sendAmendReturnConfirmationEmail(eclReturn)
      case None                  =>
        EitherT.left(
          Future.successful(
            EmailSubmissionError
              .InternalUnexpectedError(None, Some("Return type is missing in session"))
          )
        )
    }

  private def checkOptionalVal[T](value: Option[T]) =
    if (value.isDefined) {
      Right(value.get)
    } else {
      Left(Redirect(routes.NotableErrorController.answersAreInvalid()))
    }

  private def getRedirectionRoute(request: ReturnDataRequest[AnyContent], response: SubmitEclReturnResponse) = {
    val containsEmailAddress = checkOptionalVal(request.eclReturn.contactEmailAddress)
    request.eclReturn.returnType match {
      case Some(AmendReturn) =>
        containsEmailAddress match {
          case Right(email)    =>
            val session = request.session.clearEclValues ++ Seq(
              SessionKeys.Email             -> email,
              SessionKeys.ObligationDetails -> Json.toJson(request.eclReturn.obligationDetails).toString()
            )

            Redirect(routes.AmendReturnSubmittedController.onPageLoad()).withSession(session)
          case Left(errorPage) => errorPage
        }
      case _                 =>
        containsEmailAddress match {
          case Right(email)    =>
            checkOptionalVal(request.eclReturn.calculatedLiability) match {
              case Right(calculatedLiability) =>
                val session =
                  request.session.clearEclValues ++ response.chargeReference.fold(Seq.empty[(String, String)])(c =>
                    Seq(SessionKeys.ChargeReference -> c)
                  ) ++ Seq(
                    SessionKeys.Email             -> email,
                    SessionKeys.ObligationDetails -> Json.stringify(
                      Json.toJson(request.eclReturn.obligationDetails)
                    ),
                    SessionKeys.AmountDue         ->
                      calculatedLiability.amountDue.amount.toString()
                  )

                Redirect(routes.ReturnSubmittedController.onPageLoad()).withSession(session)
              case Left(errorPage)            => errorPage
            }
          case Left(errorPage) => errorPage
        }
    }
  }

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

  private def createAndEncodeHtmlForPdf()(implicit request: ReturnDataRequest[_]): String = {
    val date         = LocalDate.now
    val organisation = eclDetails()
    val contact      = contactDetails()
    val amendReason  = amendReasonDetails()
    base64EncodeHtmlView(
      amendReturnPdfView(
        ViewUtils.formatLocalDate(date),
        organisation.copy(rows = organisation.rows.map(_.copy(actions = None))),
        contact.copy(rows = contact.rows.map(_.copy(actions = None))),
        amendReason.copy(
          rows = amendReason.rows.map(_.copy(actions = None)),
          attributes = Map("id" -> "amendReason")
        )
      ).toString()
    )
  }

  private def viewHtmlOrError()(implicit
    request: ReturnDataRequest[AnyContent]
  ): EitherT[Future, ResponseError, HtmlFormat.Appendable] =
    if (appConfig.getEclReturnEnabled) {
      EitherT {
        request.periodKey match {
          case None            =>
            Future.successful(Left(ResponseError.internalServiceError("Unable to find period key")))
          case Some(periodKey) =>
            viewModelWithEclReturnSubmission(periodKey).fold(
              error => Left(error),
              viewModel => Right(view(viewModel))
            )
        }
      }
    } else {
      EitherT[Future, ResponseError, HtmlFormat.Appendable] {
        Future.successful(Right(view(viewModelWithoutEclReturnSubmission())))
      }
    }

  private def viewModelWithEclReturnSubmission(periodKey: String)(implicit
    hc: HeaderCarrier,
    request: ReturnDataRequest[AnyContent]
  ): EitherT[Future, ResponseError, CheckYourAnswersViewModel] =
    EitherT {
      (for {
        eclReturnSubmission <-
          returnsService.getEclReturnSubmission(periodKey, request.eclRegistrationReference).asResponseError
      } yield eclReturnSubmission).fold(
        error => Left(error),
        eclReturnSubmission =>
          Right(
            CheckYourAnswersViewModel(
              eclReturn = request.eclReturn,
              eclReturnSubmission = Some(eclReturnSubmission),
              startAmendUrl = request.startAmendUrl
            )
          )
      )
    }

  private def viewModelWithoutEclReturnSubmission()(implicit
    request: ReturnDataRequest[AnyContent]
  ): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      eclReturn = request.eclReturn,
      eclReturnSubmission = None,
      startAmendUrl = request.startAmendUrl
    )
}
