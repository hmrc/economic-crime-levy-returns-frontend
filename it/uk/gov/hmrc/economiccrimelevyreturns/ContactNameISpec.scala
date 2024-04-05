package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CheckMode, EclReturn, NormalMode, SessionData, SessionKeys}

class ContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  private def updateContactName(eclReturn: EclReturn, name: String) =
    eclReturn.copy(contactName = Some(name))

  private def clearContactName(eclReturn: EclReturn) =
    eclReturn.copy(contactName = None)

  private def testSetup(eclReturn: EclReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )
    eclReturn
  }

  private def validContactName: String =
    ensureMaxLength(alphaNumericString, MinMaxValues.NameMaxLength)

  s"GET ${routes.ContactNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactNameController.onPageLoad(NormalMode))

    "respond with 200 status and the contact name HTML view" in {
      stubAuthorised()

      stubGetReturn(testSetup(random[EclReturn]))
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.ContactNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Who is completing this return?")
    }
  }

  s"POST ${routes.ContactNameController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactNameController.onSubmit(NormalMode))

    "save the provided name then redirect to the contact role page" in {
      stubAuthorised()

      val name      = validContactName
      val eclReturn = clearContactName(testSetup(random[EclReturn]))

      stubGetReturn(eclReturn)
      stubUpsertReturn(updateContactName(eclReturn, name))

      val result = callRoute(
        FakeRequest(routes.ContactNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", name))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ContactRoleController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.ContactNameController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.ContactNameController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = validContactName,
      updateEclReturnValue = updateContactName,
      clearEclReturnValue = clearContactName,
      callToMake = routes.ContactNameController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
