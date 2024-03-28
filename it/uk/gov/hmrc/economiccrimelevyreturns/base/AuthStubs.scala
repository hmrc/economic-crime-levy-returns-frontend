package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{OK, UNAUTHORIZED}
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment

trait AuthStubs { self: WireMockStubs =>

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.serviceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "$testInternalId",
             |  "authorisedEnrolments": [ {
             |    "key": "${EclEnrolment.serviceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.identifierKey}", "value": "$testEclRegistrationReference" }],
             |    "state": "activated"
             |  } ]
             |}
         """.stripMargin)
    )

  def stubInsufficientEnrolments(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.serviceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(UNAUTHORIZED)
        .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
    )

  def stubUnsupportedAffinityGroup(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.serviceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(UNAUTHORIZED)
        .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAffinityGroup\"")
    )

}
