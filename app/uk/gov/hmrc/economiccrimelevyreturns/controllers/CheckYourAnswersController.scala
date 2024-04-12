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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalOrErrorAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionKeys._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{EmailSubmissionError, ResponseError}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.{EmailService, RegistrationService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReturnPdfView, CheckYourAnswersView, ErrorTemplate}
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
  getReturnData: DataRetrievalOrErrorAction,
  returnsService: ReturnsService,
  sessionService: SessionService,
  emailService: EmailService,
  pdfView: AmendReturnPdfView,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  appConfig: AppConfig,
  storeUrl: StoreUrlAction,
  registrationService: RegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getReturnData andThen storeUrl).async { implicit request =>
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
      viewHtml     <- viewHtmlOrError()
      pdfViewHtml  <- pdfViewHtmlOrError()
      updatedReturn = request.eclReturn.copy(
                        base64EncodedNrsSubmissionHtml = Some(base64EncodeHtmlView(viewHtml.body)),
                        base64EncodedDmsSubmissionHtml = pdfViewHtml.map(html => base64EncodeHtmlView(html.body))
                      )
      _            <- returnsService.upsertReturn(eclReturn = updatedReturn).asResponseError
      response     <- returnsService.submitReturn(request.internalId).asResponseError
      _             = sendConfirmationMail(request.eclReturn, response, request.eclRegistrationReference)
    } yield response).foldF(
      error => Future.successful(routeError(error)),
      response => getRedirectionRoute(response)
    )
  }

  private def pdfViewHtmlOrError()(implicit
    request: ReturnDataRequest[AnyContent]
  ): EitherT[Future, ResponseError, Option[HtmlFormat.Appendable]] =
    EitherT {
      request.eclReturn.returnType match {
        case Some(FirstTimeReturn) => Future.successful(Right(None))
        case Some(AmendReturn)     =>
          if (appConfig.getEclReturnEnabled) {
            request.periodKey match {
              case None            =>
                Future.successful(Left(ResponseError.internalServiceError("Unable to find period key")))
              case Some(periodKey) =>
                pdfViewModelWithEclReturnSubmission(periodKey).fold(
                  error => Left(error),
                  viewModel => Right(Some(pdfView(viewModel, appConfig)))
                )
            }
          } else {
            Future.successful(Right(Some(pdfView(pdfViewModelWithoutEclReturnSubmission(), appConfig))))
          }
        case None                  => Future.successful(Left(ResponseError.badRequestError("Return type is empty")))
      }
    }

  private def sendConfirmationMail(eclReturn: EclReturn, response: SubmitEclReturnResponse, eclReference: String)(
    implicit
    messages: Messages,
    hc: HeaderCarrier
  ) =
    eclReturn.returnType match {
      case Some(FirstTimeReturn)                              => emailService.sendReturnSubmittedEmail(eclReturn, response.chargeReference)
      case Some(AmendReturn) if appConfig.getEclReturnEnabled =>
        sendEmailWithContactDetails(eclReturn, eclReference)

      case Some(AmendReturn) => emailService.sendAmendReturnConfirmationEmail(eclReturn, None)
      case None              =>
        EitherT.left(
          Future.successful(
            EmailSubmissionError
              .InternalUnexpectedError(None, Some("Return type is missing in session"))
          )
        )
    }

  private def sendEmailWithContactDetails(eclReturn: EclReturn, eclReference: String)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): EitherT[Future, ResponseError, Unit] =
    for {
      subscription <- registrationService.getSubscription(eclReference).asResponseError
      _             = emailService
                        .sendAmendReturnConfirmationEmail(eclReturn, Some(subscription.correspondenceAddressDetails))

    } yield ()

  private def checkOptionalVal[T](value: Option[T]): Either[Result, T] =
    if (value.isDefined) {
      Right(value.get)
    } else {
      Left(Redirect(routes.NotableErrorController.answersAreInvalid()))
    }

  private def getRedirectionRoute(response: SubmitEclReturnResponse)(implicit
    hc: HeaderCarrier,
    request: ReturnDataRequest[AnyContent]
  ): Future[Result] = {
    val containsEmailAddress = checkOptionalVal(request.eclReturn.contactEmailAddress)
    request.eclReturn.returnType match {
      case Some(AmendReturn) =>
        containsEmailAddress match {
          case Right(email)    =>
            (request.eclReturn.calculatedLiability, request.periodKey) match {
              case (Some(liability), Some(periodKey)) =>
                val amountDue = liability.amountDue.amount
                val band      = liability.calculatedBand
                (for {
                  subscription <- returnsService
                                    .getEclReturnSubmission(periodKey, request.eclRegistrationReference)
                                    .asResponseError
                } yield subscription).fold(
                  _ => showSuccessPage(email, Some(band), Some(amountDue), isIncrease = false),
                  subscription =>
                    showSuccessPage(
                      email,
                      Some(band),
                      Some(amountDue),
                      (amountDue > subscription.returnDetails.amountOfEclDutyLiable)
                    )
                )
              case _                                  =>
                Future.successful(showSuccessPage(email, None, None, isIncrease = false))
            }
          case Left(errorPage) => Future.successful(errorPage)
        }
      case _                 =>
        containsEmailAddress match {
          case Right(email)    =>
            checkOptionalVal(request.eclReturn.calculatedLiability) match {
              case Right(calculatedLiability) =>
                val session = {
                  request.session.clearEclValues ++ response.chargeReference.fold(Seq.empty[(String, String)])(c =>
                    Seq(SessionKeys.chargeReference -> c)
                  ) ++ Seq(
                    SessionKeys.email             -> email,
                    SessionKeys.obligationDetails -> Json.stringify(
                      Json.toJson(request.eclReturn.obligationDetails)
                    ),
                    SessionKeys.amountDue         ->
                      calculatedLiability.amountDue.amount.toString(),
                    SessionKeys.returnType        -> Json.stringify(Json.toJson(request.eclReturn.returnType))
                  )
                }
                val sessionData = SessionData(request.internalId, session.data)
                sessionService.upsert(sessionData)

                Future.successful(Redirect(routes.ReturnSubmittedController.onPageLoad()).withSession(session))
              case Left(errorPage)            => Future.successful(errorPage)
            }
          case Left(errorPage) => Future.successful(errorPage)
        }
    }
  }

  private def showSuccessPage(
    email: String,
    band: Option[Band],
    amountDue: Option[BigDecimal],
    isIncrease: Boolean
  )(implicit
    request: ReturnDataRequest[AnyContent]
  ): Result = {
    def asString[T](option: Option[T]) = option match {
      case Some(value) => value.toString
      case None        => ""
    }

    val sessionData = Seq(
      SessionKeys.email             -> email,
      SessionKeys.band              -> asString(band),
      SessionKeys.amountDue         -> asString(amountDue),
      SessionKeys.isIncrease        -> isIncrease.toString,
      SessionKeys.obligationDetails -> Json.stringify(Json.toJson(request.eclReturn.obligationDetails)),
      SessionKeys.returnType        -> Json.stringify(Json.toJson(request.eclReturn.returnType))
    ) ++ request.startAmendUrl.fold(Seq.empty[(String, String)])(url => Seq(SessionKeys.startAmendUrl -> url))

    Redirect(routes.AmendReturnSubmittedController.onPageLoad())
      .withSession(addToSession(request.session.clearEclValues, sessionData))
  }

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

  private def viewHtmlOrError()(implicit
    request: ReturnDataRequest[AnyContent]
  ): EitherT[Future, ResponseError, HtmlFormat.Appendable] =
    EitherT {
      request.eclReturn.returnType match {
        case Some(FirstTimeReturn) => Future.successful(Right(view(viewModelWithoutEclReturnSubmission(), appConfig)))
        case Some(AmendReturn)     =>
          if (appConfig.getEclReturnEnabled) {
            request.periodKey match {
              case None            =>
                Future.successful(Left(ResponseError.internalServiceError("Unable to find period key")))
              case Some(periodKey) =>
                viewModelWithEclReturnSubmission(periodKey).fold(
                  error => Left(error),
                  viewModel => Right(view(viewModel, appConfig))
                )
            }
          } else {
            Future.successful(Right(view(viewModelWithoutEclReturnSubmission(), appConfig)))
          }
        case None                  => Future.successful(Left(ResponseError.internalServiceError("Unable to find return type")))
      }
    }

  private def pdfViewModelWithEclReturnSubmission(periodKey: String)(implicit
    hc: HeaderCarrier,
    request: ReturnDataRequest[AnyContent]
  ): EitherT[Future, ResponseError, AmendReturnPdfViewModel] =
    returnsService
      .getEclReturnSubmission(periodKey, request.eclRegistrationReference)
      .map(eclReturnSubmission =>
        AmendReturnPdfViewModel(
          date = LocalDate.now(),
          eclReturn = request.eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission)
        )
      )
      .asResponseError

  private def pdfViewModelWithoutEclReturnSubmission()(implicit
    request: ReturnDataRequest[AnyContent]
  ): AmendReturnPdfViewModel =
    AmendReturnPdfViewModel(
      date = LocalDate.now(),
      eclReturn = request.eclReturn,
      eclReturnSubmission = None
    )

  private def viewModelWithEclReturnSubmission(periodKey: String)(implicit
    hc: HeaderCarrier,
    request: ReturnDataRequest[AnyContent]
  ): EitherT[Future, ResponseError, CheckYourAnswersViewModel] =
    returnsService
      .getEclReturnSubmission(periodKey, request.eclRegistrationReference)
      .map(eclReturnSubmission =>
        CheckYourAnswersViewModel(
          eclReturn = request.eclReturn,
          eclReturnSubmission = Some(eclReturnSubmission),
          startAmendUrl = request.startAmendUrl
        )
      )
      .asResponseError

  private def viewModelWithoutEclReturnSubmission()(implicit
    request: ReturnDataRequest[AnyContent]
  ): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      eclReturn = request.eclReturn,
      eclReturnSubmission = None,
      startAmendUrl = request.startAmendUrl
    )
}
