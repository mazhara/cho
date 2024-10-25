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
import com.toloka.cho.domain.AuthorInfo
import com.toloka.cho.domain.AuthorType


case class PostBookPage(
    title: String = "",
    author: String = "",
    description: String = "",
    publisher: String = "",
    inHallOnly: Boolean = false,
    year: Int = 0,
    tags: Option[String] = None,
    image: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Add Book", status) {
  import PostBookPage.*

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateTitle(v)     => (this.copy(title = v), Cmd.None)
    case UpdateAuthor(v)       => (this.copy(author = v), Cmd.None)
    case UpdateDescription(v) => (this.copy(description = v), Cmd.None)
    case UpdatePublisher(v) => (this.copy(publisher = v), Cmd.None)
    case ToggleHallOnly        => (this.copy(inHallOnly = !this.inHallOnly), Cmd.None)
    case UpdateYear(v)    => (this.copy(year = v), Cmd.None)
    case UpdateImageFile(maybeFile) =>
      (this, Commands.loadFile(maybeFile))
    case UpdateImage(maybeImage) =>
      (this.copy(image = maybeImage), Cmd.None)
    case UpdateTags(v)      => (this.copy(tags = Some(v)), Cmd.None)
    case AttemptPostBook =>
      (
        this,
        Commands.postBook(
          title,
          author,
          description,
          publisher,
          inHallOnly,
          year,      
          tags,
          image
        )
      )
    case FindAuthor => (this, Commands.findAuthor(this.author))
    case PostBookError(error) => (setErrorStatus(error), Cmd.None)
    case PostBookSuccess(bookId) =>
      (setSuccessStatus("Success!"), Logger.consoleLog[IO](s"Added book with id $bookId"))
    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] =
    if (!Session.isActive) renderInvalidContents()
    else
      List(
      renderInput("Title", "title", "text", true, UpdateTitle(_)),
      renderInput("Author", "author", "text", true, UpdateAuthor(_)),
      button(`type` := "button", onClick(FindAuthor))("Find author"),
      renderTextArea("Description", "description", true, UpdateDescription(_)),
      renderInput("Publisher", "publisher", "text", true, UpdatePublisher(_)),
      button(`type` := "button", onClick(AttemptPostBook))("Find publisher"),
      renderToggle("In hall Only", "inHallOnly", true, _ => ToggleHallOnly),
      renderInput("year", "year", "number", false, s => UpdateYear(parseNumber(s))),
      renderImageUploadInput("Logo", "image", image, UpdateImageFile(_)),
      renderInput("Tags", "tags", "text", false, UpdateTags(_)),
      button(`type` := "button", onClick(AttemptPostBook))("Add Book")
    )

  private def renderInvalidContents() = List(
    p(`class` := "form-text")("You need to be logged in to add a book.")
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
  case class UpdateTitle(name: String)           extends Msg
  case class UpdateAuthor(author: String)               extends Msg
  case class UpdateDescription(description: String)   extends Msg
  case class UpdatePublisher(publisher: String)   extends Msg
  case object ToggleHallOnly                            extends Msg
  case class UpdateYear(year: Int)            extends Msg
  case class UpdateImageFile(maybeFile: Option[File]) extends Msg
  case class UpdateImage(maybeImage: Option[String])  extends Msg
  case class UpdateTags(tags: String)                 extends Msg
  case object AttemptPostBook                         extends Msg
  case object FindAuthor                              extends Msg
  case object FindPublisher                           extends Msg
  case class PostBookError(error: String)              extends Msg
  case class PostBookSuccess(jobId: String)            extends Msg

  object Endpoints {
    val postBook = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.postBook
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => PostBookError(e.toString)
      override val onResponse: Response => Msg = Endpoint.onResponseText(PostBookSuccess(_), PostBookError(_))
    }

    def findAuthor(pattern: String) = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.authors + s"?query=$pattern"
      override val method: Method            = Method.Get
      override val onError: HttpError => Msg = e => PostBookError(e.toString)
      override val onResponse: Response => Msg = Endpoint.onResponseText(PostBookSuccess(_), PostBookError(_))
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
          isbn = None
          ,title = name
          ,description = Some(description)
          , authors = None
          ,publisherId = None
          , publisherName = Some(publisher)
          , genre = None
          ,publishedYear = Some(year)
          ,tags.map(text => text.split(",").map(_.trim).toList)
          ,image
          , None
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

    def findAuthor(author: String) = {
      Endpoints.findAuthor(author).callAuthorized(AuthorInfo(None, author, AuthorType.Author))
  }
  }
}
