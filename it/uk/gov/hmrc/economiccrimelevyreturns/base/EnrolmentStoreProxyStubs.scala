package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.KeyValue
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.{EclEnrolment, Enrolment, QueryKnownFactsRequest, QueryKnownFactsResponse}

trait EnrolmentStoreProxyStubs { self: WireMockStubs =>

  def stubQueryKnownFacts(eclRegistrationReference: String, eclRegistrationDate: String): StubMapping =
    stub(
      post(urlEqualTo(s"/enrolment-store-proxy/enrolments")).withRequestBody(
        equalToJson(
          Json
            .toJson(
              QueryKnownFactsRequest(
                service = EclEnrolment.ServiceName,
                knownFacts = Seq(KeyValue(key = EclEnrolment.IdentifierKey, value = eclRegistrationReference))
              )
            )
            .toString()
        )
      ),
      aResponse()
        .withStatus(OK)
        .withBody(
          Json
            .toJson(
              QueryKnownFactsResponse(
                service = EclEnrolment.ServiceName,
                enrolments = Seq(
                  Enrolment(
                    identifiers = Seq(KeyValue(key = EclEnrolment.IdentifierKey, value = eclRegistrationReference)),
                    verifiers = Seq(KeyValue(key = EclEnrolment.VerifierKey, value = eclRegistrationDate))
                  )
                )
              )
            )
            .toString()
        )
    )

}
