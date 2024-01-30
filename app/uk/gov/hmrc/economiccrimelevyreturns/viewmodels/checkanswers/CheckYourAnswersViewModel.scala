/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.viewmodels.checkanswers

import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, GetEclReturnSubmissionResponse}

final case class CheckYourAnswersViewModel(
  eclReturn: EclReturn,
  eclReturnSubmission: Option[GetEclReturnSubmissionResponse],
  startAmendUrl: Option[String]) {

  val isAmendment: Boolean = eclReturn.returnType.contains(AmendReturn)
}

object CheckYourAnswersViewModel{
  implicit val format: OFormat[CheckYourAnswersViewModel] = Json.format[CheckYourAnswersViewModel]

  implicit val contentType: ContentTypeOf[CheckYourAnswersViewModel] = ContentTypeOf[CheckYourAnswersViewModel](Some(ContentTypes.JSON))
  implicit val writes: Writeable[CheckYourAnswersViewModel] = Writeable(Writeable.writeableOf_JsValue.transform.compose(format.writes))
}