package toloka.cho.books.pages

import tyrian.*
import cats.effect.*

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }

  case class Status(message: String, kind: StatusKind)

  object Urls {
    val EMPTY            = ""
    val HOME             = "/"
    val LOGIN            = "/login"
    val SIGNUP           = "/signup"
    val FORGOT_PASSWORD  = "/forgotpassword"
    val RECOVER_PASSWORD = "/recoverpassword"
    val BOOKS             = "/books"
    val HASH             = "#"
  }

  import Urls.*
  def get(location: String) = location match {
    case `LOGIN`                   => LoginPage()
    case `SIGNUP`                  => SignupPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RECOVER_PASSWORD`        => RecoverPasswordPage()
    case `EMPTY` | `HOME` | `BOOKS` => BooksListPage()
    case s"/books/$id"              => BookPage(id)
    case _                         => NotFoundPage()
  }
}

abstract class Page {
  import toloka.cho.books.App.Msg

  def initCmd: Cmd[IO, Msg]

  def update(msg: Msg): (Page, Cmd[IO, Msg])

  def view(): Html[Msg]
}
