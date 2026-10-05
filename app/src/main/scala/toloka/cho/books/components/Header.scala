package toloka.cho.books.components

import org.scalajs.dom
import toloka.cho.books.App
import toloka.cho.books.common.Language.Language
import toloka.cho.books.common.HeaderTranslations
import tyrian.Html.*
import tyrian.*
import toloka.cho.books.core.{Router, Session}
import toloka.cho.books.pages.Page

object Header {

  def view(lang: Language, languageSelectorOpen: Boolean, searchText: String): Html[App.Msg] = {
    header(cls := "header")(
      nav(cls := "nav-container")(
        renderNavLinks(lang),
        renderLogo(),
        renderSearch(lang, languageSelectorOpen, searchText)
      )
    )
  }


  // Left Menu
  private def renderNavLinks(lang: Language) = {
    implicit val language: Language = lang
    val constantLinks: List[Html[App.Msg]] = List(
      renderSimpleNavLink(HeaderTranslations.get("header.books"), Page.Urls.BOOKS),
      renderSimpleNavLink(HeaderTranslations.get("header.events"), Page.Urls.EVENTS),
      renderSimpleNavLink(HeaderTranslations.get("header.about"), Page.Urls.HOME)
    )

    val unauthedLinks = List(
      renderSimpleNavLink(HeaderTranslations.get("header.login"), Page.Urls.LOGIN),
      renderSimpleNavLink(HeaderTranslations.get("header.signup"), Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      renderNavLink(HeaderTranslations.get("header.logout"), Page.Urls.HASH)(_ => Session.Logout)
    )

    ul(cls := "nav-menu calibri font-extrabold")(
      constantLinks ++ (
        if (Session.isActive) authedLinks
        else unauthedLinks
        )
    )
  }

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(locationToMsg: String => App.Msg) =
    li()(
      a(
        href := location,
        onEvent(
          "click",
          e => {
            e.preventDefault()
            locationToMsg(location)
          }
        )
      )(text)
    )

  private def renderSearch(lang: Language, languageSelectorOpen: Boolean, searchText: String) = {
    implicit val language: Language = lang
    // Right Search Bar & Language Selector
    div(cls := "search-container")(
      LanguageSelector.view(languageSelectorOpen, lang),
      div(cls := "search-bar")(
        form(
          onEvent("submit", (event: dom.Event) =>
            event.preventDefault()
            App.SubmitHeaderSearch
          )
        )(
          input(
            `type` := "search",
            placeholder := HeaderTranslations.get("header.search"),
            cls := "search-input",
            value := searchText,
            maxLength := 100,
            onInput(App.HeaderSearchTextChanged.apply)
          ),
          button(`type` := "submit", cls := "search-button")(
            i(cls := "fa fa-search")("")
          )
        )
      )
    )
  }

  private def renderLogo() = {
    // Center Logo
    div(cls := "logo-container")(
      a(href := "/")(
        img(src := "/img/cho.png", alt := "CHO Logo", cls := "header-logo")
      )
    )
  }
}
