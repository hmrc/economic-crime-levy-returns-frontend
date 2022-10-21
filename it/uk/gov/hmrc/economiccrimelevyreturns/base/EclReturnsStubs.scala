package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._

trait EclReturnsStubs {

  def stubGetReturn(): StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-returns/returns/test-id")),
      aResponse()
        .withStatus(200)
        .withBody(s"""
             |{
             |  "internalId": "test-id"
             |}
         """.stripMargin)
    )

}
