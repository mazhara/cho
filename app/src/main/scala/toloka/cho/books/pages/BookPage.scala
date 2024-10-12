package toloka.cho.books.pages

import tyrian.Cmd
import tyrian.Html
import tyrian.Html.*
import tyrian.http.*

import cats.effect.IO

import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*
import cats.effect.IO
import io.circe.generic.auto.*

import laika.api.*
import laika.format.*

import toloka.cho.books.*
import com.toloka.cho.domain.book.Book
import toloka.cho.books.common.Endpoint
import toloka.cho.books.common.Constants
import toloka.cho.books.pages.Page.StatusKind
import toloka.cho.books.components.BookComponents


@js.native
@JSGlobal()
class Moment extends js.Object {
  def fromNow(): String = js.native
}
@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object {
  def unix(date: Long): Moment = js.native
}

final case class BookPage(
  id: String = "",
  maybeBook: Option[Book] = None,
  status: Page.Status = Page.Status.LOADING
) extends Page {

  import BookPage.*

  override def initCmd: Cmd[IO, App.Msg] = Commands.getBook(id)  
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case SetError(e) => (setErrorStatus(e), Cmd.None)
    case SetBook(book) => (setSuccessStatus("Success").copy(maybeBook = Some(book)), Cmd.None)
    case _           => (this, Cmd.None)
  }

  private def renderBookPage(book: Book) =
    div(`class` := "container-fluid the-rock")(
      div(`class` := "row jvm-books-details-top-card")(
        div(`class` := "col-md-12 p-0")(
          div(`class` := "jvm-books-details-card-profile-img")(
            img(
              `class` := "img-fluid",
              src     := book.bookInfo.image.getOrElse(Constants.bookImagePlaceholder),
              alt     := book.bookInfo.title
            )
          ),
          div(`class` := "jvm-books-details-card-profile-title")(
            h1(s"${book.bookInfo.title} - ${book.bookInfo.authors}"),
            div(`class` := "jvm-books-details-card-profile-book-details-company-and-location")(
              BookComponents.renderBookSummary(book)
            )
          ),
        )
      ),
      div(`class` := "container-fluid")(
        div(`class` := "container")(
          div(`class` := "markdown-body overview-section")(
            renderBookDescription(book)
          )
        )
      )
    )


  private def renderNoBookPage() =
    div(`class` := "container-fluid the-rock")(
      div(`class` := "row jvm-jobs-details-top-card")(status.kind.match {
        case StatusKind.LOADING => h1("Loading...")
        case StatusKind.ERROR   => h1("This job does not exist !")
        case StatusKind.SUCCESS => h1("No job !")
      })
    )

  private def renderBookDescription(book: Book) = {
    val descriptionHtml = markdownTransformer.transform(book.bookInfo.description.getOrElse("")) match {
      case Left(e) =>
        """
          Had an error showing Markdown for this book description !
          Just hit the apply button.
        """
      case Right(html) => html
    }
    div(`class` := "book-description")().innerHtml(descriptionHtml)
  }

  val markdownTransformer = Transformer
    .from(Markdown)
    .to(HTML)
    .build

  override def view(): Html[App.Msg] = maybeBook match {
      case Some(book) => renderBookPage(book)
      case None      => renderNoBookPage()
    }

  private def setErrorStatus(message: String) =
    this.copy(status = Page.Status(message, Page.StatusKind.ERROR))
  private def setSuccessStatus(message: String) =
    this.copy(status = Page.Status(message, Page.StatusKind.SUCCESS))
}

object BookPage {
  trait Msg                          extends App.Msg
  case class SetError(error: String) extends Msg
  case class SetBook(book: Book)        extends Msg
  object Endpoints {
    def getBook(id: String) = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.books + s"/$id"
      override val method: Method            = Method.Get
      override val onError: HttpError => Msg = e => SetError(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[Book, Msg](SetBook(_), SetError(_))
    }
  }
  object Commands {
    def getBook(id: String) =
      Endpoints.getBook(id).call()
  }
}
