package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}

class FirstContactRoleISpec extends ISpecBase with AuthorisedBehaviour {

  val roleMaxLength: Int = 160

  s"GET ${routes.ContactRoleController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactRoleController.onPageLoad(NormalMode))

    "respond with 200 status and the contact role HTML view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val name         = random[String]

      stubGetReturn(
        eclReturn.copy(contactName = Some(name))
          )

      val result = callRoute(FakeRequest(routes.ContactRoleController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(s"What is $name's role in your organisation?")
    }
  }

  s"POST ${routes.ContactRoleController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.ContactRoleController.onSubmit(NormalMode))

    "save the provided role then redirect to the contact email page" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]
      val name         = random[String]
      val role         = stringsWithMaxLength(roleMaxLength).sample.get

      val updatedReturn = eclReturn.copy(contactName = Some(name), contactRole = Some(role))

      stubGetReturn(updatedReturn)
      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.ContactRoleController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", role))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.ContactEmailController.onPageLoad(NormalMode).url)
    }
  }
}
