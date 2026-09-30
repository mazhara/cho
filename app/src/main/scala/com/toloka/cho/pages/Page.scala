package com.toloka.cho.pages

import cats.effect.*
import com.toloka.cho.App
import com.toloka.cho.common.Language.Language
import com.toloka.cho.components.Component
import tyrian.*

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }

  case class Status(message: String, kind: StatusKind)
  object Status {
    val LOADING: Status = Status("Loading", StatusKind.LOADING)
  }

  object Urls {
    val EMPTY            = ""
    val HOME             = "/"
    val BOOKS            = "/books"
    val EVENTS           = "/events"
    val ADMIN            = "/admin"
    val ADMIN_BOOKS      = "/admin/books"
    val ADMIN_USERS      = "/admin/users"
    val LOGIN            = "/login"
    val SIGNUP           = "/signup"
    val FORGOT_PASSWORD  = "/forgotpassword"
    val RECOVER_PASSWORD = "/recoverpassword"
    val HASH             = "#"
    def BOOK(id: String) = s"/books/$id"
  }

  import Urls.*
  def get(location: String, lang: Language): Page = location match {
    case `HOME`       => AboutPage(lang)
    case `EMPTY`      => BooksListPage(lang)
    case `BOOKS`      => BooksListPage(lang)
    case `EVENTS`     => EventListPage(lang)
    case `ADMIN_BOOKS` if AuthSession.canAccessAdmin => AdminBooksPage(lang)
    case `ADMIN_BOOKS` => LoginPage(lang)
    case `ADMIN_USERS` if AuthSession.canManageUsers => AdminUsersPage(lang)
    case `ADMIN_USERS` if AuthSession.canAccessAdmin => AdminViewPage(lang)
    case `ADMIN_USERS` => LoginPage(lang)
    case `ADMIN` if AuthSession.canAccessAdmin => AdminViewPage(lang)
    case `ADMIN`       => LoginPage(lang)
    case `LOGIN`       => LoginPage(lang)
    case `SIGNUP`      => SignupPage(lang)
    case `FORGOT_PASSWORD` => ForgotPasswordPage(lang)
    case `RECOVER_PASSWORD` => RecoverPasswordPage(lang)
    case _            => NotFoundPage(lang)
  }
}

abstract class Page extends Component[App.Msg, Page] {
  def subHeader: Option[Html[App.Msg]] = None
  def submitHeaderSearch(query: String): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None)
  def isStandalone: Boolean = false

  // Hook letting a page tell App that handling `msg` should clear the shared header search text,
  // instead of App special-casing individual page messages (e.g. BooksListPage.SelectSection).
  def resetsHeaderSearch(msg: App.Msg): Boolean = false
}

/** Common shape of the standalone auth pages (login/signup/forgot/recover password):
  * no init command, rendered outside the header/footer chrome, and a status field that
  * gets set to an error/success message. Concrete pages only need to supply `withStatus`.
  */
abstract class AuthFormPage extends Page {
  protected def status: Option[Page.Status]
  protected def withStatus(status: Option[Page.Status]): Page

  override def isStandalone: Boolean    = true
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  protected def withError(message: String): Page =
    withStatus(Some(Page.Status(message, Page.StatusKind.ERROR)))

  protected def withSuccess(message: String): Page =
    withStatus(Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}
