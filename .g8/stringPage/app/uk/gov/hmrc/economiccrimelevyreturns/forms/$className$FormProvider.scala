package uk.gov.hmrc.economiccrimelevyreturns.forms

import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.Mappings
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("$className;format="decap"$.error.required")
        .verifying(maxLength($maxLength$, "$className;format="decap"$.error.length"))
    )
}
