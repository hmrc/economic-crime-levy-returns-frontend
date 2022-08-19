package uk.gov.hmrc.economiccrimelevyreturns.forms

import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.Mappings
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("$className;format="decap"$.error.required"
    )
  )
}
