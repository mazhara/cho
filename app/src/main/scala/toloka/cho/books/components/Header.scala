package toloka.cho.books.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

import toloka.cho.books.core.* 
import toloka.cho.books.App
import toloka.cho.books.pages.Page
import toloka.cho.books.common.Constants

object Header {

  def view() = {
    div(`class` := "container-fluid p-0")(
      div(`class` := "jvm-nav")(
        div(`class` := "container")(
          nav(`class` := "navbar navbar-expand-lg navbar-light JVM-nav")(
            div(`class` := "container")(
              renderLogo(),
              button(
                `class` := "navbar-toggler",
                `type`  := "button",
                attribute("data-bs-toggle", "collapse"),
                attribute("data-bs-target", "#navbarNav"),
                attribute("aria-controls", "navbarNav"),
                attribute("aria-expanded", "false"),
                attribute("aria-label", "Toggle navigation")
              )(
                span(`class` := "navbar-toggler-icon")()
              ),
              div(`class` := "collapse navbar-collapse", id := "navbarNav")(
                ul(
                  `class` := "navbar-nav ms-auto menu align-center expanded text-center"
                )(
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )
  }

  private def renderLogo() = {
    a(
      href := Page.Urls.HOME,
      `class` := "navbar-brand",
      onEvent(
        "click",
        e => {
          e.preventDefault()
          Router.ChangeLocation("/")
        }
      )
    )(
      img(
        `class` := "home-logo",
        src     := Constants.logoImage,
        alt     := "Toloka Logo"
      )
    )
  }

  private def renderNavLinks() = {
    val constantLinks: List[Html[App.Msg]] = List(
      renderSimpleNavLink("Books", Page.Urls.BOOKS),
      renderSimpleNavLink("Add Book", Page.Urls.ADD_BOOK)
    )

    val unauthedLinks = List(
      renderSimpleNavLink("Login", Page.Urls.LOGIN),
      renderSimpleNavLink("Sign Up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      renderSimpleNavLink("Profile", Page.Urls.PROFILE),
      renderNavLink("Logout", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(locationToMsg: String => App.Msg) =
    li(`class` := "nav-item")(
      Anchors.renderNavLink(text, location, "nav-link jvm-item Home active-item")(locationToMsg)
    )

}
