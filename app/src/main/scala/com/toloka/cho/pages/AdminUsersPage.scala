package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.domain.{AuthValidation, ManagedUser, ManagedUserForm}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.scalajs.dom
import scala.scalajs.js
import com.toloka.cho.App
import com.toloka.cho.common.Constants
import com.toloka.cho.common.Language.Language
import tyrian.Attribute
import tyrian.Cmd
import tyrian.Html
import tyrian.Html.*
import tyrian.http.{Header, Method}

final case class AdminUsersPage(
    lang: Language,
    users: List[ManagedUser] = Nil,
    search: String = "",
    submittedSearch: String = "",
    saveRequest: Int = 0,
    skip: Int = 0,
    canLoadMore: Boolean = true,
    loading: Boolean = true,
    saving: Boolean = false,
    error: Option[String] = None,
    formOpen: Boolean = false,
    editingEmail: Option[String] = None,
    email: String = "",
    username: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    phone: String = "",
    role: String = "READER",
    password: String = "",
    deleteTarget: Option[ManagedUser] = None,
    selectedUser: Option[ManagedUser] = None,
    currentTime: String = AdminViewPage.localTime
) extends Page:
  private implicit val language: Language = lang
  override def isStandalone: Boolean = true
  override def initCmd: Cmd[IO, App.Msg] = AdminUsersPage.loadUsers("", 0)

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AdminViewPage.ClockTick => (copy(currentTime = AdminViewPage.localTime), Cmd.None)
    case AdminUsersPage.UsersLoaded(result, requestedSkip) =>
      val nextUsers = if requestedSkip == 0 then result else users ++ result
      (copy(users = nextUsers, skip = nextUsers.length, canLoadMore = result.size == AdminUsersPage.pageSize, loading = false), Cmd.None)
    case AdminUsersPage.Failed(message) =>
      (copy(loading = false, saving = false, error = Some(message)), Cmd.None)
    case AdminUsersPage.SearchChanged(value) => (copy(search = value.take(100)), Cmd.None)
    case AdminUsersPage.Search =>
      (copy(submittedSearch = search.trim, loading = true, users = Nil, skip = 0, canLoadMore = true, error = None), AdminUsersPage.loadUsers(search.trim, 0))
    case AdminUsersPage.LoadMore if !loading && canLoadMore =>
      (copy(loading = true), AdminUsersPage.loadUsers(submittedSearch, skip))
    case AdminUsersPage.OpenAdd =>
      (copy(formOpen = true, selectedUser = None, deleteTarget = None, saveRequest = saveRequest + 1, saving = false,
        editingEmail = None, email = "", username = "", firstName = "", lastName = "",
        company = "", phone = "", role = "READER", password = "", error = None), Cmd.None)
    case AdminUsersPage.OpenEdit(user) =>
      (copy(
        formOpen = true,
        selectedUser = None,
        deleteTarget = None,
        saveRequest = saveRequest + 1,
        saving = false,
        editingEmail = Some(user.email),
        email = user.email,
        username = user.username,
        firstName = user.firstName.getOrElse(""),
        lastName = user.lastName.getOrElse(""),
        company = user.company.getOrElse(""),
        phone = user.phone.getOrElse(""),
        role = user.role,
        password = "",
        error = None
      ), Cmd.None)
    case AdminUsersPage.CloseForm =>
      (copy(formOpen = false, saving = false, saveRequest = saveRequest + 1, error = None), Cmd.None)
    case AdminUsersPage.ViewUser(user) =>
      (copy(selectedUser = Some(user), error = None), Cmd.None)
    case AdminUsersPage.CloseUser => (copy(selectedUser = None), Cmd.None)
    case AdminUsersPage.EmailChanged(value) => (copy(email = value.take(254), error = None), Cmd.None)
    case AdminUsersPage.UsernameChanged(value) => (copy(username = value.take(32), error = None), Cmd.None)
    case AdminUsersPage.FirstNameChanged(value) => (copy(firstName = value.take(100), error = None), Cmd.None)
    case AdminUsersPage.LastNameChanged(value) => (copy(lastName = value.take(100), error = None), Cmd.None)
    case AdminUsersPage.CompanyChanged(value) => (copy(company = value.take(150), error = None), Cmd.None)
    case AdminUsersPage.PhoneChanged(value) => (copy(phone = value.take(40), error = None), Cmd.None)
    case AdminUsersPage.RoleChanged(value) => (copy(role = value, error = None), Cmd.None)
    case AdminUsersPage.PasswordChanged(value) => (copy(password = value.take(128), error = None), Cmd.None)
    case AdminUsersPage.Save if saving => (this, Cmd.None)
    case AdminUsersPage.Save =>
      if email.trim.isEmpty || username.trim.isEmpty || firstName.trim.isEmpty || lastName.trim.isEmpty then
        (copy(error = Some("Email, username, first name, and last name are required.")), Cmd.None)
      else if !ManagedUserForm.usernameIsValid(username.trim) then
        (copy(error = Some("Username must be 3-32 letters, numbers, dots, underscores, or hyphens.")), Cmd.None)
      else if !Set("READER", "LIBRARIAN", "ADMIN").contains(role) then
        (copy(error = Some("Choose a valid user role.")), Cmd.None)
      else if phone.trim.nonEmpty && !AuthValidation.phoneFormatIsValid(phone) then
        (copy(error = Some("Enter an international phone number, for example +380501234567.")), Cmd.None)
      else if (editingEmail.isEmpty || password.nonEmpty) && !AuthValidation.passwordIsValid(password) then
        (copy(error = Some("Password must contain 8-64 characters, including an uppercase letter, a lowercase letter, a number, and a symbol.")), Cmd.None)
      else
        AuthSession.authorization match
          case Some(auth) =>
            val form = ManagedUserForm(
              editingEmail.getOrElse(email.trim),
              username.trim,
              Some(firstName.trim),
              Some(lastName.trim),
              Option(phone.trim).filter(_.nonEmpty),
              Option(company.trim).filter(_.nonEmpty),
              role,
              Option(password).filter(_.nonEmpty)
            )
            val request = saveRequest + 1
            (copy(saving = true, error = None, saveRequest = request),
              AdminUsersPage.saveUser(form, editingEmail.nonEmpty, auth, request))
          case None => (copy(error = Some("Your administrator session has expired. Sign in again.")), Cmd.None)
    case AdminUsersPage.UserSaved(request) =>
      val next = if request == saveRequest && formOpen then
        copy(formOpen = false, saving = false, password = "", saveRequest = saveRequest + 1)
      else this
      (next.copy(loading = true), AdminUsersPage.loadUsers(submittedSearch, 0))
    case AdminUsersPage.SaveFailed(request, message) if request == saveRequest && formOpen =>
      (copy(saving = false, error = Some(message)), Cmd.None)
    case AdminUsersPage.ConfirmDelete(user) => (copy(deleteTarget = Some(user), error = None), Cmd.None)
    case AdminUsersPage.CancelDelete => (copy(deleteTarget = None), Cmd.None)
    case AdminUsersPage.UserDeleted(email) =>
      (copy(deleteTarget = None, saving = false, users = users.filterNot(_.email == email)), Cmd.None)
    case AdminUsersPage.Delete(user) =>
      AuthSession.authorization match
        case Some(auth) => (copy(saving = true, error = None), AdminUsersPage.deleteUser(user.email, auth))
        case None => (copy(error = Some("Your administrator session has expired. Sign in again.")), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "admin-dashboard admin-users-page")(
      AdminViewPage(lang).sidebarForUsers,
      div(cls := "admin-workspace")(
        header(cls := "admin-topbar")(
          AdminViewPage(lang).currentUser,
          div(cls := "admin-workspace-title")("User management"),
          div(cls := "admin-topbar-actions")(
            div(cls := "admin-clock")(i(cls := "fa-regular fa-clock")(""), span(currentTime)),
            a(href := "/admin", cls := "admin-settings", Attribute("title", "Dashboard"))(i(cls := "fa-solid fa-house")(""))
          )
        ),
        main(cls := "admin-users-content")(
          div(cls := "admin-users-heading")(
            h1("User Management"),
            div(cls := "admin-users-actions")(
              button(`type` := "button", cls := "admin-user-add", onClick(AdminUsersPage.OpenAdd))(
                i(cls := "fa-solid fa-circle-plus")(""), text(" Add User")
              ),
              label(cls := "admin-user-search")(
                i(cls := "fa-solid fa-magnifying-glass")(""),
                input(
                  `type` := "search",
                  Attribute("placeholder", "Search by ID or Name"),
                  Attribute("value", search),
                  onInput(AdminUsersPage.SearchChanged(_)),
                  onEvent("keydown", (event: dom.KeyboardEvent) =>
                    if event.key == "Enter" then AdminUsersPage.Search else App.NoOp
                  )
                )
              ),
              button(`type` := "button", cls := "admin-user-search-submit", disabled(loading), onClick(AdminUsersPage.Search))("Search")
            )
          ),
          error.map(message => div(cls := "admin-users-error", Attribute("role", "alert"))(message)).getOrElse(div()),
          if loading && users.isEmpty then div(cls := "admin-users-empty")("Loading users…")
          else if users.isEmpty then div(cls := "admin-users-empty")("No users found.")
          else div(cls := "admin-users-table-wrap")(
            table(cls := "admin-users-table")(
              thead(tr(th("ID"), th("Name"), th("Email"), th("Username"), th("Role"), th("Actions"))),
              tbody(users.map(userRow))
            )
          ),
          if canLoadMore then div(cls := "admin-user-load-more")(
            button(`type` := "button", cls := "admin-user-search-submit", disabled(loading), onClick(AdminUsersPage.LoadMore))(
              if loading then "Loading…" else "Load more"
            )
          ) else div()
        ),
        if formOpen then userForm else div(),
        selectedUser.map(userDialog).getOrElse(div()),
        deleteTarget.map(deleteDialog).getOrElse(div())
      )
    )

  private def userRow(user: ManagedUser): Html[App.Msg] =
    tr(
      td(user.id.toString),
      td(s"${user.firstName.getOrElse("")} ${user.lastName.getOrElse("")}".trim),
      td(user.email),
      td(user.username),
      td(user.role.capitalize),
      td(cls := "admin-user-row-actions")(
        button(`type` := "button", Attribute("title", "View user"),
          Attribute("aria-label", s"View ${user.name}"), onClick(AdminUsersPage.ViewUser(user)))(
          i(cls := "fa-regular fa-eye")("")
        ),
        button(`type` := "button", Attribute("title", "Edit user"), onClick(AdminUsersPage.OpenEdit(user)))(
          i(cls := "fa-solid fa-pen-to-square")("")
        ),
        button(`type` := "button", Attribute("title", "Delete user"), onClick(AdminUsersPage.ConfirmDelete(user)))(
          i(cls := "fa-solid fa-trash-can")("")
        )
      )
    )

  private def userForm: Html[App.Msg] =
    modal(if editingEmail.isDefined then "Edit User" else "Add User", AdminUsersPage.CloseForm)(
      div(cls := "admin-user-form-grid")(
        field("First name", firstName, AdminUsersPage.FirstNameChanged(_), isRequired = true),
        field("Last name", lastName, AdminUsersPage.LastNameChanged(_), isRequired = true),
        field("Email", email, AdminUsersPage.EmailChanged(_), inputType = "email", isRequired = true, isDisabled = editingEmail.isDefined),
        field("Username", username, AdminUsersPage.UsernameChanged(_), isRequired = true),
        field("Phone", phone, AdminUsersPage.PhoneChanged(_), inputType = "tel"),
        field("Company", company, AdminUsersPage.CompanyChanged(_)),
        if editingEmail.isEmpty then field("Temporary password", password, AdminUsersPage.PasswordChanged(_), inputType = "password", isRequired = true)
        else field("New password (optional)", password, AdminUsersPage.PasswordChanged(_), inputType = "password"),
        div(cls := "admin-user-field admin-user-field-wide")(
          fieldset(cls := "admin-user-role-options")(
            legend("Assign role") ::
            List("READER", "LIBRARIAN", "ADMIN").map { item =>
              label(
                cls := (if role == item then "admin-user-role-choice admin-user-role-active" else "admin-user-role-choice"),
              )(
                input(`type` := "radio", name := "managed-user-role", value := item,
                  checked(role == item), onChange(AdminUsersPage.RoleChanged(item))),
                text(item.capitalize)
              )
            }
          ),
          small("Reader: library member. Librarian: manage books and copies. Admin: also manage users.")
        )
      ),
      small("Username: 3-32 letters, numbers, dots, underscores or hyphens. Phone is optional and must include its country code."),
      p(cls := "admin-user-form-help")(
        if editingEmail.isEmpty then "Password: 8-64 characters with uppercase and lowercase letters, a number, and a symbol."
        else "Leave the password empty to keep it unchanged. A new password requires 8-64 characters with uppercase and lowercase letters, a number, and a symbol."
      ),
      error.map(message => div(cls := "admin-users-error", Attribute("role", "alert"))(message)).getOrElse(div()),
      div(cls := "admin-user-form-actions")(
        button(`type` := "button", cls := "admin-user-cancel", disabled(saving), onClick(AdminUsersPage.CloseForm))("Cancel"),
        button(`type` := "button", cls := "admin-user-add", disabled(saving), onClick(AdminUsersPage.Save))(
          if saving then "Saving…" else if editingEmail.isDefined then "Save changes" else "Create user"
        )
      )
    )

  private def userDialog(user: ManagedUser): Html[App.Msg] =
    modal("User details", AdminUsersPage.CloseUser)(
      dl(cls := "admin-user-details")(
        dt("ID"), dd(user.id.toString),
        dt("Name"), dd(user.name),
        dt("Email"), dd(user.email),
        dt("Username"), dd(user.username),
        dt("Role"), dd(user.role.capitalize),
        dt("Phone"), dd(user.phone.getOrElse("Not specified")),
        dt("Company"), dd(user.company.getOrElse("Not specified"))
      ),
      div(cls := "admin-user-form-actions")(
        button(`type` := "button", cls := "admin-user-cancel", onClick(AdminUsersPage.CloseUser))("Close"),
        button(`type` := "button", cls := "admin-user-add", onClick(AdminUsersPage.OpenEdit(user)))("Edit user / role")
      )
    )

  private def deleteDialog(user: ManagedUser): Html[App.Msg] =
    modal("Delete user?", AdminUsersPage.CancelDelete)(
      p(s"Delete ${user.email}? This action cannot be undone."),
      error.map(message => div(cls := "admin-users-error", Attribute("role", "alert"))(message)).getOrElse(div()),
      div(cls := "admin-user-form-actions")(
        button(`type` := "button", cls := "admin-user-cancel", onClick(AdminUsersPage.CancelDelete))("Cancel"),
        button(`type` := "button", cls := "admin-user-delete-confirm", disabled(saving), onClick(AdminUsersPage.Delete(user)))("Delete user")
      )
    )

  private def modal(titleText: String, close: App.Msg)(content: Html[App.Msg]*): Html[App.Msg] =
    div(
      cls := "admin-modal-backdrop",
      Attribute("role", "presentation"),
      onEvent("click", (event: dom.MouseEvent) => if event.target == event.currentTarget then close else App.NoOp)
    )(
      section(cls := "admin-user-dialog", Attribute("role", "dialog"), Attribute("aria-modal", "true"))(
        header(cls := "admin-user-dialog-heading")(
          h2(titleText),
          button(`type` := "button", Attribute("title", "Close"), onClick(close))("×")
        ) :: content.toList
      )
    )

  private def field(
      labelText: String,
      value: String,
      onChange: String => App.Msg,
      inputType: String = "text",
      isRequired: Boolean = false,
      isDisabled: Boolean = false
  ): Html[App.Msg] =
    div(cls := "admin-user-field")(
      label(Attribute("for", s"managed-user-${labelText.takeWhile(_ != '(').trim.toLowerCase.replace(' ', '-')}"))(labelText),
      input(
        id := s"managed-user-${labelText.takeWhile(_ != '(').trim.toLowerCase.replace(' ', '-')}",
        `type` := inputType,
        Attribute("value", value),
        required(isRequired),
        disabled(isDisabled),
        onInput(onChange)
      )
    )

