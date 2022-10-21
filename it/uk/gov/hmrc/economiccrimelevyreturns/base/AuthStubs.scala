package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment

trait AuthStubs {

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.Key}",
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
        .withStatus(200)
        .withBody(s"""
             |{
             |  "internalId": "test-id",
             |  "authorisedEnrolments": [{
             |    "key": "${EclEnrolment.Key}",
             |    "identifiers": [{ "key":"${EclEnrolment.Identifier}", "value": "X00000123456789" }],
             |    "state": "activated"
             |  }]
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
               |    "enrolment": "${EclEnrolment.Key}",
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
        .withStatus(401)
        .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
    )

}
