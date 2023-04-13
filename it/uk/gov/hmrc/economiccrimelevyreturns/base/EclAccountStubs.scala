package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.ObligationData

trait EclAccountStubs { self: WireMockStubs =>

  def stubGetObligations(obligationData: ObligationData): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-account/obligation-data")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(obligationData).toString())
    )

}
