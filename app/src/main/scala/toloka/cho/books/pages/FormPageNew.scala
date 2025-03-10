package toloka.cho.books.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*

import cats.effect.IO
import toloka.cho.books.core.Router
import toloka.cho.books.App
import org.scalajs.dom.*

import scala.concurrent.duration.FiniteDuration
import org.scalajs.dom.HTMLFormElement
import toloka.cho.books.common.Constants


abstract class AuthPage(title: String, subtitle: String, status: Option[Page.Status]) extends Page:
  protected def renderFormContent(): List[Html[App.Msg]]
  override def initCmd: Cmd[IO, App.Msg] =
    clearForm()
  override def view(): Html[App.Msg] =
    renderForm()

  protected def renderForm(): Html[App.Msg] =
    div(`class` := "row")(
      div(`class` := "col-md-7")(
        div(`class` := "form-section")(
          div(`class` := "top-section")(
            h1(span(title)),
            label(text(subtitle)),
            maybeRenderErrors()
          ),
          form(
            name    := "signin",
            `class` := "form",
            id      := "form",
            onEvent(
              "submit",
              e =>
                e.preventDefault()
                App.NoOp
            )
          )(
            renderFormContent()
          )
        )
      ),
      div(`class` := "col-md-5 p-0")(
        div(`class` := "logo")(
          img(src := Constants.logoImage),
          h2(`class` := "logo-h2")(
            text(Constants.cho)
          ),
          label(`class` := "logo-label")(
            text("New to our platform? Sign Up now.")
          ),
          button(`type` := "button", `class` := "logo-button" )("SING UP"),
        )
      )
    )
  protected def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => App.Msg
  ) =
    div(`class` := "row")(
      div(`class` := "col-md-12")(
        div(`class` := "form-input")(
          label(`for` := uid, `class` := "form-label")(
            if (isRequired) span("*") else span(),
            text(name)
          ),
          input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
        )
      )
    )

  protected def renderInlineInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => App.Msg,
      inputValue: String,
      defaultValue: String = ""
  ) =
    div(`class` := "row")(
      div(`class` := "col-md-12")(
        div(`class` := "form-input")(
          input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange),  placeholder := inputValue)
        )
      )
    )


  
  protected def renderInputWithVal(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => App.Msg,
      inputValue: String
  ) =
    div(`class` := "row")(
      div(`class` := "col-md-12")(
        div(`class` := "form-input")(
          label(`for` := uid, `class` := "form-label")(
            if (isRequired) span("*") else span(),
            text(name)
          ),
          input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange), value := inputValue)
        )
      )
    )

  protected def renderToggle(
      name: String,
      uid: String,
      isRequired: Boolean,
      onChange: String => App.Msg
  ) =
    div(`class` := "row")(
      div(`class` := "col-md-12 job")(
        div(`class` := "form-check form-switch")(
          label(`for` := uid, `class` := "form-check-label")(
            if (isRequired) span("*") else span(),
            text(name)
          ),
          input(`type` := "checkbox", `class` := "form-check-input", id := uid, onInput(onChange))
        )
      )
    )

  protected def renderImageUploadInput(
      name: String,
      uid: String,
      imgSrc: Option[String],
      onChange: Option[File] => App.Msg
  ) =
    div(`class` := "form-input")(
      label(`for` := uid, `class` := "form-label")(name),
      input(
        `type`  := "file",
        `class` := "form-control",
        id      := uid,
        accept  := "image/*",
        onEvent(
          "change",
          e =>
            val imageInput = e.target.asInstanceOf[HTMLInputElement]
            val fileList   = imageInput.files
            if (fileList.length > 0)
              onChange(Some(fileList(0)))
            else
              onChange(None)
        )
      )
    )
  protected def renderTextArea(
      name: String,
      uid: String,
      isRequired: Boolean,
      onChange: String => App.Msg
  ) =
    div(`class` := "row")(
      div(`class` := "col-md-12")(
        div(`class` := "form-input")(
          label(`for` := name, `class` := "form-label")(
            if (isRequired) span("*") else span(),
            text(name)
          ),
          textarea(`class` := "form-control", id := uid, onInput(onChange))("")
        )
      )
    )

  private def maybeRenderErrors() =
    status
      .filter(s => s.kind == Page.StatusKind.ERROR && s.message.nonEmpty)
      .map(s => div(`class` := "page-status-errors")(s.message))
      .getOrElse(div())

  private def clearForm() =
    Cmd.Run[IO, Unit, App.Msg] {
      def effect: IO[Option[HTMLFormElement]] = for
        maybeForm <- IO(Option(document.getElementById("form").asInstanceOf[HTMLFormElement]))
        finalForm <-
          if (maybeForm.isEmpty) IO.sleep(FiniteDuration(100, "millis")) *> effect
          else IO(maybeForm)
      yield finalForm
      effect.map(_.foreach(_.reset()))
    }(_ => App.NoOp)
