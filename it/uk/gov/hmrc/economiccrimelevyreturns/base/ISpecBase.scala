/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyreturns.base

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Status => _, _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Call, Result, Results}
import play.api.test._
import play.api.{Application, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.TestUtils
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, FirstTimeReturn}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

abstract class ISpecBase
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with OptionValues
    with DefaultAwaitTimeout
    with Writeables
    with RouteInvokers
    with Results
    with Status
    with HeaderNames
    with MimeTypes
    with ResultExtractors
    with WireMockHelper
    with WireMockStubs
    with Generators
    with TestUtils {

  implicit val arbString: Arbitrary[String]    = Arbitrary(Gen.alphaNumStr.retryUntil(_.nonEmpty))
  implicit lazy val system: ActorSystem        = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit def ec: ExecutionContext            = global

  def configOverrides: Map[String, Any] = Map()

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"  -> false,
    "auditing.enabled" -> false
  ) ++ setWireMockPort(
    "auth",
    "economic-crime-levy-returns",
    "economic-crime-levy-calculator",
    "economic-crime-levy-account",
    "email",
    "enrolment-store-proxy",
    "economic-crime-levy-registration"
  ) ++ configOverrides

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .in(Mode.Test)
      .build()

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  /*
  This is to initialise the app before running any tests, as it is lazy by default in org.scalatestplus.play.BaseOneAppPerSuite.
  It enables us to include behaviour tests that call routes within the `should` part of a test but before `in`.
   */
  locally { val _ = app }

  override def beforeAll(): Unit = {
    startWireMock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    resetWireMock()
    super.afterEach()
  }

  def callRoute[A](fakeRequest: FakeRequest[A], requiresAuth: Boolean = true)(implicit
    app: Application,
    w: Writeable[A]
  ): Future[Result] = {
    val errorHandler = app.errorHandler

    val req = if (requiresAuth) fakeRequest.withSession("authToken" -> "test") else fakeRequest

    route(app, req) match {
      case None         => fail("Route does not exist")
      case Some(result) =>
        result.recoverWith { case t: Throwable =>
          errorHandler.onServerError(req, t)
        }
    }
  }

  def html(result: Future[Result]): String = {
    contentType(result) shouldBe Some("text/html")
    Jsoup.parse(contentAsString(result)).html()
  }

  case class RelatedValueInfo[B](
    value: B,
    updateEclReturnValue: (EclReturn, B) => EclReturn,
    clearEclReturnValue: EclReturn => EclReturn,
    destination: Call
  )

  object RelatedValueInfo {
    def apply[B](
      value: B,
      updateEclReturnValue: (EclReturn, B) => EclReturn,
      clearEclReturnValue: EclReturn => EclReturn,
      destination: Call
    ) =
      new RelatedValueInfo[B](
        value = value,
        updateEclReturnValue = updateEclReturnValue,
        clearEclReturnValue = clearEclReturnValue,
        destination = destination
      )
  }

  private def set[A](
    value: A,
    updateEclReturnValue: (EclReturn, A) => EclReturn,
    testSetup: Option[(EclReturn, String) => EclReturn]
  ) = {
    val eclReturn = updateEclReturnValue(
      random[EclReturn].copy(internalId = testInternalId),
      value
    )
    setup(eclReturn, testSetup)
  }

  private def clear[A](
    clearEclReturnValue: EclReturn => EclReturn,
    testSetup: Option[(EclReturn, String) => EclReturn]
  ) = {
    val eclReturn = clearEclReturnValue(
      random[EclReturn].copy(internalId = testInternalId)
    )
    setup(eclReturn, testSetup)
  }

  private def setup(eclReturn: EclReturn, testSetup: Option[(EclReturn, String) => EclReturn]) =
    testSetup match {
      case Some(setup) => setup(eclReturn, testInternalId)
      case None        => eclReturn
    }

  def goToNextPageInCheckMode[A, B](
    value: A,
    updateEclReturnValue: (EclReturn, A) => EclReturn,
    clearEclReturnValue: EclReturn => EclReturn,
    callToMake: Call,
    testSetup: Option[(EclReturn, String) => EclReturn] = None,
    relatedValueInfo: Option[RelatedValueInfo[B]] = None
  ): Unit =
    "Go to next page is CheckMode" should {
      "go to check your answers page if data has not changed" in {
        stubAuthorised()

        val eclReturn = set(value, updateEclReturnValue, testSetup)
        relatedValueInfo.foreach(info => info.updateEclReturnValue(eclReturn, info.value))

        stubGetReturn(eclReturn)
        stubUpsertReturn(updateEclReturnValue(eclReturn, value))
        stubGetEmptyObligations()

        val result = callRoute(
          FakeRequest(callToMake).withFormUrlEncodedBody(("value", value.toString))
        )

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          routes.CheckYourAnswersController.onPageLoad(eclReturn.returnType.getOrElse(FirstTimeReturn)).url
        )
      }

      relatedValueInfo.foreach { info =>
        "request related data if it is absent" in {
          stubAuthorised()

          val eclReturn = info.clearEclReturnValue(clear(clearEclReturnValue, testSetup))

          stubGetReturn(eclReturn)
          stubUpsertReturn(updateEclReturnValue(eclReturn, value))
          stubGetEmptyObligations()

          val result = callRoute(
            FakeRequest(callToMake).withFormUrlEncodedBody(("value", value.toString))
          )

          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(info.destination.url)
        }
      }
    }
}
