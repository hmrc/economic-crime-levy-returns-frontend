package uk.gov.hmrc.economiccrimelevyreturns.utils

import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.util.UUID

object CorrelationIdHelper {

  def getOrCreateCorrelationId(request: Request[_]): HeaderCarrier = {
    val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    hcFromRequest
      .headers(scala.Seq(HttpHeader.CorrelationId)) match {
      case Nil =>
        hcFromRequest.withExtraHeaders((HttpHeader.CorrelationId, UUID.randomUUID().toString))
      case _   =>
        hcFromRequest
    }
  }

}
