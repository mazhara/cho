package toloka.cho.books.pages

import cats.effect.IO
import toloka.cho.books.App
import toloka.cho.books.common.Constants
import toloka.cho.books.core.Router
import toloka.cho.books.pages.AdminViewPage.{Msg, ToggleSidebar}
import tyrian.{Cmd, Html}
import tyrian.Html.*


final case class AdminViewPage() extends Page:

  override def view(): Html[App.Msg] =
    div(
      cls := "flex flex-col md:flex-row h-screen overflow-hidden")(
      sidebar,
      div(
        cls := "main-content flex-1 bg-gray-100 p-4 md:ml-200 overflow-auto")(
        mobileSidebarToggleButton(true),//model.sidebarOpen - fixme
        header,
        div(cls := "flex flex-col lg:flex-row gap-4 mt-4")(
          statBox(UserIcon, "0150", "Total User Base"),
          statBox(BookIcon, "01500", "Total Book Count"),
          overdueBorrowers(List(("Test", 1))) // fixme model.overdueBorrowers
        ),
        div(cls := "flex flex-col md:flex-row mt-4 gap-4")(
          pieChartPlaceholder,
          shoAdmins(List(("Test", 1, true))) // fixme model.shoAdmins
        ),
        legend
      )
    )

  private def sidebar: Html[Msg] =
    div[Msg](
      cls := "sidebar bg-blue-200 h-full flex flex-col justify-between w-[200px] md:block hidden fixed z-10"
    )(
      // Top: logo + navigation
      div[Msg](cls := "flex flex-col items-center")(
        renderLogo(),
        div[Msg](
          cls := "flex flex-col gap-4 mt-4 w-full px-4"
        )(
          a[Msg](cls := "text-white hover:bg-teal-700 p-2 rounded flex items-center justify-center")(DashboardIcon, text("Dashboard")),
          a[Msg](cls := "text-white hover:bg-teal-700 p-2 rounded flex items-center justify-center")(CatalogIcon, text("Catalog")),
          a[Msg](cls := "text-white hover:bg-teal-700 p-2 rounded flex items-center justify-center")(BooksIcon, text("Books")),
          a[Msg](cls := "text-white hover:bg-teal-700 p-2 rounded flex items-center justify-center")(UsersIcon, text("Users"))
        )
      ),

      // Bottom: logout
      div[Msg](cls := "flex items-center justify-center p-4")(
        a[Msg](cls := "text-white hover:bg-teal-700 p-2 rounded flex items-center justify-center")(LogoutIcon, text("Log Out"))
      )
    )

  private def mobileSidebarToggleButton(isOpen: Boolean): Html[Msg] =
    button(
      cls := "md:hidden block p-2 m-2 bg-teal-700 text-white rounded",
      onClick(ToggleSidebar)
    )(
      text(if isOpen then "Close Menu" else "Open Menu")
    )

  private def header: Html[Msg] =
    div(
      cls := "header flex justify-between items-center p-4 bg-white shadow w-full"
    )(
      div(
        cls := "text-gray-600 flex items-center"
      )(
        UserIcon,
        span(cls := "ml-2")(text("Nisal Gunasekara (Admin)"))
      ),
      div(
        cls := "text-right text-sm text-gray-500"
      )(
        text("12:29 PM"),
        br(),
        text("Sep 02, 2023")
      )
    )

  def statBox(icon: Html[Msg], value: String, label: String): Html[Msg] =
    div(
      cls := "flex items-center bg-white rounded shadow p-4 gap-2 flex-1 min-w-[150px]")(
      icon,
      div(
        span(cls := "text-2xl font-bold")(text(value)),
        br(),
        span(cls := "text-sm text-gray-600")(text(label))
      )
    )

  private def overdueBorrowers(borrowers: List[(String, Int)]): Html[Msg] =
    div(
      cls := "bg-white rounded shadow p-4 flex-1"
    )(
      h4(text("Overdue Borrowers")),
      ul(
        cls := "list-none"
      )(
        borrowers.map { case (name, id) =>
          li(
            cls := "border rounded p-2 my-1 flex justify-between"
          )(
            span(UserIcon, text(s" $name Borrowed ID : $id")),
            RefreshIcon
          )
        } :+ li(cls := "text-center text-sm mt-2")(text("next"))
      )
    )

  private def shoAdmins(admins: List[(String, Int, Boolean)]): Html[Msg] =
    div(
      cls := "bg-white rounded shadow p-4 flex-1")(
      h4(text("Sho admins")),
      div(
        cls := "flex flex-col gap-2 mt-2")(
        admins.map { case (name, id, active) =>
          div(
            cls := "rounded border bg-blue-100 p-2 flex items-center gap-2")(
            AdminIcon,
            div(
              span(cls := "font-semibold")(text(name)),
              br(),
              span(cls := "text-sm")(text(s"Admin ID : $id"))
            ),
            span(cls := "text-sm ml-auto text-green-600")(text(if active then "● Active" else "○ Inactive")),
            RefreshIcon
          )
        }
      )
    )

  private def legend: Html[Msg] =
    div(
      cls := "flex flex-col md:flex-row items-start md:items-center gap-4 mt-4"
    )(
      span(cls := "font-bold text-teal-800")(text("Sho library")),
      span(cls := "flex items-center gap-1")(
        div(cls := "w-4 h-4 rounded-full bg-blue-300")(),
        text("Total Borrowed Books")
      ),
      span(cls := "flex items-center gap-1")(
        div(cls := "w-4 h-4 rounded-full bg-teal-700")(),
        text("Total Returned Books")
      )
    )

  private def pieChartPlaceholder: Html[Msg] =
    div(cls := "bg-gray-200 rounded-full w-full max-w-300 aspect-square mx-auto")()

  private def renderLogo(): Html[Msg] =
    a(
      href := Page.Urls.HOME,
      `class` := "navbar-brand",
      onEvent(
        "click",
        e =>
          e.preventDefault()
          ToggleSidebar // Map to AdminViewPage.Msg
      )
    )(
      img(
        `class` := "home-logo",
        src := Constants.logoImage,
        alt := "Toloka Logo"
      )
    )


  val DashboardIcon = i[Msg](cls := "icon-dashboard")()
  val CatalogIcon = i[Msg](cls := "icon-catalog")()
  val BooksIcon = i[Msg](cls := "icon-books")()
  val UsersIcon = i[Msg](cls := "icon-users")()
  val LogoutIcon= i[Msg](cls := "icon-logout")()
  val UserIcon = i[Msg](cls := "icon-user")()
  val BookIcon = i[Msg](cls := "icon-book")()
  val AdminIcon = i[Msg](cls := "icon-admin")()
  val RefreshIcon = i[Msg](cls := "icon-refresh cursor-pointer")()

  //fixme
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  //fixme
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None)

object AdminViewPage:
  trait Msg                                                         extends App.Msg
  case object ToggleSidebar                                         extends Msg
  case class FilterBooks(selectedFilters: Map[String, Set[String]]) extends Msg

