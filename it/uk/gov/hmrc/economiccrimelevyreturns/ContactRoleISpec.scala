package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models._

class ContactRoleISpec extends ISpecBase with AuthorisedBehaviour {

  private def updateContactRole(eclReturn: EclReturn, role: String): EclReturn =
    eclReturn.copy(contactRole = Some(role))

  private def clearContactRole(eclReturn: EclReturn): EclReturn =
    eclReturn.copy(contactRole = None)

  private def testSetup(eclReturn: EclReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.periodKey -> testPeriodKey)
      )
    )
    updateContactName(eclReturn)
  }

  private def validContactRole: String =
    ensureMaxLength(alphaNumericString, MinMaxValues.roleMaxLength)

  s"GET ${routes.ContactRoleController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactRoleController.onPageLoad(NormalMode))

    "respond with 200 status and the contact role HTML view" in {
      stubAuthorised()

      val eclReturn = testSetup(random[EclReturn])
      stubGetReturn(eclReturn)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.ContactRoleController.onPageLoad(NormalMode)))

      status(result) shouldBe OK
      html(result)     should include(s"What is ${eclReturn.contactName.get}'s role in your organisation?")
    }
  }

  s"POST ${routes.ContactRoleController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactRoleController.onSubmit(NormalMode))

    "save the provided role then redirect to the contact email page" in {
      stubAuthorised()

      val role      = validContactRole
      val eclReturn = clearContactRole(testSetup(random[EclReturn]))

      stubGetReturn(eclReturn)
      stubUpsertReturn(updateContactRole(eclReturn, role))

      val result = callRoute(
        FakeRequest(routes.ContactRoleController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", role))
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ContactEmailController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.ContactRoleController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionRoute(routes.ContactRoleController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = validContactRole,
      updateEclReturnValue = updateContactRole,
      clearEclReturnValue = clearContactRole,
      callToMake = routes.ContactRoleController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
