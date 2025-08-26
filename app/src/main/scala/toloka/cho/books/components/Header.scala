package toloka.cho.books.components

import toloka.cho.books.App
import toloka.cho.books.common.Constants
import tyrian.*
import tyrian.Html.*

object Header {

  def view(): Html[App.Msg] = 
    header(cls := "header")(
      nav(cls := "nav-container")(
        // Left Menu
        ul(cls := "nav-menu calibri font-extrabold")(
          li()(a(href := "/books")("Книги")),
          li()(a(href := "/events")("Події")),
          li()(a(href := "/")("Про нас")),
        ),

        // Center Logo
        div(cls := "logo-container")(
          a(href := "/")(
            img(src := Constants.choLogo, alt := "CHO Logo", cls := "header-logo")
          )
        ),

        // Right Search Bar
        div(cls := "search-container")(
          form(action := "/search")(
            input(`type` := "text", placeholder := "Пошук", cls := "search-input"),
            button(`type` := "submit", cls := "search-button")(
              i(cls := "fa fa-search")("")
            )
          )
        )
      )
    )
}
