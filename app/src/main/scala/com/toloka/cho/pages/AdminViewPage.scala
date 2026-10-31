package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.App
import com.toloka.cho.common.{AdminPageTranslations, Constants, Endpoint}
import com.toloka.cho.core.Session
import com.toloka.cho.common.Language.Language
import com.toloka.cho.domain.admin.AdminDashboardStats
import io.circe.generic.auto.*
import tyrian.Html.*
import tyrian.{Attribute, Cmd, Html}
import tyrian.http.{HttpError, Method, Response}


final case class AdminViewPage(
    lang: Language,
    stats: Option[AdminDashboardStats] = None,
    statsError: Boolean = false
) extends Page:
  private implicit val language: Language = lang

  private val overdueBorrowers = List(
    ("Samantha Connors", "B-0142"),
    ("Sarah Connors", "B-0188"),
    ("Smith Connor", "B-0205"),
    ("Samantha Connor", "B-0273"),
    ("Sam Connor", "B-0311")
  )

  private val libraryTeam = List(
    ("Nisal Gunasekara", "01", true, true),
    ("Nisal Gunasekara", "02", false, true),
    ("Nisal Gunasekara", "03", false, false),
    ("Nisal Gunasekara", "04", false, true)
  )

  override def isStandalone: Boolean = true

  override def initCmd: Cmd[IO, App.Msg] = AdminViewPage.Endpoints.getStats.callAuthorized()

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AdminViewPage.StatsLoaded(loadedStats) => (copy(stats = Some(loadedStats), statsError = false), Cmd.None)
    case AdminViewPage.StatsFailed(_)           => (copy(statsError = true), Cmd.None)
    case _                                       => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "admin-dashboard")(
      sidebar,
      div(cls := "admin-workspace")(
        topBar,
        main(cls := "admin-dashboard-grid")(
          borrowingChart,
          div(cls := "admin-stats")(
            statCard("fa-users", stats.fold("—")(_.userCount.toString), AdminPageTranslations.get("admin.user.total")),
            statCard("fa-book-open", stats.fold("—")(_.copyCount.toString), AdminPageTranslations.get("admin.books.total")),
            if statsError then div(cls := "event-status event-status-error")(AdminPageTranslations.get("admin.stats.error"))
            else div()
          ),
          overduePanel,
          teamPanel
        )
      )
    )

  private def sidebar: Html[App.Msg] =
    aside(cls := "admin-sidebar")(
      a(href := "/", cls := "admin-brand", title := "CHO")(
        img(src := "/img/toloka.png", alt := "CHO", cls := "admin-brand-image")
      ),
      nav(cls := "admin-navigation")(
        navItem("fa-chart-pie", AdminPageTranslations.get("admin.dashboard"), "/admin", active = true),
        navItem("fa-table-cells-large", AdminPageTranslations.get("admin.catalog"), "/books"),
        navItem("fa-book", AdminPageTranslations.get("admin.books"), "/books"),
        navItem("fa-users", AdminPageTranslations.get("admin.users"), "/admin#users")
      ),
      a(
        href := "/",
        cls  := "admin-logout",
        onEvent(
          "click",
          e =>
            e.preventDefault()
            Session.Logout
        )
      )(
        i(cls := "fa-solid fa-right-from-bracket")(""),
        span(AdminPageTranslations.get("admin.logout"))
      )
    )

  private def navItem(icon: String, label: String, target: String, active: Boolean = false): Html[App.Msg] =
    a(
      href  := target,
      cls   := (if active then "admin-nav-item admin-nav-item-active" else "admin-nav-item"),
      title := label
    )(
      i(cls := s"fa-solid $icon")(""),
      span(label)
    )

  private def topBar: Html[App.Msg] =
    header(cls := "admin-topbar")(
      div(cls := "admin-current-user")(
        div(cls := "admin-avatar")(AuthSession.initials.getOrElse("?")),
        div(
          strong(AuthSession.displayName.getOrElse(AdminPageTranslations.get("admin.user.unknown"))),
          span(
            AuthSession.role
              .map {
                case "ADMIN"     => AdminPageTranslations.get("admin.role.admin")
                case "LIBRARIAN" => AdminPageTranslations.get("admin.role.librarian")
                case other       => other
              }
              .getOrElse(AdminPageTranslations.get("admin.role.unknown"))
          )
        )
      ),
      div(cls := "admin-workspace-title")(
        AdminPageTranslations.get("admin.workspace")
      ),
      button(`type` := "button", cls := "admin-settings", title := "Settings", disabled(true))(
        i(cls := "fa-solid fa-gear")("")
      )
    )

  private def statCard(icon: String, value: String, label: String): Html[App.Msg] =
    article(cls := "admin-stat-card")(
      div(cls := "admin-stat-icon")(
        i(cls := s"fa-solid $icon")("")
      ),
      div(
        strong(value),
        span(label)
      )
    )

  private def borrowingChart: Html[App.Msg] =
    section(cls := "admin-chart-panel", Attribute("aria-label", "Borrowing overview"))(
      div(
        cls := "admin-pie-chart",
        Attribute("role", "img"),
        Attribute("aria-label", "62% borrowed, 38% returned")
      )(),
      div(cls := "admin-chart-legend")(
        strong(AdminPageTranslations.get("admin.libraryName")),
        span(cls := "admin-legend-item")(
          span(cls := "admin-legend-dot admin-legend-borrowed")(),
          text(AdminPageTranslations.get("admin.borrowed"))
        ),
        span(cls := "admin-legend-item")(
          span(cls := "admin-legend-dot admin-legend-returned")(),
          text(AdminPageTranslations.get("admin.returned"))
        )
      )
    )

  private def overduePanel: Html[App.Msg] =
    section(cls := "admin-panel admin-overdue-panel")(
      h2(AdminPageTranslations.get("admin.overdue")),
      ul(
        overdueBorrowers.map { case (name, copyId) =>
          li(
            div(cls := "admin-list-person")(
              i(cls := "fa-regular fa-user")(""),
              span(
                strong(name),
                small(s"Borrowed ID: $copyId")
              )
            ),
            button(`type` := "button", cls := "admin-refresh", title := s"Refresh $name", disabled(true))(
              i(cls := "fa-solid fa-arrows-rotate")("")
            )
          )
        }
      ),
      button(`type` := "button", cls := "admin-next-button", disabled(true))(
        AdminPageTranslations.get("admin.next")
      )
    )

  private def teamPanel: Html[App.Msg] =
    section(cls := "admin-panel admin-team-panel", Attribute("id", "users"))(
      h2(AdminPageTranslations.get("admin.team")),
      div(cls := "admin-team-list")(
        libraryTeam.map { case (name, id, isAdmin, active) =>
          div(cls := "admin-team-member")(
            i(cls := (if isAdmin then "fa-solid fa-shield-halved" else "fa-solid fa-user-tie"))(""),
            div(
              strong(name),
              small(
                s"${AdminPageTranslations.get(if isAdmin then "admin.role.admin" else "admin.role.librarian")} ID: $id"
              )
            ),
            span(cls := (if active then "admin-member-active" else "admin-member-inactive"))(
              if active then AdminPageTranslations.get("admin.active")
              else AdminPageTranslations.get("admin.inactive")
            ),
            i(cls := "fa-solid fa-arrows-rotate admin-member-refresh")("")
          )
        }
      )
    )

object AdminViewPage:
  trait Msg extends App.Msg
  final case class StatsLoaded(stats: AdminDashboardStats) extends Msg
  final case class StatsFailed(error: String) extends Msg

  object Endpoints:
    val getStats = new Endpoint[Msg]:
      override val location: String = Constants.endpoints.adminStats
      override val method: Method = Method.Get
      override val onError: HttpError => Msg = error => StatsFailed(error.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[AdminDashboardStats, Msg](StatsLoaded(_), error => StatsFailed(error))
