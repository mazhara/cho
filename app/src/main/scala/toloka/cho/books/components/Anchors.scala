package toloka.cho.books.components

import tyrian.*
import tyrian.Html.*
import toloka.cho.books.core.Router
import toloka.cho.books.App

object Anchors {
  def renderSimpleNavLink(text: String, location: String, cssClass: String = "") =
    renderNavLink(text, location, cssClass)(Router.ChangeLocation(_))
  def renderNavLink(text: String, location: String, cssClass: String = "")(locationToMsg: String => App.Msg) =
      a(
        href    := location,
        `class` := cssClass,
        onEvent(
          "click",
          e => {
            e.preventDefault()
            locationToMsg(location)
          }
        )
      )(text)
}
