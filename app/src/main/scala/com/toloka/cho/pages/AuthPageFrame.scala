package com.toloka.cho.pages

import org.scalajs.dom
import com.toloka.cho.App
import com.toloka.cho.common.AuthPageTranslations
import com.toloka.cho.common.Language.Language
import com.toloka.cho.core.Router
import tyrian.Attribute
import tyrian.Html
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
      ) ++ content
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
      Attribute("value", value),
      Attribute("autocomplete", autocomplete),
      required(true),
      maxLength := 254,
      Attribute("minlength", minimumLength.fold("0")(_.toString)),
      Attribute("aria-label", label),
      onInput(onChange)
    )

  def submitButton(label: String, isDisabled: Boolean = false): Html[App.Msg] =
    button(`type` := "submit", cls := "auth-submit-button", disabled(isDisabled))(label)

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
