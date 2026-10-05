package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.auth.ForgotPasswordInfo
import io.circe.generic.auto.*
import toloka.cho.books.App
import toloka.cho.books.common.AuthPageTranslations
import toloka.cho.books.common.Language.Language
import tyrian.Cmd
import tyrian.Html
import tyrian.http.{HttpError, Response}

final case class ForgotPasswordPage(
                                     lang: Language,
                                     email: String = "",
                                     status: Option[Page.Status] = None
                                   ) extends Page:
  private implicit val language: Language = lang

  override def isStandalone: Boolean = true
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case ForgotPasswordPage.EmailChanged(value) => (copy(email = value, status = None), Cmd.None)
    case ForgotPasswordPage.Submit =>
      if (!AuthPageFrame.emailIsValid(email.trim))
        (withError(AuthPageTranslations.get("auth.email.invalid")), Cmd.None)
      else
        (
          copy(status = None),
          ForgotPasswordPage.send(ForgotPasswordInfo(email.trim))
        )
    case ForgotPasswordPage.Sent =>
      (copy(status = Some(Page.Status(AuthPageTranslations.get("auth.forgot.success"), Page.StatusKind.SUCCESS))), Cmd.None)
    case ForgotPasswordPage.RequestFailed(message) => (withError(message), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    AuthPageFrame.view(
      lang,
      AuthPageTranslations.get("auth.forgot.title"),
      AuthPageTranslations.get("auth.forgot.subtitle"),
      List(
        AuthPageFrame.form(
          List(
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.email"),
              "email",
              AuthPageTranslations.get("auth.email"),
              email,
              "email",
              ForgotPasswordPage.EmailChanged(_)
            ),
            AuthPageFrame.submitButton(AuthPageTranslations.get("auth.forgot.submit")),
            AuthPageFrame.navLink(Page.Urls.RECOVER_PASSWORD, AuthPageTranslations.get("auth.recover.link")),
            AuthPageFrame.navLink(Page.Urls.LOGIN, AuthPageTranslations.get("auth.back.login"))
          ),
          ForgotPasswordPage.Submit
        )
      ),
      status,
      brandOnLeft = true
    )

  private def withError(message: String): ForgotPasswordPage =
    copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

object ForgotPasswordPage:
  sealed trait Msg extends App.Msg
  final case class EmailChanged(value: String) extends Msg
  case object Submit extends Msg
  case object Sent extends Msg
  final case class RequestFailed(message: String) extends Msg

  private def send(payload: ForgotPasswordInfo): Cmd[IO, App.Msg] =
    AuthApi.post[ForgotPasswordInfo, Msg](
      AuthApi.endpointForForgotPassword,
      payload,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then Sent
        else RequestFailed(AuthApi.failure(response)),
      error => RequestFailed(error.toString)
    )
