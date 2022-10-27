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
               |    "enrolment": "${EclEnrolment.ServiceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments", "affinityGroup" ]
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
             |  "affinityGroup": "Organisation",
             |  "authorisedEnrolments": [ {
             |    "key": "${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationReference" }],
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
               |    "enrolment": "${EclEnrolment.ServiceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments", "affinityGroup" ]
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

  def stubAuthorisedWithAgentAffinityGroup(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.ServiceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments", "affinityGroup" ]
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
             |  "affinityGroup": "Agent",
             |  "authorisedEnrolments": [ {
             |    "key": "${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationReference" }],
             |    "state": "activated"
             |  } ]
             |}
         """.stripMargin)
    )

}
