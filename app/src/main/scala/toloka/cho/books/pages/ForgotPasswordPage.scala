package toloka.cho.books.pages

import cats.effect.IO
import io.circe.generic.auto.*


import tyrian.Cmd
import tyrian.Html
import tyrian.Html.*

import tyrian.http.Method
import tyrian.http.HttpError
import tyrian.http.Response

import toloka.cho.books.*
import toloka.cho.books.common.Constants
import toloka.cho.books.common.Endpoint
import com.toloka.cho.domain.auth.ForgotPasswordInfo
import toloka.cho.books.components.Anchors

final case class ForgotPasswordPage(email: String = "", status: Option[Page.Status] = None)
    extends TwoSidedPage ("Reset Password", "Please enter your email to recover a password", status, false) {
  import ForgotPasswordPage.*
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateEmail(e) => (this.copy(email = e), Cmd.None)
    case AttemptResetPassword =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Please insert a valid email"), Cmd.None)
      else
        (this, Commands.resetPassword(email))
    case ResetSuccess => (setSuccessStatus("Check your email"), Cmd.None)
    case ResetFailure(error) => (setErrorStatus(error), Cmd.None)
    case _            => (this, Cmd.None)
  }

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override protected def renderPrimarySideContent(): List[Html[App.Msg]] = List(
    renderInlineInput("Email", "email", "text", true, UpdateEmail(_), "Email*", "Email*"),
    button(`type` := "button", onClick(AttemptResetPassword))("Send Email"),
    Anchors.renderSimpleNavLink("Have a token ?", Page.Urls.RESET_PASSWORD, "auth-link")
  )

  override protected def renderSecondarySideContent(): List[Html[App.Msg]] = List(
    img(src := Constants.logoImage),
    h2(`class` := "logo-h2")(
      text(Constants.cho)
    )
  )
}
object ForgotPasswordPage {
  trait Msg                              extends App.Msg
  case class UpdateEmail(email: String)  extends Msg
  case object AttemptResetPassword       extends Msg
  case class ResetFailure(error: String) extends Msg
  case object ResetSuccess               extends Msg
  object Endpoints {
    val resetPassword: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String            = Constants.endpoints.forgotPassword
      override val method: Method              = Method.Post
      override val onError: HttpError => Msg   = e => ResetFailure(e.toString())
      override val onResponse: Response => Msg = _ => ResetSuccess
    }
  }

  object Commands {
    def resetPassword(email: String) =
      Endpoints.resetPassword.call(ForgotPasswordInfo(email))
  }

}
