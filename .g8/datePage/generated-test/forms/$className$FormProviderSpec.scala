package uk.gov.hmrc.economiccrimelevyreturns.forms

import java.time.{LocalDate, ZoneOffset}

import uk.gov.hmrc.economiccrimelevyreturns.forms.behaviours.DateBehaviours

class $className$FormProviderSpec extends DateBehaviours {

  val form = new $className$FormProvider()()

  "value" should {

    val validData = datesBetween(
      min = LocalDate.of(2000, 1, 1),
      max = LocalDate.now(ZoneOffset.UTC)
    )

    behave like dateField(form, "value", validData)

    behave like mandatoryDateField(form, "value", "$className;format="decap"$.error.required.all")
  }
}
