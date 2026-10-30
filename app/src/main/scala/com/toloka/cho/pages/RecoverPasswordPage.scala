package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.domain.auth.RecoverPasswordInfo
import io.circe.generic.auto.*
import com.toloka.cho.App
import com.toloka.cho.common.AuthPageTranslations
import com.toloka.cho.common.Language.Language
import tyrian.Cmd
import tyrian.Html
import tyrian.http.{HttpError, Response}

final case class RecoverPasswordPage(
                                      lang: Language,
                                      email: String = "",
                                      token: String = "",
                                      password: String = "",
                                      confirmPassword: String = "",
                                      override val status: Option[Page.Status] = None
                                    ) extends AuthFormPage:
  private implicit val language: Language = lang

  override protected def withStatus(status: Option[Page.Status]): Page = copy(status = status)

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case RecoverPasswordPage.EmailChanged(value) => (copy(email = value, status = None), Cmd.None)
    case RecoverPasswordPage.TokenChanged(value) => (copy(token = value, status = None), Cmd.None)
    case RecoverPasswordPage.PasswordChanged(value) => (copy(password = value, status = None), Cmd.None)
    case RecoverPasswordPage.ConfirmPasswordChanged(value) => (copy(confirmPassword = value, status = None), Cmd.None)
    case RecoverPasswordPage.Submit =>
      if (!AuthPageFrame.emailIsValid(email.trim))
        (withError(AuthPageTranslations.get("auth.email.invalid")), Cmd.None)
      else if (token.trim.isEmpty)
        (withError(AuthPageTranslations.get("auth.request.failed")), Cmd.None)
      else if (password.length < 8)
        (withError(AuthPageTranslations.get("auth.password.short")), Cmd.None)
      else if (password != confirmPassword)
        (withError(AuthPageTranslations.get("auth.password.mismatch")), Cmd.None)
      else
        (
          copy(status = None),
          RecoverPasswordPage.recover(RecoverPasswordInfo(email.trim, token.trim, password))
        )
    case RecoverPasswordPage.Recovered =>
      (withSuccess(AuthPageTranslations.get("auth.recover.success")), Cmd.None)
    case RecoverPasswordPage.RequestFailed(message) => (withError(message), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    AuthPageFrame.view(
      lang,
      AuthPageTranslations.get("auth.recover.title"),
      AuthPageTranslations.get("auth.recover.subtitle"),
      List(
        AuthPageFrame.form(
          List(
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.email"),
              "email",
              AuthPageTranslations.get("auth.email"),
              email,
              "email",
              RecoverPasswordPage.EmailChanged(_)
            ),
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.token"),
              "text",
              AuthPageTranslations.get("auth.token"),
              token,
              "one-time-code",
              RecoverPasswordPage.TokenChanged(_)
            ),
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.password.new"),
              "password",
              AuthPageTranslations.get("auth.password.new"),
              password,
              "new-password",
              RecoverPasswordPage.PasswordChanged(_),
              Some(8)
            ),
            AuthPageFrame.textInput(
              AuthPageTranslations.get("auth.password.confirm"),
              "password",
              AuthPageTranslations.get("auth.password.confirm"),
              confirmPassword,
              "new-password",
              RecoverPasswordPage.ConfirmPasswordChanged(_),
              Some(8)
            ),
            AuthPageFrame.submitButton(AuthPageTranslations.get("auth.recover.submit")),
            AuthPageFrame.navLink(Page.Urls.FORGOT_PASSWORD, AuthPageTranslations.get("auth.forgot.link")),
            AuthPageFrame.navLink(Page.Urls.LOGIN, AuthPageTranslations.get("auth.back.login"))
          ),
          RecoverPasswordPage.Submit
        )
      ),
      status,
      brandOnLeft = true
    )

object RecoverPasswordPage:
  sealed trait Msg extends App.Msg
  final case class EmailChanged(value: String) extends Msg
  final case class TokenChanged(value: String) extends Msg
  final case class PasswordChanged(value: String) extends Msg
  final case class ConfirmPasswordChanged(value: String) extends Msg
  case object Submit extends Msg
  case object Recovered extends Msg
  final case class RequestFailed(message: String) extends Msg

  private def recover(payload: RecoverPasswordInfo): Cmd[IO, App.Msg] =
    AuthApi.post[RecoverPasswordInfo, Msg](
      AuthApi.endpointForRecoverPassword,
      payload,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then Recovered
        else RequestFailed(AuthApi.failure(response)),
      error => RequestFailed(error.toString)
    )
