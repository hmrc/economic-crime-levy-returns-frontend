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

  def updateContactEmail(eclReturn: EclReturn, email: String) =
    eclReturn.copy(contactEmailAddress = Some(email))

  def clearContactEmail(eclReturn: EclReturn) =
    eclReturn.copy(contactEmailAddress = None)

  def testSetup(eclReturn: EclReturn, internalId: String = testInternalId): EclReturn = {
    stubGetSession(
      SessionData(
        internalId = internalId,
        values = Map(SessionKeys.PeriodKey -> testPeriodKey)
      )
    )
    updateContactName(eclReturn)
  }

//  s"GET ${routes.ContactEmailController.onPageLoad(NormalMode).url}" should {
//    behave like authorisedActionRoute(routes.ContactEmailController.onPageLoad(NormalMode))
//
//    "respond with 200 status and the contact email HTML view" in {
//      stubAuthorised()
//
//      val eclReturn        = random[EclReturn]
//      val name             = random[String]
//      val sessionData      = random[SessionData]
//      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))
//
//      stubGetReturn(eclReturn.copy(contactName = Some(name)))
//      stubGetSession(validSessionData)
//      stubUpsertSession()
//
//      val result = callRoute(FakeRequest(routes.ContactEmailController.onPageLoad(NormalMode)))
//
//      status(result) shouldBe OK
//
//      html(result) should include(s"What is $name's email address?")
//    }
//  }
//
//  s"POST ${routes.ContactEmailController.onSubmit(NormalMode).url}" should {
//    behave like authorisedActionRoute(routes.ContactEmailController.onSubmit(NormalMode))
//
//    "save the provided email address then redirect to the contact telephone number page" in {
//      stubAuthorised()
//
//      val eclReturn        = random[EclReturn]
//      val name             = random[String]
//      val email            = emailAddress(MinMaxValues.EmailMaxLength).sample.get
//      val sessionData      = random[SessionData]
//      val validSessionData = sessionData.copy(values = Map(SessionKeys.PeriodKey -> testPeriodKey))
//
//      val updatedReturn = eclReturn.copy(contactName = Some(name), contactEmailAddress = Some(email.toLowerCase))
//
//      stubGetReturn(updatedReturn)
//      stubUpsertReturn(updatedReturn)
//      stubGetSession(validSessionData)
//
//      val result = callRoute(
//        FakeRequest(routes.ContactEmailController.onSubmit(NormalMode))
//          .withFormUrlEncodedBody(("value", email))
//      )
//
//      status(result) shouldBe SEE_OTHER
//
//      redirectLocation(result) shouldBe Some(routes.ContactNumberController.onPageLoad(NormalMode).url)
//    }
//  }

  s"POST ${routes.ContactEmailController.onSubmit(CheckMode).url}" should {
//    behave like authorisedActionRoute(routes.ContactEmailController.onSubmit(CheckMode))
    behave like goToNextPageInCheckMode(
      value = emailAddress(MinMaxValues.EmailMaxLength).sample.get,
      updateEclReturnValue = updateContactEmail,
      clearEclReturnValue = clearContactEmail,
      callToMake = routes.ContactEmailController.onSubmit(CheckMode),
      testSetup = Some(testSetup)
    )
  }
}
