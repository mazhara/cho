package toloka.cho.books.components

import toloka.cho.books.App
import tyrian.*
import tyrian.Html.*

object SubHeader {

  case class MenuItem(name: String, link: String)

  def view(items: List[MenuItem], activeItem: String): Html[App.Msg] =
    div(cls := "subheader-container")(
      ul(cls := "subheader-menu")(
        items.map { item =>
          val isActive = if (item.name == activeItem) "active" else ""
          li(cls := isActive)(a(href := item.link)(item.name))
        }
      )
    )
}
