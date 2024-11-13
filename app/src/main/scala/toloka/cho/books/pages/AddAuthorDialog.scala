package toloka.cho.books.pages

import cats.effect.IO
import tyrian.Cmd
import tyrian.Html.*
import tyrian.Html
import tyrian.http.*
import toloka.cho.books.App
import toloka.cho.books.core.Router

final case class AddAuthorDialog(dialogVisible: Boolean, dialogContent: String) extends Page {

  override def initCmd: Cmd[IO, App.Msg] = Cmd.Emit(AddAuthorDialog.ShowDialog)

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case AddAuthorDialog.ShowDialog =>
      (this.copy(dialogVisible = true), Cmd.None)
    case AddAuthorDialog.HideDialog =>
      (this.copy(dialogVisible = false), Cmd.emit(Router.ChangeLocation(Page.Urls.ADD_BOOK)))
    case AddAuthorDialog.UpdateContent(content) =>
      (this.copy(dialogContent = content), Cmd.emit(Router.ChangeLocation(Page.Urls.ADD_BOOK)))
  }

  override def view(): Html[App.Msg] = {
    val dialogBox = if (dialogVisible) {
      div(
        styles("position" -> "fixed",
              "top" -> "20%",
              "left" -> "30%",
              "width" -> "40%",
              "padding" -> "20px",
              "border" -> "1px solid #ccc",
              "backgroundColor" -> "#fff",
              "boxShadow" -> "0 0 10px rgba(0,0,0,0.2)"))(
        button(`type` := "button", onClick(AddAuthorDialog.HideDialog))("Close"),
        p(text(dialogContent))
      )
    } else  {
      p(`class` := "form-text")("Search author")
    }

    dialogBox
  }

}

object AddAuthorDialog:
  trait Msg extends App.Msg
  case object ShowDialog extends Msg
  case object HideDialog extends Msg
  case class UpdateContent(content: String) extends Msg




