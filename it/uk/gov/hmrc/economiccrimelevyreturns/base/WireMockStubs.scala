/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyreturns.base

import uk.gov.hmrc.economiccrimelevyreturns.EclTestData

trait WireMockStubs extends EclTestData with AuthStubs with EclReturnsStubs with EnrolmentStoreProxyStubs
