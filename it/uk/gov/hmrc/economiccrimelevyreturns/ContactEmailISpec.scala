package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}

class ContactEmailISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ContactEmailController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactEmailController.onPageLoad(NormalMode))

    "respond with 200 status and the contact email HTML view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val name      = random[String]

      stubGetReturn(eclReturn.copy(contactName = Some(name)))
      stubGetSessionEmpty()

      val result = callRoute(FakeRequest(routes.ContactEmailController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's email address?")
    }
  }

  s"POST ${routes.ContactEmailController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.ContactEmailController.onSubmit(NormalMode))

    "save the provided email address then redirect to the contact telephone number page" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val name      = random[String]
      val email     = emailAddress(MinMaxValues.EmailMaxLength).sample.get

      val updatedReturn = eclReturn.copy(contactName = Some(name), contactEmailAddress = Some(email.toLowerCase))

      stubGetReturn(updatedReturn)
      stubUpsertReturn(updatedReturn)
      stubGetSessionEmpty()

      val result = callRoute(
        FakeRequest(routes.ContactEmailController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", email))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ContactNumberController.onPageLoad(NormalMode).url)
    }
  }
}
