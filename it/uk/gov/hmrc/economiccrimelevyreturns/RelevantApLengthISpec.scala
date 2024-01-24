package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, NormalMode}
import uk.gov.hmrc.economiccrimelevyreturns.cleanup.RelevantApLengthDataCleanup
class RelevantApLengthISpec extends ISpecBase with AuthorisedBehaviour {

  val dataCleanup = new RelevantApLengthDataCleanup
  s"GET ${routes.RelevantApLengthController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionRoute(routes.RelevantApLengthController.onPageLoad(NormalMode))

    "respond with 200 status and the relevant AP length view" in {
      stubAuthorised()

      val eclReturn = random[EclReturn]

      stubGetReturn(eclReturn)
      stubGetSessionEmpty()

      val result = callRoute(FakeRequest(routes.RelevantApLengthController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("How long is your relevant accounting period?")
    }
  }

  s"POST ${routes.RelevantApLengthController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionRoute(routes.RelevantApLengthController.onSubmit(NormalMode))

    "save the relevant AP length then redirect to the UK revenue page" in {
      stubAuthorised()

      val eclReturn        = random[EclReturn]
      val relevantApLength = Gen.chooseNum[Int](MinMaxValues.ApDaysMin, MinMaxValues.ApDaysMax).sample.get

      stubGetReturn(eclReturn)
      stubGetSessionEmpty()

      val updatedReturn =
        dataCleanup.cleanup(eclReturn.copy(relevantApLength = Some(relevantApLength)))

      stubUpsertReturn(updatedReturn)

      val result = callRoute(
        FakeRequest(routes.RelevantApLengthController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", relevantApLength.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
    }
  }
}
