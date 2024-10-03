package toloka.cho.books.pages

import scala.util.Try
import cats.effect.IO
import cats.syntax.traverse.*
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*
import tyrian.Cmd
import tyrian.Html.*
import tyrian.Html
import tyrian.http.*
import tyrian.cmds.Logger
import toloka.cho.books.core.Session
import toloka.cho.books.App
import org.scalajs.dom.File
import org.scalajs.dom.FileReader
import toloka.cho.books.common.Constants
import com.toloka.cho.domain.book.BookInfo
import toloka.cho.books.common.Endpoint


case class PostBookPage(
    name: String = "",
    author: String = "",
    description: String = "",
    publisher: String = "",
    inHallOnly: Boolean = false,
    year: Int = 0,
    tags: Option[String] = None,
    image: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post Book", status) {
  import PostBookPage.*
  override def view(): Html[App.Msg] =
    if (Session.isActive) super.view()
    else renderInvalidPage
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateName(v)     => (this.copy(name = v), Cmd.None)
    case UpdateAuthor(v)       => (this.copy(author = v), Cmd.None)
    case UpdateDescription(v) => (this.copy(description = v), Cmd.None)
    case UpdatePublisher(v) => (this.copy(publisher = v), Cmd.None)
    case ToggleHallOnly        => (this.copy(inHallOnly = !this.inHallOnly), Cmd.None)
    case UpdateYear(v)    => (this.copy(year = v), Cmd.None)
    case UpdateImageFile(maybeFile) =>
      (this, Commands.loadFile(maybeFile))
    case UpdateImage(maybeImage) =>
      (this.copy(image = maybeImage), Logger.consoleLog[IO]("I have the image " + maybeImage))
    case UpdateTags(v)      => (this.copy(tags = Some(v)), Cmd.None)
    case AttemptPostBook =>
      (
        this,
        Commands.postBook(
          name,
          author,
          description,
          publisher,
          inHallOnly,
          year,      
          tags,
          image
        )
      )
    case PostBookError(error) => (setErrorStatus(error), Cmd.None)
    case PostBookSuccess(bookId) =>
      (setSuccessStatus("Success!"), Logger.consoleLog[IO](s"Added book with id $bookId"))
    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Name", "name", "text", true, UpdateName(_)),
    renderInput("Author", "author", "text", true, UpdateAuthor(_)),
    renderTextArea("Description", "description", true, UpdateDescription(_)),
    renderInput("Publisher", "publisher", "text", true, UpdatePublisher(_)),
    renderInput("In Hall Only", "inHallOnly", "checkbox", true, _ => ToggleHallOnly),
    renderInput(
      "year",
      "year",
      "number",
      false,
      s => UpdateYear(parseNumber(s))
    ),
    renderImageUploadInput("Logo", "image", image, UpdateImageFile(_)),
    renderInput("Tags", "tags", "text", false, UpdateTags(_)),
    button(`type` := "button", onClick(AttemptPostBook))("Post Job")
  )
  private def renderInvalidPage =
    div(
      h1("Post Job"),
      div("You need to be logged in to post a job.")
    )
  private def parseNumber(s: String) =
    Try(s.toInt).getOrElse(0)
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}
object PostBookPage {
  trait Msg                                           extends App.Msg
  case class UpdateName(name: String)           extends Msg
  case class UpdateAuthor(author: String)               extends Msg
  case class UpdateDescription(description: String)   extends Msg
  case class UpdatePublisher(publisher: String)   extends Msg
  case object ToggleHallOnly                            extends Msg
  case class UpdateYear(year: Int)            extends Msg
  case class UpdateImageFile(maybeFile: Option[File]) extends Msg
  case class UpdateImage(maybeImage: Option[String])  extends Msg
  case class UpdateTags(tags: String)                 extends Msg
  case object AttemptPostBook                         extends Msg
  case class PostBookError(error: String)              extends Msg
  case class PostBookSuccess(jobId: String)            extends Msg
  object Endpoints {
    val postBook = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.postBook
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => PostBookError(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(s, _) if s >= 200 && s < 300 =>
            val jobId = response.body
            PostBookSuccess(jobId)
          case Status(401, _) =>
            PostBookError("You are not authorized to post a job")
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match {
              case Left(e)  => PostBookError(s"Error: $e")
              case Right(e) => PostBookError(e)
            }
          case _ =>
            PostBookError("Unkown reply from server.")
        }
    }
  }
  object Commands {
    def postBook(
        name: String,
        author: String,
        description: String,
        publisher: String,
        inHallOnly: Boolean,
        year: Int,
        tags: Option[String],
        image: Option[String],
    ) =
      Endpoints.postBook.callAuthorized(
        BookInfo(
          name
          ,author
          ,description
          ,publisher
          ,year
          ,inHallOnly
          ,tags.map(text => text.split(",").map(_.trim).toList)
          ,image
        )
      )
    def loadFile(maybeFile: Option[File]) =
      Cmd.Run[IO, Option[String], Msg](
        maybeFile.traverse { file =>
          IO.async_ { cb =>
            val reader = new FileReader
            reader.onload = _ => cb(Right(reader.result.toString))
            reader.readAsDataURL(file)
          }
        }
      )(UpdateImage(_))
  }
}
