package toloka.cho.books.pages

import cats.effect.IO
import cats.implicits.*
import com.toloka.cho.domain.auth.{LoginInfo, LoginResponse}
import io.circe.generic.auto.*
import io.circe.parser.decode
import org.scalajs.dom
import toloka.cho.books.App
import toloka.cho.books.common.AuthPageTranslations
import toloka.cho.books.common.Language.Language
import toloka.cho.books.core.Router
import tyrian.Cmd
import tyrian.Html
import tyrian.http.Response

final case class LoginPage(
                            lang: Language,
                            email: String = "",
                            password: String = "",
                            status: Option[Page.Status] = None
                          ) extends Page:
  private implicit val language: Language = lang

  override def isStandalone: Boolean = true
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case LoginPage.EmailChanged(value) => (copy(email = value, status = None), Cmd.None)
    case LoginPage.PasswordChanged(value) => (copy(password = value, status = None), Cmd.None)
    case LoginPage.Submit =>
      if (!AuthPageFrame.emailIsValid(email.trim))
        (withError(AuthPageTranslations.get("auth.email.invalid")), Cmd.None)
      else if (password.isEmpty)
        (withError(AuthPageTranslations.get("auth.login.failed")), Cmd.None)
      else
        (
          copy(status = None),
          AuthApi.post(
            AuthApi.endpointForLogin,
            LoginInfo(email.trim, password),
            LoginPage.onLoginResponse,
            error => LoginPage.RequestFailed(error.toString)
          )
        )
    case LoginPage.LoginSucceeded(authorization, response) =>
      decode[LoginResponse](response.body) match
        case Left(_) => (withError(AuthPageTranslations.get("auth.request.failed")), Cmd.None)
        case Right(user) =>
          val destination =
            if user.role == "ADMIN" || user.role == "LIBRARIAN" then Page.Urls.ADMIN
            else Page.Urls.BOOKS
          val saveSession = Cmd.SideEffect[IO](AuthSession.save(authorization, user.role))
          (this, saveSession |+| Cmd.Emit(Router.ChangeLocation(destination)))
    case LoginPage.RequestFailed(_) =>
      (withError(AuthPageTranslations.get("auth.login.failed")), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    AuthPageFrame.view(
      lang,
      AuthPageTranslations.get("auth.login.title"),
      AuthPageTranslations.get("auth.login.subtitle"),
      List(
        AuthPageFrame.form(
          List(
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.email"),
              "email",
              AuthPageTranslations.get("auth.email"),
              email,
              "username",
              LoginPage.EmailChanged(_)
            ),
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.password"),
              "password",
              AuthPageTranslations.get("auth.password"),
              password,
              "current-password",
              LoginPage.PasswordChanged(_)
            ),
            AuthPageFrame.navLink(Page.Urls.FORGOT_PASSWORD, AuthPageTranslations.get("auth.forgot.link")),
            AuthPageFrame.submitButton(AuthPageTranslations.get("auth.login.submit"))
          ),
          LoginPage.Submit
        )
      ),
      status,
      brandOnLeft = false,
      prompt = Some((
        AuthPageTranslations.get("auth.login.prompt"),
        AuthPageTranslations.get("auth.signup.cta"),
        Page.Urls.SIGNUP
      ))
    )

  private def withError(message: String): LoginPage =
    copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

object LoginPage:
  sealed trait Msg extends App.Msg
  final case class EmailChanged(value: String) extends Msg
  final case class PasswordChanged(value: String) extends Msg
  case object Submit extends Msg
  final case class LoginSucceeded(authorization: String, response: Response) extends Msg
  final case class RequestFailed(message: String) extends Msg

  private def onLoginResponse(response: Response): Msg =
    if response.status.code >= 200 && response.status.code < 300 then
      response.headers.get("authorization") match
        case Some(authorization) => LoginSucceeded(authorization, response)
        case None                => RequestFailed("Missing authorization header")
    else RequestFailed(AuthApi.failure(response))
