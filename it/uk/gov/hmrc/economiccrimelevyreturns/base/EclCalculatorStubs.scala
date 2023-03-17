package uk.gov.hmrc.economiccrimelevyreturns.base

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability}

trait EclCalculatorStubs { self: WireMockStubs =>

  def stubCalculateLiability(calculateLiabilityRequest: CalculateLiabilityRequest): StubMapping = {
    val calculatedLiability = random[CalculatedLiability]

    stub(
      post(urlEqualTo("/economic-crime-levy-calculator/calculate-liability"))
        .withRequestBody(
          equalToJson(Json.toJson(calculateLiabilityRequest).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(calculatedLiability).toString())
    )
  }

}
