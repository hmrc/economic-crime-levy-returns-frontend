package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.email.ReturnSubmittedEmailRequest

trait EmailStubs { self: WireMockStubs =>

  def stubSendReturnSubmittedEmail(
    returnSubmittedEmailRequest: ReturnSubmittedEmailRequest
  ): StubMapping =
    stub(
      post(urlEqualTo("/hmrc/email"))
        .withRequestBody(
          equalToJson(
            Json.toJson(returnSubmittedEmailRequest).toString()
          )
        ),
      aResponse()
        .withStatus(ACCEPTED)
    )

}
