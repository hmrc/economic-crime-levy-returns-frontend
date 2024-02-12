/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyreturns.base

import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators

trait WireMockStubs
    extends EclTestData
    with Generators
    with AuthStubs
    with EclReturnsStubs
    with SessionDataStubs
    with RegistrationStubs
    with EclCalculatorStubs
    with EclAccountStubs
    with EnrolmentStoreProxyStubs
    with EmailStubs
