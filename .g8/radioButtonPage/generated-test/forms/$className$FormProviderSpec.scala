package uk.gov.hmrc.economiccrimelevyreturns.forms

import uk.gov.hmrc.economiccrimelevyreturns.forms.behaviours.OptionFieldBehaviours
import uk.gov.hmrc.economiccrimelevyreturns.models.$className$
import play.api.data.FormError

class $className$FormProviderSpec extends OptionFieldBehaviours {

  val form = new $className$FormProvider()()

  "value" should {

    val fieldName = "value"
    val requiredKey = "$className;format="decap"$.error.required"

    behave like optionsField[$className$](
      form,
      fieldName,
      validValues  = $className$.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
