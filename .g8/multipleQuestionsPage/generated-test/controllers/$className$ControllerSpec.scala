package uk.gov.hmrc.economiccrimelevyreturns.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyreturns.forms.$className$FormProvider
import uk.gov.hmrc.economiccrimelevyreturns.models.{$className$, NormalMode, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.views.html.$className$View

import scala.concurrent.Future

class $className$ControllerSpec extends SpecBase {

  val view: $className$View = app.injector.instanceOf[$className$View]

  val formProvider = new $className$FormProvider()

  val form: Form[$className$] = formProvider()

  val mockEclReturnConnector: EclReturnsConnector = mock[EclReturnsConnector]

  class TestFixture(data: EclReturn = emptyReturn) {
    val controller = new $className$Controller(
      messagesApi = messagesApi,
      eclReturnsConnector = mockEclReturnConnector,
      navigator = fakeNavigator,
      authorise = fakeAuthorisedAction,
      getReturnData = fakeDataRetrievalAction(data),
      formProvider = formProvider,
      controllerComponents = mcc,
      view = view
    )
  }

  "onPageLoad" should {

    "return OK and the correct view" in new TestFixture() {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
    }

    "populate the view correctly when the question has previously been answered" in new TestFixture(
      emptyReturn.copy(??? = Some($className$("value 1", "value 2"))) // TODO Choose the data you are testing
    ) {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form.fill($className$("value 1", "value 2")), NormalMode)(
        fakeRequest,
        messages
      ).toString
    }

    "redirect to the next page when valid data is submitted" in new TestFixture() {
      when(mockEclReturnConnector.updateReturn(any())).thenReturn(Future.successful(emptyReturn))

      val result: Future[Result] =
        controller.onSubmit(NormalMode)(
          fakeRequest.withFormUrlEncodedBody(("$field1Name$" -> "value 1"), ("$field2Name$" -> "value 2"))
        )

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe onwardRoute.url
    }

    "return a Bad Request and errors when invalid data is submitted" in new TestFixture() {
      val result: Future[Result] = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> ""))

      val formWithErrors: Form[$className$] = form.bind(Map("value" -> ""))

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
    }
  }
}
