package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SessionData, SessionKeys}

class CancelReturnAmendmentISpec extends ISpecBase with AuthorisedBehaviour {

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  s"GET ${routes.CancelReturnAmendmentController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.CancelReturnAmendmentController.onPageLoad())

    "respond with 200 status and the relevant AP 12 months view" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)
      stubGetEmptyObligations()

      val result = callRoute(FakeRequest(routes.CancelReturnAmendmentController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Are you sure you want to cancel your request to amend your return?")
    }
  }

  s"POST ${routes.CancelReturnAmendmentController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.CancelReturnAmendmentController.onSubmit())

    "redirect to ECL Account home page when the Yes option is selected" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn].copy(
        internalId = testInternalId
      )
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.periodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)
      stubDeleteReturn()
      stubGetEmptyObligations()

      val result = callRoute(
        FakeRequest(routes.CancelReturnAmendmentController.onSubmit())
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(appConfig.eclAccountUrl)
    }
  }
}
