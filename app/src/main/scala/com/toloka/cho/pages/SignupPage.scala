package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.domain.auth.NewUserInfo
import com.toloka.cho.domain.AuthValidation
import io.circe.generic.auto.*
import org.scalajs.dom
import com.toloka.cho.App
import com.toloka.cho.common.AuthPageTranslations
import com.toloka.cho.common.Language.Language
import tyrian.Cmd
import tyrian.{Attribute, Html}
import tyrian.Html.*
import tyrian.http.{HttpError, Response}

final case class SignupPage(
                             lang: Language,
                             firstName: String = "",
                             lastName: String = "",
                             phone: String = "",
                             email: String = "",
                             password: String = "",
                             confirmPassword: String = "",
                             dataProcessingConsent: Boolean = false,
                             override val status: Option[Page.Status] = None,
                             passwordVisible: Boolean = false
                           ) extends AuthFormPage:
  private implicit val language: Language = lang

  override protected def withStatus(status: Option[Page.Status]): Page = copy(status = status)

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case SignupPage.FirstNameChanged(value) => (copy(firstName = value, status = None), Cmd.None)
    case SignupPage.LastNameChanged(value) => (copy(lastName = value, status = None), Cmd.None)
    case SignupPage.PhoneChanged(value) => (copy(phone = value, status = None), Cmd.None)
    case SignupPage.EmailChanged(value) => (copy(email = value, status = None), Cmd.None)
    case SignupPage.PasswordChanged(value) => (copy(password = value, status = None), Cmd.None)
    case SignupPage.ConfirmPasswordChanged(value) => (copy(confirmPassword = value, status = None), Cmd.None)
    case SignupPage.ConsentChanged(value) => (copy(dataProcessingConsent = value, status = None), Cmd.None)
    case SignupPage.TogglePassword => (copy(passwordVisible = !passwordVisible), Cmd.None)
    case SignupPage.Submit =>
      if (firstName.trim.isEmpty || lastName.trim.isEmpty)
        (withError(AuthPageTranslations.get("auth.request.failed")), Cmd.None)
      else if (!AuthPageFrame.emailIsValid(email.trim))
        (withError(AuthPageTranslations.get("auth.email.invalid")), Cmd.None)
      else if (!AuthValidation.phoneFormatIsValid(phone))
        (withError(AuthPageTranslations.get("auth.phone.invalid")), Cmd.None)
      else if (!AuthValidation.passwordIsValid(password))
        (withError(AuthPageTranslations.get("auth.password.policy")), Cmd.None)
      else if (password != confirmPassword)
        (withError(AuthPageTranslations.get("auth.password.mismatch")), Cmd.None)
      else if (!dataProcessingConsent)
        (withError(AuthPageTranslations.get("auth.consent.required")), Cmd.None)
      else
        (
          copy(status = None),
          SignupPage.signup(
            NewUserInfo(
              email.trim,
              password,
              Option(firstName.trim).filter(_.nonEmpty),
              Option(lastName.trim).filter(_.nonEmpty),
              None,
              Some(AuthValidation.normalizePhone(phone)),
              dataProcessingConsent = true
            )
          )
        )
    case SignupPage.SignupSucceeded =>
      (withSuccess(AuthPageTranslations.get("auth.signup.success")), Cmd.None)
    case SignupPage.RequestFailed(message) => (withError(message), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    AuthPageFrame.view(
      lang,
      AuthPageTranslations.get("auth.signup.title"),
      AuthPageTranslations.get("auth.signup.subtitle"),
      List(
        AuthPageFrame.form(
          List(
            div(cls := "auth-field-grid")(
              AuthPageFrame.textInput(
                AuthPageTranslations.get("auth.firstName"),
                "text",
                AuthPageTranslations.get("auth.firstName"),
                firstName,
                "given-name",
                SignupPage.FirstNameChanged(_)
              ),
              AuthPageFrame.textInput(
                AuthPageTranslations.get("auth.lastName"),
                "text",
                AuthPageTranslations.get("auth.lastName"),
                lastName,
                "family-name",
                SignupPage.LastNameChanged(_)
              ),
              AuthPageFrame.textInput(
                AuthPageTranslations.get("auth.phone"),
                "tel",
                "+380501234567",
                phone,
                "tel",
                SignupPage.PhoneChanged(_)
              ),
              AuthPageFrame.textInput(
                AuthPageTranslations.get("auth.email"),
                "email",
                AuthPageTranslations.get("auth.email"),
                email,
                "email",
                SignupPage.EmailChanged(_)
              ),
              AuthPageFrame.passwordInput(
                AuthPageTranslations.get("auth.password"),
                password,
                "new-password",
                SignupPage.PasswordChanged(_),
                passwordVisible,
                SignupPage.TogglePassword,
                isNewPassword = true
              ),
              AuthPageFrame.passwordInput(
                AuthPageTranslations.get("auth.password.confirm"),
                confirmPassword,
                "new-password",
                SignupPage.ConfirmPasswordChanged(_),
                passwordVisible,
                SignupPage.TogglePassword,
                isNewPassword = true
              )
            ),
            p(cls := "auth-field-help")(AuthPageTranslations.get("auth.phone.invalid")),
            p(cls := "auth-field-help")(AuthPageTranslations.get("auth.password.policy")),
            label(cls := "auth-consent")(
              input(
                `type` := "checkbox",
                required(true),
                Attribute("aria-label", AuthPageTranslations.get("auth.consent")),
                onEvent(
                  "change",
                  (event: dom.Event) =>
                    SignupPage.ConsentChanged(
                      event.target.asInstanceOf[dom.HTMLInputElement].checked
                    )
                )
              ),
              span(AuthPageTranslations.get("auth.consent"))
            ),
            AuthPageFrame.submitButton(
              AuthPageTranslations.get("auth.signup.submit"),
              isDisabled = !dataProcessingConsent
            )
          ),
          SignupPage.Submit
        )
      ),
      status,
      brandOnLeft = true,
      prompt = Some((
        AuthPageTranslations.get("auth.signup.prompt"),
        AuthPageTranslations.get("auth.login.cta"),
        Page.Urls.LOGIN
      ))
    )

object SignupPage:
  sealed trait Msg extends App.Msg
  final case class FirstNameChanged(value: String) extends Msg
  final case class LastNameChanged(value: String) extends Msg
  final case class PhoneChanged(value: String) extends Msg
  final case class EmailChanged(value: String) extends Msg
  final case class PasswordChanged(value: String) extends Msg
  final case class ConfirmPasswordChanged(value: String) extends Msg
  final case class ConsentChanged(value: Boolean) extends Msg
  case object TogglePassword extends Msg
  case object Submit extends Msg
  case object SignupSucceeded extends Msg
  final case class RequestFailed(message: String) extends Msg

  private def signup(payload: NewUserInfo): Cmd[IO, App.Msg] =
    AuthApi.post[NewUserInfo, Msg](
      AuthApi.endpointForSignup,
      payload,
      response =>
        if response.status.code == 201 then SignupSucceeded
        else RequestFailed(AuthApi.failure(response)),
      error => RequestFailed(error.toString)
    )
