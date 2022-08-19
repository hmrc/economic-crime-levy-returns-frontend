package uk.gov.hmrc.economiccrimelevyreturns.models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait $className$

object $className$ {

  case object $option1key;format="Camel"$ extends $className$
  case object $option2key;format="Camel"$ extends $className$

  val values: Seq[$className$] = Seq(
    $option1key;format="Camel"$, $option2key;format="Camel"$
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"$className;format="decap"$.\${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_\$index")
      )
  }

  implicit val enumerable: Enumerable[$className$] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
