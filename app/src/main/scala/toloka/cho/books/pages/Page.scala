package toloka.cho.books.pages

import tyrian.*
import cats.effect.*
import toloka.cho.books.componenets.Component
import toloka.cho.books.App


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
    val RESET_PASSWORD = "/recoverpassword"
    val PROFILE         = "/profile"
    val BOOKS             = "/books"
    val POST_BOOK        = "/postbook"
    val HASH             = "#"
  }

  import Urls.*
  def get(location: String) = location match {
    case `LOGIN`                   => LoginPage()
    case `SIGNUP`                  => SignupPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RESET_PASSWORD`        => ResetPasswordPage()
    case `POST_BOOK`                => PostBookPage()
    case `PROFILE`                 => ProfilePage()
    case `EMPTY` | `HOME` | `BOOKS` => BooksListPage()
    case s"/books/$id"              => BookPage(id)
    case _                         => NotFoundPage()
  }
}

abstract class Page extends Component[App.Msg,Page]