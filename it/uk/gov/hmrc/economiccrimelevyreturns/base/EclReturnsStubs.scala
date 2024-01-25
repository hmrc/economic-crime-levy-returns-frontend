package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, GetEclReturnSubmissionResponse, SubmitEclReturnResponse}

import java.time.Instant

trait EclReturnsStubs { self: WireMockStubs =>

  def stubGetReturn(eclReturn: EclReturn): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(eclReturn).toString())
    )

  def stubGetEclReturnSubmission(
    periodKey: String,
    eclReference: String,
    getEclReturnSubmissionResponse: GetEclReturnSubmissionResponse
  ): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/submission/$periodKey/$eclReference")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(getEclReturnSubmissionResponse).toString())
    )

  def stubUpsertReturn(eclReturn: EclReturn): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-returns/returns"))
        .withRequestBody(
          equalToJson(Json.toJson(eclReturn).toString(), true, true)
        ),
      aResponse()
        .withStatus(NO_CONTENT)
    )

  def stubUpsertReturnWithoutRequestMatching(eclReturn: EclReturn): StubMapping =
    stub(
      put(urlEqualTo("/economic-crime-levy-returns/returns")),
      aResponse()
        .withStatus(NO_CONTENT)
    )

  def stubGetReturnValidationErrors(valid: Boolean, error: DataValidationError): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId/validation-errors")),
      if (valid) {
        aResponse()
          .withStatus(OK) withBody (Json.stringify(Json.toJson(JsNull)))
      } else {
        aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(error.message).toString())
      }
    )

  def stubSubmitReturn(chargeReference: Option[String]): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy-returns/submit-return/$testInternalId")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(SubmitEclReturnResponse(Instant.now, chargeReference)).toString())
    )

  def stubDeleteReturn(): StubMapping =
    stub(
      delete(urlEqualTo(s"/economic-crime-levy-returns/returns/$testInternalId")),
      aResponse()
        .withStatus(NO_CONTENT)
    )

}
