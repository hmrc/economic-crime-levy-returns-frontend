package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._

trait EclReturnsStubs { self: WireMockStubs =>

  def stubGetReturn(): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "$testInternalId"
             |}
         """.stripMargin)
    )

}
