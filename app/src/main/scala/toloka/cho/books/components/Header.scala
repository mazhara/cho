package toloka.cho.books.components

import toloka.cho.books.App
import toloka.cho.books.common.Language.Language
import toloka.cho.books.common.HeaderTranslations
import tyrian.Html._
import tyrian._

object Header {

  def view(lang: Language, languageSelectorOpen: Boolean): Html[App.Msg] = {
    implicit val language: Language = lang

    header(cls := "header")(
      nav(cls := "nav-container")(
        // Left Menu
        ul(cls := "nav-menu calibri font-extrabold")(
          li()(a(href := "/books")(HeaderTranslations.get("header.books"))),
//          li()(a(href := "/events")(HeaderTranslations.get("header.events"))),
          li()(a(href := "/")(HeaderTranslations.get("header.about")))
        ),

        // Center Logo
        div(cls := "logo-container")(
          a(href := "/")(
            img(src := "/img/cho.png", alt := "CHO Logo", cls := "header-logo")
          )
        ),

        // Right Search Bar & Language Selector
        div(cls := "search-container")(
          LanguageSelector.view(languageSelectorOpen, lang),
          div(cls := "search-bar")(
            form(action := "/search")(
              input(
                `type` := "text",
                placeholder := HeaderTranslations.get("header.search"),
                cls := "search-input"
              ),
              button(`type` := "submit", cls := "search-button")(
                i(cls := "fa fa-search")("")
              )
            )
          )
        )
      )
    )
  }
}
