package com.toloka.cho.pages

import org.scalajs.dom
import com.toloka.cho.App
import com.toloka.cho.common.AuthPageTranslations
import com.toloka.cho.common.Language.Language
import com.toloka.cho.core.Router
import tyrian.Attribute
import tyrian.Html
import tyrian.Property
import tyrian.Html.*

object AuthPageFrame {
  def view(
            lang: Language,
            title: String,
            subtitle: String,
            content: List[Html[App.Msg]],
            status: Option[Page.Status],
            brandOnLeft: Boolean,
            prompt: Option[(String, String, String)] = None
          ): Html[App.Msg] = {
    implicit val language: Language = lang
    val brand = div(cls := "auth-brand-panel")(
      div(cls := "auth-brand-content")(
        img(src := "/img/toloka.png", alt := "Toloka", cls := "auth-brand-logo"),
        h2(AuthPageTranslations.get("auth.brand")),
        prompt.map { case (copy, linkText, location) =>
          div(cls := "auth-brand-prompt")(
            p(copy),
            navLink(location, linkText, "auth-brand-button")
          )
        }.getOrElse(div())
      )
    )
    val formContent =
      List(
        h1(title),
        p(cls := "auth-form-subtitle")(subtitle),
        status.map { current =>
          div(
            cls := (if current.kind == Page.StatusKind.ERROR then "auth-status auth-status-error" else "auth-status auth-status-success"),
            Attribute("role", "status")
          )(current.message)
        }.getOrElse(div())
      ) ++ content :+ navLink(
        Page.Urls.HOME,
        AuthPageTranslations.get("auth.back.home"),
        "auth-home-button"
      )
    val formPanel = main(cls := "auth-form-panel")(
      div(cls := "auth-form-content")(formContent*)
    )
    div(cls := (if brandOnLeft then "auth-page auth-brand-first" else "auth-page"))(
      if brandOnLeft then brand else formPanel,
      if brandOnLeft then formPanel else brand
    )
  }

  def textInput(
                 label: String,
                 inputType: String,
                 placeholder: String,
                 value: String,
                 autocomplete: String,
                 onChange: String => App.Msg,
                 minimumLength: Option[Int] = None
               ): Html[App.Msg] =
    input(
      `type` := inputType,
      Attribute("placeholder", placeholder),
      Property("value", value),
      Attribute("autocomplete", autocomplete),
      required(true),
      maxLength := 254,
      Attribute("minlength", minimumLength.fold("0")(_.toString)),
      Attribute("aria-label", label),
      onInput(onChange)
    )

  def submitButton(label: String, isDisabled: Boolean = false): Html[App.Msg] =
    button(`type` := "submit", cls := "auth-submit-button", disabled(isDisabled))(label)

  def passwordInput(
    label: String,
    value: String,
    autocomplete: String,
    onChange: String => App.Msg,
    visible: Boolean,
    onToggle: App.Msg,
    isNewPassword: Boolean = false,
    inputId: Option[String] = None,
    isDisabled: Boolean = false
  )(implicit language: Language): Html[App.Msg] =
    div(cls := "auth-password-field")(
      input(
        `type` := (if visible then "text" else "password"),
        Attribute("id", inputId.getOrElse(s"password-${label.hashCode}")),
        Attribute("placeholder", label),
        Property("value", value),
        Attribute("autocomplete", autocomplete),
        Attribute("aria-label", label),
        required(true),
        disabled(isDisabled),
        maxLength := (if isNewPassword then 64 else 254),
        Attribute("minlength", if isNewPassword then "8" else "0"),
        onInput(onChange)
      ),
      button(
        `type` := "button",
        cls := "auth-password-toggle",
        disabled(isDisabled),
        Attribute("aria-label", s"${AuthPageTranslations.get(if visible then "auth.password.hide" else "auth.password.show")}: $label"),
        Attribute("aria-pressed", visible.toString),
        onClick(onToggle)
      )(
        i(
          cls := (if visible then "fa-solid fa-eye-slash" else "fa-solid fa-eye"),
          Attribute("aria-hidden", "true")
        )()
      )
    )

  def navLink(location: String, label: String, className: String = "auth-text-link"): Html[App.Msg] =
    a(
      href := location,
      cls := className,
      onEvent(
        "click",
        (event: dom.Event) =>
          event.preventDefault()
          Router.ChangeLocation(location)
      )
    )(label)

  def form(content: List[Html[App.Msg]], onSubmit: App.Msg): Html[App.Msg] =
    Html.form(
      cls := "auth-form",
      onEvent(
        "submit",
        (event: dom.Event) =>
          event.preventDefault()
          onSubmit
      )
    )(content*)

  def emailIsValid(email: String): Boolean =
    email.length <= 254 && email.matches(
      """^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$"""
    )
}
