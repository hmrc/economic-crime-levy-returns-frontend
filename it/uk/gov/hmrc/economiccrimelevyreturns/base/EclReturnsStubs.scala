package uk.gov.hmrc.economiccrimelevyreturns.base

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability, EclReturn, SubmitEclReturnResponse}

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

  def stubCalculateLiability(calculateLiabilityRequest: CalculateLiabilityRequest): StubMapping = {
    val calculatedLiability = random[CalculatedLiability]

    stub(
      post(urlEqualTo("/economic-crime-levy-returns/calculate-liability"))
        .withRequestBody(
          equalToJson(Json.toJson(calculateLiabilityRequest).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(calculatedLiability).toString())
    )
  }

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

  def stubSubmitReturn(chargeReference: String): StubMapping =
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
