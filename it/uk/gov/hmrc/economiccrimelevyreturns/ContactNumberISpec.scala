package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, FirstTimeReturn, NormalMode, SessionData, SessionKeys}

class ContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ContactNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactNumberController.onPageLoad(NormalMode))

    "respond with 200 status and the contact number HTML view" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val name             = random[String]
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      stubGetReturn(eclReturn.copy(contactName = Some(name)))
      stubGetSession(validSessionData)
      stubUpsertSession()

      val result = callRoute(FakeRequest(routes.ContactNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's telephone number?")
    }
  }

  s"POST ${routes.ContactNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactNumberController.onSubmit(NormalMode))

    "save the provided telephone number then redirect to the check your answers page" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val name             = random[String]
      val number           = stringFromRegex(MinMaxValues.TelephoneNumberMaxLength, Regex.TelephoneNumberRegex).sample.get
      val sessionData      = random[SessionData]
      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))

      val updatedReturn =
        eclReturn.copy(contactName = Some(name), contactTelephoneNumber = Some(number.filterNot(_.isWhitespace)))

      stubGetReturn(updatedReturn)
      stubUpsertReturn(updatedReturn)
      stubGetSession(validSessionData)

      val result = callRoute(
        FakeRequest(routes.ContactNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", number))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad(eclReturn.returnType.getOrElse(FirstTimeReturn)).url
      )
    }
  }
}
