package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SubmitEclReturnResponse}

import java.time.Instant

trait EclReturnsStubs { self: WireMockStubs =>

  def stubGetReturn(eclReturn: EclReturn): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(eclReturn).toString())
    )

  def stubUpsertReturn(eclReturn: EclReturn): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-returns/returns"))
        .withRequestBody(
          equalToJson(Json.toJson(eclReturn).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(eclReturn).toString())
    )

  def stubUpsertReturnWithoutRequestMatching(eclReturn: EclReturn): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-returns/returns")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(eclReturn).toString())
    )

  def stubGetReturnValidationErrors(valid: Boolean, errors: DataValidationErrors): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId/validation-errors")),
      if (valid) {
        aResponse()
          .withStatus(NO_CONTENT)
      } else {
        aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(errors).toString())
      }
    )

  def stubSubmitReturn(chargeReference: Option[String]): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy-returns/submit-return/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(SubmitEclReturnResponse(Instant.now, chargeReference)).toString())
    )

  def stubDeleteReturn(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
