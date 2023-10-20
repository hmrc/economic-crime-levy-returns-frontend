package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.email.{ReturnSubmittedEmailParameters, ReturnSubmittedEmailRequest}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, Languages}
import uk.gov.hmrc.economiccrimelevyreturns.views.ViewUtils

class CheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.CheckYourAnswersController.onPageLoad())

    "respond with 200 status and the Check your answers HTML view when the ECL return data is valid" in {
      stubAuthorised()

      val validEclReturn = random[ValidEclReturn]
      val errors         = random[DataValidationErrors]

      stubGetReturn(validEclReturn.eclReturn)
      stubGetReturnValidationErrors(valid = true, errors)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"Check your answers")
    }

    "redirect to the answers are invalid page when the ECL return data is invalid" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val errors    = random[DataValidationErrors]

      stubGetReturn(eclReturn)
      stubGetReturnValidationErrors(valid = false, errors)

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onPageLoad()))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }
  }

  s"POST ${routes.CheckYourAnswersController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.CheckYourAnswersController.onSubmit())

    "redirect to the ECL return submitted page after submitting the ECL return successfully" in {
      stubAuthorised()

      val validEclReturn      = random[ValidEclReturn]
      val chargeReference     = random[Option[String]]
      val obligationDetails   = validEclReturn.eclReturn.obligationDetails.get
      val calculatedLiability = validEclReturn.eclReturn.calculatedLiability.get

      stubGetReturn(validEclReturn.eclReturn)

      stubUpsertReturnWithoutRequestMatching(validEclReturn.eclReturn)

      stubSubmitReturn(chargeReference)

      val eclDueDate      =
        ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceDueDate, translate = false)(
          messagesApi.preferred(Seq(Languages.english))
        )
      val dateSubmitted   = ViewUtils.formatToday(translate = false)(messagesApi.preferred(Seq(Languages.english)))
      val periodStartDate =
        ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceFromDate, translate = false)(
          messagesApi.preferred(Seq(Languages.english))
        )
      val periodEndDate   =
        ViewUtils.formatLocalDate(obligationDetails.inboundCorrespondenceToDate, translate = false)(
          messagesApi.preferred(Seq(Languages.english))
        )
      val amountDue       = ViewUtils.formatMoney(calculatedLiability.amountDue.amount)

      val emailParams = ReturnSubmittedEmailParameters(
        name = validEclReturn.eclReturn.contactName.get,
        dateSubmitted = dateSubmitted,
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
        chargeReference = chargeReference,
        fyStartYear = obligationDetails.inboundCorrespondenceFromDate.getYear.toString,
        fyEndYear = obligationDetails.inboundCorrespondenceToDate.getYear.toString,
        datePaymentDue = if (chargeReference.isDefined) Some(eclDueDate) else None,
        amountDue = amountDue
      )

      stubSendReturnSubmittedEmail(
        ReturnSubmittedEmailRequest(
          to = Seq(validEclReturn.eclReturn.contactEmailAddress.get),
          parameters = emailParams,
          templateId = if (chargeReference.isDefined) { ReturnSubmittedEmailRequest.ReturnTemplateId }
          else { ReturnSubmittedEmailRequest.NilReturnTemplateId }
        )
      )

      stubDeleteReturn()

      val result = callRoute(FakeRequest(routes.CheckYourAnswersController.onSubmit()))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ReturnSubmittedController.onPageLoad().url)

      eventually {
        verify(1, postRequestedFor(urlEqualTo("/hmrc/email")))
      }
    }
  }

}
