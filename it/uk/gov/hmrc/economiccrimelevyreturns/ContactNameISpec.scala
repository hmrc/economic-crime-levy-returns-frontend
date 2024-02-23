package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode, SessionData, SessionKeys}

class ContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ContactNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactNameController.onPageLoad(NormalMode))

    "respond with 200 status and the contact name HTML view" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)

      val result = callRoute(FakeRequest(routes.ContactNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Who is completing this return?")
    }
  }

  s"POST ${routes.ContactNameController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactNameController.onSubmit(NormalMode))

    "save the provided name then redirect to the contact role page" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val name             = stringFromRegex(MinMaxValues.NameMaxLength, Regex.NameRegex).sample.get.trim
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn)
      stubGetSession(validSessionData)

      val updatedReturn = eclReturn.copy(contactName = Some(name))

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.ContactNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", name))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ContactRoleController.onPageLoad(NormalMode).url)
    }
  }
}
