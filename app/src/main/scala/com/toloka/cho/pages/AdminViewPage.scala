package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.App
import com.toloka.cho.common.{AdminPageTranslations, Constants, Endpoint}
import com.toloka.cho.core.Session
import com.toloka.cho.common.Language.Language
import com.toloka.cho.domain.admin.AdminDashboardStats
import com.toloka.cho.domain.AuthValidation
import com.toloka.cho.domain.auth.NewPasswordInfo
import com.toloka.cho.core.Router
import io.circe.generic.auto.*
import org.scalajs.dom
import tyrian.Html.*
import tyrian.{Attribute, Cmd, Html}
import tyrian.http.{Header, HttpError, Method, Response}


final case class AdminViewPage(
    lang: Language,
    stats: Option[AdminDashboardStats] = None,
    statsError: Boolean = false,
    currentTime: String = AdminViewPage.localTime,
    settingsOpen: Boolean = false,
    currentPassword: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    status: Option[Page.Status] = None,
    savingPassword: Boolean = false,
    currentPasswordVisible: Boolean = false,
    newPasswordVisible: Boolean = false,
    confirmPasswordVisible: Boolean = false
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
    case AdminViewPage.ClockTick =>
      (copy(currentTime = AdminViewPage.localTime), Cmd.None)
    case AdminViewPage.OpenSettings =>
      (copy(settingsOpen = true, status = None, currentPasswordVisible = false,
        newPasswordVisible = false, confirmPasswordVisible = false), Cmd.None)
    case AdminViewPage.ToggleCurrentPassword =>
      (copy(currentPasswordVisible = !currentPasswordVisible), Cmd.None)
    case AdminViewPage.ToggleNewPassword =>
      (copy(newPasswordVisible = !newPasswordVisible), Cmd.None)
    case AdminViewPage.ToggleConfirmPassword =>
      (copy(confirmPasswordVisible = !confirmPasswordVisible), Cmd.None)
    case AdminViewPage.CloseSettings if !savingPassword =>
      (copy(settingsOpen = false, currentPassword = "", newPassword = "", confirmPassword = "", status = None), Cmd.None)
    case AdminViewPage.CurrentPasswordChanged(value) =>
      (copy(currentPassword = value, status = None), Cmd.None)
    case AdminViewPage.NewPasswordChanged(value) =>
      (copy(newPassword = value, status = None), Cmd.None)
    case AdminViewPage.ConfirmPasswordChanged(value) =>
      (copy(confirmPassword = value, status = None), Cmd.None)
    case AdminViewPage.SubmitPasswordChange if !savingPassword =>
      if currentPassword.isEmpty then
        (withError("admin.settings.currentRequired"), Cmd.None)
      else if !AuthValidation.passwordIsValid(newPassword) then
        (withError("admin.settings.passwordPolicy"), Cmd.None)
      else if newPassword == currentPassword then
        (withError("admin.settings.passwordUnchanged"), Cmd.None)
      else if newPassword != confirmPassword then
        (withError("admin.settings.passwordMismatch"), Cmd.None)
      else
        AuthSession.authorization match
          case Some(authorization) =>
            (
              copy(status = None, savingPassword = true),
              AdminViewPage.changePassword(NewPasswordInfo(currentPassword, newPassword), authorization)
            )
          case None => (withError("admin.settings.sessionExpired"), Cmd.None)
    case AdminViewPage.PasswordChanged | AdminViewPage.SessionExpired =>
      (
        copy(settingsOpen = false, currentPassword = "", newPassword = "", confirmPassword = "", savingPassword = false),
        Cmd.Run[IO, Unit, App.Msg](AuthSession.clear)(_ => Router.ChangeLocation(Page.Urls.LOGIN))
      )
    case AdminViewPage.PasswordChangeFailed(_) =>
      (withError("admin.settings.passwordFailed"), Cmd.None)
    case _                                       => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "admin-dashboard")(
      sidebar(),
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
        ),
        if settingsOpen then settingsDialog else div()
      )
    )

  private def sidebar(activeBooks: Boolean = false, activeUsers: Boolean = false): Html[App.Msg] =
    aside(cls := "admin-sidebar")(
      a(href := "/", cls := "admin-brand", title := "CHO")(
        img(src := "/img/toloka.png", alt := "CHO", cls := "admin-brand-image")
      ),
      nav(cls := "admin-navigation")(
        navItem("fa-chart-pie", AdminPageTranslations.get("admin.dashboard"), "/admin", active = !activeBooks && !activeUsers),
        navItem("fa-table-cells-large", AdminPageTranslations.get("admin.catalog"), "/books"),
        navItem("fa-book", AdminPageTranslations.get("admin.books"), "/admin/books", active = activeBooks),
        navItem("fa-users", AdminPageTranslations.get("admin.users"), "/admin/users", active = activeUsers)
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

  private[pages] def sidebarForBooks: Html[App.Msg] = sidebar(activeBooks = true)
  private[pages] def sidebarForUsers: Html[App.Msg] = sidebar(activeUsers = true)

  private def navItem(icon: String, label: String, target: String, active: Boolean = false): Html[App.Msg] =
    a(
      href  := target,
      cls   := (if active then "admin-nav-item admin-nav-item-active" else "admin-nav-item"),
      title := label
    )(
      i(cls := s"fa-solid $icon")(""),
      span(label)
    )

  private[pages] def currentUser: Html[App.Msg] =
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
    )

  private def topBar: Html[App.Msg] =
    header(cls := "admin-topbar")(
      currentUser,
      div(cls := "admin-workspace-title")(
        AdminPageTranslations.get("admin.workspace")
      ),
      div(cls := "admin-topbar-actions")(
        div(cls := "admin-clock", Attribute("aria-label", AdminPageTranslations.get("admin.localTime")))(
          i(cls := "fa-regular fa-clock")(""),
          span(currentTime)
        ),
        button(
          `type` := "button",
          cls := "admin-settings",
          title := AdminPageTranslations.get("admin.settings.title"),
          Attribute("aria-label", AdminPageTranslations.get("admin.settings.title")),
          onClick(AdminViewPage.OpenSettings)
        )(i(cls := "fa-solid fa-gear")(""))
      )
    )

  private def settingsDialog: Html[App.Msg] =
    div(
      cls := "admin-modal-backdrop",
      onEvent("click", (event: dom.MouseEvent) =>
        if event.target == event.currentTarget then AdminViewPage.CloseSettings else App.NoOp
      )
    )(
      section(
        cls := "admin-settings-dialog",
        Attribute("role", "dialog"),
        Attribute("aria-modal", "true"),
        Attribute("aria-labelledby", "admin-settings-title")
      )(
        header(cls := "admin-settings-heading")(
          div(cls := "admin-settings-icon")(i(cls := "fa-solid fa-gear")("")),
          h2(Attribute("id", "admin-settings-title"))(AdminPageTranslations.get("admin.settings.title")),
          button(
            `type` := "button",
            cls := "admin-settings-close",
            disabled(savingPassword),
            Attribute("aria-label", AdminPageTranslations.get("admin.settings.close")),
            onClick(AdminViewPage.CloseSettings)
          )("X")
        ),
        form(onEvent("submit", (event: dom.Event) =>
          event.preventDefault()
          AdminViewPage.SubmitPasswordChange
        ))(
          div(cls := "admin-settings-fields")(
            passwordField("admin-current-password", "admin.settings.current", currentPassword,
              "current-password", AdminViewPage.CurrentPasswordChanged(_),
              currentPasswordVisible, AdminViewPage.ToggleCurrentPassword),
            passwordField("admin-new-password", "admin.settings.new", newPassword,
              "new-password", AdminViewPage.NewPasswordChanged(_),
              newPasswordVisible, AdminViewPage.ToggleNewPassword),
            passwordField("admin-confirm-password", "admin.settings.confirmLabel", confirmPassword,
              "new-password", AdminViewPage.ConfirmPasswordChanged(_),
              confirmPasswordVisible, AdminViewPage.ToggleConfirmPassword)
          ),
          p(cls := "admin-settings-help")(AdminPageTranslations.get("admin.settings.passwordPolicy")),
          status.map(s => div(cls := "admin-settings-error", Attribute("role", "alert"))(s.message)).getOrElse(div()),
          div(cls := "admin-settings-actions")(
            button(`type` := "button", cls := "admin-settings-cancel", disabled(savingPassword),
              onClick(AdminViewPage.CloseSettings))(AdminPageTranslations.get("admin.settings.cancel")),
            button(`type` := "submit", cls := "admin-settings-confirm", disabled(savingPassword))(
              AdminPageTranslations.get(if savingPassword then "admin.settings.saving" else "admin.settings.confirm")
            )
          )
        )
      )
    )

  private def passwordField(
      id: String,
      labelKey: String,
      fieldValue: String,
      autocomplete: String,
      onChange: String => App.Msg,
      visible: Boolean,
      onToggle: App.Msg
  ): Html[App.Msg] =
    div(cls := "admin-settings-field")(
      label(Attribute("for", id))(AdminPageTranslations.get(labelKey)),
      AuthPageFrame.passwordInput(
        AdminPageTranslations.get(labelKey), fieldValue, autocomplete, onChange,
        visible, onToggle, isNewPassword = autocomplete == "new-password",
        inputId = Some(id), isDisabled = savingPassword
      )
    )

  private def withError(key: String): AdminViewPage =
    copy(status = Some(Page.Status(AdminPageTranslations.get(key), Page.StatusKind.ERROR)), savingPassword = false)

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
  case object ClockTick extends Msg
  case object OpenSettings extends Msg
  case object CloseSettings extends Msg
  case object ToggleCurrentPassword extends Msg
  case object ToggleNewPassword extends Msg
  case object ToggleConfirmPassword extends Msg
  final case class CurrentPasswordChanged(value: String) extends Msg
  final case class NewPasswordChanged(value: String) extends Msg
  final case class ConfirmPasswordChanged(value: String) extends Msg
  case object SubmitPasswordChange extends Msg
  case object PasswordChanged extends Msg
  case object SessionExpired extends Msg
  final case class PasswordChangeFailed(message: String) extends Msg
  final case class StatsLoaded(stats: AdminDashboardStats) extends Msg
  final case class StatsFailed(error: String) extends Msg

  def localTime: String =
    val now = new scala.scalajs.js.Date()
    f"${now.getHours().toInt}%02d:${now.getMinutes().toInt}%02d"

  private def changePassword(payload: NewPasswordInfo, authorization: String): Cmd[IO, App.Msg] =
    AuthApi.post[NewPasswordInfo, Msg](
      Constants.endpoints.changePassword,
      payload,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then PasswordChanged
        else if response.status.code == 401 then SessionExpired
        else PasswordChangeFailed(AuthApi.failure(response)),
      error => PasswordChangeFailed(error.toString),
      List(Header("Authorization", authorization)),
      Method.Put
    )

  object Endpoints:
    val getStats = new Endpoint[Msg]:
      override val location: String = Constants.endpoints.adminStats
      override val method: Method = Method.Get
      override val onError: HttpError => Msg = error => StatsFailed(error.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[AdminDashboardStats, Msg](StatsLoaded(_), error => StatsFailed(error))
