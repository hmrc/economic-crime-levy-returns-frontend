package uk.gov.hmrc.economiccrimelevyreturns

import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.{MinMaxValues, Regex}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries.stringFromRegex
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn

trait TestUtils {
  val blankReturn = EclReturn.empty("", None)

  def updateContactName(eclReturn: EclReturn) = {
    val name = stringFromRegex(MinMaxValues.NameMaxLength, Regex.NameRegex).sample.get.trim
    eclReturn.copy(contactName = Some(name))
  }
}
