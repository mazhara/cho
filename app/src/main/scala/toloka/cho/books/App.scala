package toloka.cho.books

import cats.effect.IO
import org.scalajs.dom.window
import toloka.cho.books.components.{Footer, Header}
import toloka.cho.books.core.*
import toloka.cho.books.pages.Page
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.JSExportTopLevel

object App {
  trait Msg
  case object NoOp extends Msg
  case class Model(router: Router, page: Page)
}


@JSExportTopLevel("TolokaApp")
class App extends TyrianApp[App.Msg, App.Model] {
  import App.*


  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val location            = window.location.pathname
    val page                = Page.get(location)
    val pageCmd             = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    (Model(router, page), routerCmd |+| pageCmd)
  }


  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make(
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )


  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case msg: Router.Msg =>
      println(s" Router $msg")
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) (model, Cmd.None)
      else {
        val newPage    = Page.get(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
      }
    case msg: App.Msg =>
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)
  }


  override def view(model: Model): Html[Msg] =
    div(`class` := "app")(
      Header.view(),
      main(
        div(`class` := "container-fluid p-0")(
          model.page.view()
        )
      ),
       Footer.view()
    )
}