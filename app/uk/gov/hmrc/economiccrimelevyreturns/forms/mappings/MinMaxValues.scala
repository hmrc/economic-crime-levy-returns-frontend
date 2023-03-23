/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyreturns.forms.mappings

object MinMaxValues {

  val ApDaysMin                = 1
  val ApDaysMax                = 999
  val RevenueMin               = 0L
  val RevenueMax               = 99999999999L
  val AmlDaysMin               = 0
  val AmlDaysMax               = 365
  val EmailMaxLength           = 132
  val NameMaxLength            = 160
  val TelephoneNumberMaxLength = 24
  val RoleMaxLength            = 160

}