object AdminUsersPage:
  private val pageSize = 50
  sealed trait Msg extends App.Msg
  final case class UsersLoaded(users: List[ManagedUser], requestedSkip: Int) extends Msg
  final case class Failed(message: String) extends Msg
  final case class SearchChanged(value: String) extends Msg
  case object Search extends Msg
  case object LoadMore extends Msg
  case object OpenAdd extends Msg
  final case class OpenEdit(user: ManagedUser) extends Msg
  final case class ViewUser(user: ManagedUser) extends Msg
  case object CloseUser extends Msg
  case object CloseForm extends Msg
  final case class EmailChanged(value: String) extends Msg
  final case class UsernameChanged(value: String) extends Msg
  final case class FirstNameChanged(value: String) extends Msg
  final case class LastNameChanged(value: String) extends Msg
  final case class CompanyChanged(value: String) extends Msg
  final case class PhoneChanged(value: String) extends Msg
  final case class RoleChanged(value: String) extends Msg
  final case class PasswordChanged(value: String) extends Msg
  case object Save extends Msg
  final case class UserSaved(request: Int) extends Msg
  final case class SaveFailed(request: Int, message: String) extends Msg
  final case class ConfirmDelete(user: ManagedUser) extends Msg
  case object CancelDelete extends Msg
  final case class Delete(user: ManagedUser) extends Msg
  final case class UserDeleted(email: String) extends Msg

  private def loadUsers(search: String, skip: Int): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.adminUsers}?search=${js.URIUtils.encodeURIComponent(search)}&limit=$pageSize&skip=$skip",
      Method.Get,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then
          io.circe.parser.decode[List[ManagedUser]](response.body) match
            case Right(value) => UsersLoaded(value, skip)
            case Left(error) => Failed(s"Could not read users: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      authorizationHeaders
    )

  private def saveUser(form: ManagedUserForm, updating: Boolean, auth: String, request: Int): Cmd[IO, App.Msg] =
    AuthApi.post[ManagedUserForm, Msg](
      Constants.endpoints.adminUsers,
      form,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then UserSaved(request)
        else SaveFailed(request, AuthApi.failure(response)),
      error => SaveFailed(request, error.toString),
      List(Header("Authorization", auth)),
      if updating then Method.Put else Method.Post
    )

  private def deleteUser(email: String, auth: String): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.adminUsers}?email=${js.URIUtils.encodeURIComponent(email)}",
      Method.Delete,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then UserDeleted(email)
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )

  private def authorizationHeaders: List[Header] =
    AuthSession.authorization.map(auth => Header("Authorization", auth)).toList
