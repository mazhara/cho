package toloka.cho.books.pages

import tyrian.Html
import tyrian.http.*
import tyrian.Cmd
import tyrian.Html.*
import tyrian.cmds.Logger

import cats.effect.IO
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*

import toloka.cho.books.*
import toloka.cho.books.componenets.FilterPanel
import com.toloka.cho.domain.book.Book
import toloka.cho.books.common.Constants
import toloka.cho.books.App
import com.toloka.cho.domain.book.BookFilter
import toloka.cho.books.common.Endpoint

final case class BooksListPage (
    filterPanel: FilterPanel = FilterPanel(),
    books: List[Book] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status("Loading", Page.StatusKind.LOADING))
) extends Page {

  import toloka.cho.books.pages.BookListPage.*

  override def initCmd: Cmd[IO, App.Msg] =
    filterPanel.initCmd |+| Commands.getBooks()
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case AddBooks(list, clm) =>
      (
        setSuccessStatus("Loaded").copy(books = this.books ++ list, canLoadMore = clm),
        Cmd.None
      )
    case SetErrorStatus(e) => (setErrorStatus(e), Cmd.None)
    case LoadMoreBooks      => (this, Commands.getBooks(skip = books.length))
    case msg: FilterPanel.Msg =>
      val (newFilterPanel, cmd) = filterPanel.update(msg)
      (this.copy(filterPanel = newFilterPanel), cmd)
    case _ => (this, Cmd.None)
  }

  override def view(): Html[App.Msg] =
  div(`class` := "book-list-page")(
    filterPanel.view(),
    div(`class` := "book-container")(
      books.map(renderBook) ++ maybeRenderLoadMore
    )
  )

  private def renderBook(book: Book) =
    div(`class` := "book-card")(
      div(`class` := "book-card-img")(
        img(
          `class` := "book-logo",
          src     := book.bookInfo.image.getOrElse(""),
          alt     := book.bookInfo.name
        )
      ),
      div(`class` := "book-card-content")(
        h4(s"${book.bookInfo.description} - ${book.bookInfo.name}")
      ),
      div(`class` := "jbook-card-apply")(
        a(href := book.bookInfo.author, target := "blank")("Apply")
      )
    )


  private def maybeRenderLoadMore: Option[Html[App.Msg]] = status.map { s =>
    div(`class` := "load-more-action")(
      s match {
        case Page.Status(_, Page.StatusKind.LOADING) => div("Loading...")
        case Page.Status(e, Page.StatusKind.ERROR)   => div(e)
        case Page.Status(_, Page.StatusKind.SUCCESS) =>
          if (canLoadMore)
            button(`type` := "button", onClick(LoadMoreBooks))("Load more")
          else
            div("All books loaded")
      }
    )
  }

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
  private def setSuccessStatus(message: String) =
      this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

}
object BookListPage {
  trait Msg                                                 extends App.Msg
  case class SetErrorStatus(e: String)                      extends Msg
  case class AddBooks(list: List[Book], canLoadMore: Boolean) extends Msg
  case object LoadMoreBooks   extends Msg

  object Endpoints {
    def getBooks(limit: Int = Constants.defaultPageSize, skip: Int = 0) = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.books + s"?limit=$limit&skip=$skip"
      override val method: Method   = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[List[Book], Msg](
          list => AddBooks(list, canLoadMore = skip == 0 || !list.isEmpty),
          SetErrorStatus(_)
        )
    }
  }

  object Commands {
    def getBooks(
        filter: BookFilter = BookFilter(),
        limit: Int = Constants.defaultPageSize,
        skip: Int = 0
    ): Cmd[IO, Msg] =
      Endpoints.getBooks(limit, skip).call(filter)
  }
}
