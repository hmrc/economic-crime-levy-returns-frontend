package uk.gov.hmrc.economiccrimelevyreturns.controllers

import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions._
import uk.gov.hmrc.economiccrimelevyreturns.forms.$className$FormProvider
import javax.inject.Inject
import uk.gov.hmrc.economiccrimelevyreturns.models.Mode
import uk.gov.hmrc.economiccrimelevyreturns.navigation.Navigator
import uk.gov.hmrc.economiccrimelevyreturns.pages.$className$Page
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.economiccrimelevyreturns.views.html.$className$View
import uk.gov.hmrc.economiccrimelevyreturns.connectors.EclReturnsConnector

import scala.concurrent.{ExecutionContext, Future}

class $className$Controller @Inject()(
                                       override val messagesApi: MessagesApi,
                                       eclReturnsConnector: EclReturnsConnector,
                                       navigator: Navigator,
                                       authorise: AuthorisedAction,
                                       getReturnData: DataRetrievalAction,
                                       formProvider: $className$FormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: $className$View
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData) {
    implicit request =>

      val preparedForm = request.eclReturn.??? match { //TODO Choose the data you want to fill the form with
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getReturnData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value => {
          val updatedReturn = request.eclReturn.copy(??? = Some(value)) //TODO Choose the data you want to update

          eclReturnsConnector.updateReturn(updatedReturn).map { eclReturn =>
            Redirect(navigator.nextPage($className$Page, mode, eclReturn))
          }
        }
      )
  }
}
