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

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.ReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.{AuthorisedAction, DataRetrievalAction, ValidatedReturnAction}
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionKeys._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.ReturnDataRequest
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.services.{EmailService, ReturnsService, SessionService}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers._
import uk.gov.hmrc.economiccrimelevyreturns.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyreturns.views.html.{AmendReturnPdfView, CheckYourAnswersView}
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
  validateReturnData: ValidatedReturnAction,
  returnsService: ReturnsService,
  sessionService: SessionService,
  emailService: EmailService,
  amendReturnPdfView: AmendReturnPdfView,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
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

  def onPageLoad: Action[AnyContent] = (authorise andThen getReturnData andThen validateReturnData) {
    implicit request =>
      Ok(view(eclDetails(), contactDetails(), request.startAmendUrl))
  }

  def onSubmit: Action[AnyContent] = (authorise andThen getReturnData).async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)

    val htmlView = view(eclDetails(), contactDetails())

    val base64EncodedHtmlView: String = base64EncodeHtmlView(htmlView.body)

    val base64EncodedDmsSubmissionHtml = request.eclReturn.returnType.flatMap {
      case AmendReturn     => Some(createAndEncodeHtmlForPdf())
      case FirstTimeReturn => None
    }

    val updatedReturn = request.eclReturn.copy(
      base64EncodedNrsSubmissionHtml = Some(base64EncodedHtmlView),
      base64EncodedDmsSubmissionHtml = base64EncodedDmsSubmissionHtml
    )

    (for {
      _        <- returnsService.upsertReturn(eclReturn = updatedReturn).asResponseError
      response <- returnsService.submitReturn(request.internalId).asResponseError
      _         = sendConfirmationMail(request.eclReturn, response) // shouldn't this be set to the updated return?
      _        <- returnsService.deleteReturn(request.internalId).asResponseError
      _         = sessionService.delete(request.internalId)
    } yield response).fold(
      error => Redirect(routes.NotableErrorController.answersAreInvalid()), //needs fixing
      response => getRedirectionRoute(request, response)
    )
  }

  private def sendConfirmationMail(eclReturn: EclReturn, response: SubmitEclReturnResponse)(implicit
    messages: Messages,
    hc: HeaderCarrier
  ) =
    eclReturn.returnType.getOrElse("Return type is missing in session") match {
      case FirstTimeReturn => emailService.sendReturnSubmittedEmail(eclReturn, response.chargeReference)
      case AmendReturn     => emailService.sendAmendReturnConfirmationEmail(eclReturn)
    }

  //TODO: fix exception throwing
  private def getRedirectionRoute(request: ReturnDataRequest[AnyContent], response: SubmitEclReturnResponse) =
    request.eclReturn.returnType match {
      case Some(AmendReturn) =>
        if (request.eclReturn.contactEmailAddress.isEmpty) {}
        Redirect(routes.AmendReturnSubmittedController.onPageLoad()).withSession(
          request.session.clearEclValues ++ Seq(
            SessionKeys.Email             -> request.eclReturn.contactEmailAddress
              .getOrElse(throw new IllegalStateException("Contact email address not found in return data")),
            SessionKeys.ObligationDetails -> Json.toJson(request.eclReturn.obligationDetails).toString()
          )
        )
      case _                 =>
        Redirect(routes.ReturnSubmittedController.onPageLoad()).withSession(
          request.session.clearEclValues ++ response.chargeReference.fold(Seq.empty[(String, String)])(c =>
            Seq(SessionKeys.ChargeReference -> c)
          ) ++ Seq(
            SessionKeys.Email             -> request.eclReturn.contactEmailAddress
              .getOrElse(throw new IllegalStateException("Contact email address not found in return data")),
            SessionKeys.ObligationDetails -> Json.toJson(request.eclReturn.obligationDetails).toString(),
            SessionKeys.AmountDue         ->
              request.eclReturn.calculatedLiability
                .getOrElse(
                  throw new IllegalStateException("Amount due not found in return data")
                )
                .amountDue
                .amount
                .toString()
          )
        )
    }

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

  private def createAndEncodeHtmlForPdf()(implicit request: ReturnDataRequest[_]): String = {
    val date         = LocalDate.now
    val organisation = eclDetails().copy(rows = eclDetails().rows.map(_.copy(actions = None)))
    val contact      = contactDetails().copy(rows = contactDetails().rows.map(_.copy(actions = None)))
    base64EncodeHtmlView(
      amendReturnPdfView(
        ViewUtils.formatLocalDate(date),
        organisation,
        contact
      ).toString()
    )
  }
}
