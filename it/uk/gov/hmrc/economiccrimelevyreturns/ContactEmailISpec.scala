package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode, SessionData, SessionKeys}

class ContactEmailISpec extends ISpecBase with AuthorisedBehaviour {

  private def updateContactEmail(eclReturn: EclReturn, email: String) =
    eclReturn.copy(contactEmailAddress = Some(email.toLowerCase))

  private def clearContactEmail(eclReturn: EclReturn) =
    eclReturn.copy(contactEmailAddress = None)

  private def testSetup(eclReturn: EclReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )
    updateContactName(eclReturn)
  }

  private def validContactEmail: String =
    emailAddress(MinMaxValues.EmailMaxLength).sample.get

  s"GET ${routes.ContactEmailController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactEmailController.onPageLoad(NormalMode))

    "respond with 200 status and the contact email HTML view" in {
      stubAuthorised()

      val eclReturn = testSetup(random[EclReturn])

      stubGetReturn(eclReturn)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.ContactEmailController.onPageLoad(NormalMode)))

      status(result) shouldBe OK
      html(result)     should include(s"What is ${eclReturn.contactName.get}'s email address?")
    }
  }

  s"POST ${routes.ContactEmailController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactEmailController.onSubmit(NormalMode))

    "save the provided email address then redirect to the contact telephone number page" in {
      stubAuthorised()

      val email = validContactEmail

      val eclReturn = testSetup(clearContactEmail(random[EclReturn]))
      stubGetReturn(eclReturn)
      stubUpsertReturn(updateContactEmail(eclReturn, email))

      val result = callRoute(
        FakeRequest(routes.ContactEmailController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", email))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ContactNumberController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.ContactEmailController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.ContactEmailController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = validContactEmail,
      updateEclReturnValue = updateContactEmail,
      clearEclReturnValue = clearContactEmail,
      callToMake = routes.ContactEmailController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
