package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData

trait SessionDataStubs { self: WireMockStubs =>

  def stubGetSessionEmpty() =
    stubGetSession(
      SessionData(
        internalId = testInternalId,
        values = Map()
      )
    )

  def stubGetSession(sessionData: SessionData): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/session/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(sessionData).toString())
    )

  def stubUpsertSession(sessionData: SessionData): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-returns/session"))
        .withRequestBody(
          equalToJson(Json.toJson(sessionData).toString(), true, true)
        ),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(sessionData).toString())
    )

  def stubDeleteSession(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-returns/session/$testInternalId")),
      aResponse()
        .withStatus(OK)
    )

}
