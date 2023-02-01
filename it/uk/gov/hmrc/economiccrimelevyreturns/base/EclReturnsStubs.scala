package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn

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

}
