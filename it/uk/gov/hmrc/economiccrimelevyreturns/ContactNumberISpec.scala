package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode, SessionData, SessionKeys}

class ContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  private def updateContactNumber(eclReturn: EclReturn, number: String) =
    eclReturn.copy(contactTelephoneNumber = Some(number))

  private def clearContactNumber(eclReturn: EclReturn) =
    eclReturn.copy(contactTelephoneNumber = None)

  private def testSetup(eclReturn: EclReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )
    updateContactName(eclReturn)
  }

  private def validContactNumber: String =
    ensureMaxLength(numericString, MinMaxValues.TelephoneNumberMaxLength)

  s"GET ${routes.ContactNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactNumberController.onPageLoad(NormalMode))

    "respond with 200 status and the contact number HTML view" in {
      stubAuthorised()

      val eclReturn = testSetup(random[EclReturn])

      stubGetReturn(eclReturn)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.ContactNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK
      html(result)     should include(s"What is ${eclReturn.contactName.get}'s telephone number?")
    }
  }

  s"POST ${routes.ContactNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactNumberController.onSubmit(NormalMode))

    "save the provided telephone number then redirect to the check your answers page" in {
      stubAuthorised()

      val number    = validContactNumber
      val eclReturn = testSetup(random[EclReturn])

      stubGetReturn(clearContactNumber(eclReturn))
      stubUpsertReturn(updateContactNumber(eclReturn, number))

      val result = callRoute(
        FakeRequest(routes.ContactNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", number))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }
  }

  s"POST ${routes.ContactNumberController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.ContactNumberController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = validContactNumber,
      updateEclReturnValue = updateContactNumber,
      clearEclReturnValue = clearContactNumber,
      callToMake = routes.ContactNumberController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
