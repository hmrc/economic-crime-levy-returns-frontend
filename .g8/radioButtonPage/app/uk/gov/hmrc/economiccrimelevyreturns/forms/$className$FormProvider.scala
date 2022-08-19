package uk.gov.hmrc.economiccrimelevyreturns.forms

import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyreturns.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyreturns.models.$className$
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[$className$] =
    Form(
      "value" -> enumerable[$className$]("$className;format="decap"$.error.required")
    )

}
