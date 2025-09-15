package toloka.cho.books

import cats.effect.IO
import org.scalajs.dom.document
import org.scalajs.dom.window
import toloka.cho.books.common.Language
import toloka.cho.books.common.Language.Language
import toloka.cho.books.components.{Footer, Header}
import toloka.cho.books.core.*
import toloka.cho.books.pages.Page
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object App {
  trait Msg
  case object NoOp                                     extends Msg
  case class ChangeLanguage(language: Language) extends Msg
  case object ToggleLanguageSelector extends Msg

  case class Model(router: Router, page: Page, language: Language, languageSelectorOpen: Boolean)
}

@JSExportTopLevel("TolokaApp")
class App extends TyrianApp[App.Msg, App.Model] {
  import App.*

  private val cookieName = "appLanguage"

  private def getLanguageFromCookie: Language =
    js.Dynamic.global.document.cookie
      .asInstanceOf[String]
      .split(';')
      .map(_.trim)
      .find(_.startsWith(s"$cookieName="))
      .map(_.drop(cookieName.length + 1))
      .flatMap(code => Language.languages.find(_.code == code))
      .getOrElse(Language.Ukrainian)

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val location            = window.location.pathname
    val lang                = getLanguageFromCookie
    val page                = Page.get(location, lang)
    val pageCmd             = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    (Model(router, page, lang, false), routerCmd |+| pageCmd)
  }

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make(
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case ToggleLanguageSelector =>
      (model.copy(languageSelectorOpen = !model.languageSelectorOpen), Cmd.None)
    case ChangeLanguage(lang) =>
      document.cookie = s"$cookieName=${lang.code};path=/;max-age=31536000"
      val newPage = Page.get(model.router.location, lang)
      val newPageCmd = newPage.initCmd
      (model.copy(language = lang, page = newPage, languageSelectorOpen = false), newPageCmd)
    case msg: Router.Msg =>
      println(s" Router $msg")
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) (model, Cmd.None)
      else {
        val newPage    = Page.get(newRouter.location, model.language)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
      }
    case msg: App.Msg =>
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)
  }

  override def view(model: Model): Html[Msg] =
    div(`class` := "flex-container")(
      Header.view(model.language, model.languageSelectorOpen),
      model.page.subHeader.getOrElse(div()),
      main(`class` := "flex-grow-1")(
        div(`class` := "container mx-auto p-4")(
          model.page.view()
        )
      ),
      Footer.view(model.language)
    )
}
