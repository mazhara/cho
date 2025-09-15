package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.book.{Book, BookCopy, BookFilter, BookInfo}
import io.circe.generic.auto._
import toloka.cho.books._
import toloka.cho.books.common.Language.Language
import toloka.cho.books.common.{BooksListPageTranslations, Constants, Endpoint}
import toloka.cho.books.components.SubHeader
import tyrian.Html._
import tyrian.http._
import tyrian.{Cmd, Html}

import java.util.UUID

final case class BooksListPage(
    lang: Language,
    bookFilter: BookFilter = BookFilter(),
    books: List[Book] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page:

  import toloka.cho.books.pages.BookListPage._

  private implicit val language: Language = lang

  override def subHeader: Option[Html[App.Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem(BooksListPageTranslations.get("books.subheader.new"), ""),
        SubHeader.MenuItem(BooksListPageTranslations.get("books.subheader.author"), ""),
        SubHeader.MenuItem(BooksListPageTranslations.get("books.subheader.name"), "")
      ),
      activeItem = BooksListPageTranslations.get("books.subheader.new")
    )
  )

  override def initCmd: Cmd[IO, App.Msg] = Commands.getBooks()

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AddBooks(list, clm) =>
      (
        setSuccessStatus("Loaded").copy(books = this.books ++ list, canLoadMore = clm),
        Cmd.None
      )
    case SetErrorStatus(e) => (setErrorStatus(e), Cmd.None)
    case LoadMoreBooks     => (this, Commands.getBooks(skip = books.length))
    case _                 => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")(BooksListPageTranslations.get("books.title")),
      hr(cls := "title-hr"),
      div(cls := "sorting-options")(
        span(BooksListPageTranslations.get("books.sort")),
        a(href := "#", cls := "sort-option active")(BooksListPageTranslations.get("books.subheader.new")),
        a(href := "#", cls := "sort-option")(BooksListPageTranslations.get("books.subheader.author")),
        a(href := "#", cls := "sort-option")(BooksListPageTranslations.get("books.subheader.name"))
      ),
      div(cls := "book-grid")(books.map(bookCardView)),
      div(cls := "load-more-container")(
        button(
          `type` := "button",
          cls := "load-more-button",
          onClick(LoadMoreBooks)
        )(BooksListPageTranslations.get("books.load.more"))
      )
    )

  private def bookCardView(book: Book): Html[App.Msg] =
    div(cls := "book-card")(
      div(cls := "book-cover-container")(
        book.bookInfo.image
          .map {
            base64Img =>
              // If your images are PNGs
              val dataUri = s"data:image/png;base64,$base64Img"

              img(
                src := dataUri,
                alt := book.bookInfo.title,
                cls := "book-cover"
              )
          }
          .getOrElse(
            div(cls := "no-cover")("No Cover")
          )
      ),
      div(cls := "book-details")(
        p(cls := "book-title")(book.bookInfo.title),
        p(cls := "book-author")(
          book.bookInfo.authors.map(_.values.mkString(", ")).getOrElse("Unknown Author")
        ),
        book.bookInfo.copies.exists(_.exists(!_.available)) match {
          case true  => span(cls := "book-taken-label")(BooksListPageTranslations.get("books.taken"))
          case false => span() // Empty span if not taken
        }
      )
    )

  private def loadMoreButtonView: Option[Html[App.Msg]] = status.map { s =>
    div(`class` := "load-more-container")(
      s match
        case Page.Status(_, Page.StatusKind.LOADING) =>
          div(`class` := "page-status-loading")("Loading...") // fixme class
        case Page.Status(e, Page.StatusKind.ERROR) => div(`class` := "page-status-errors")(e)
        case Page.Status(_, Page.StatusKind.SUCCESS) =>
          if (canLoadMore)
            button(
              `type` := "button",
              `class` := "load-more-button",
              onClick(LoadMoreBooks)
            )(
              BooksListPageTranslations.get("books.load.more")
            )
          else
            div(BooksListPageTranslations.get("books.all.loaded"))
    )
  }

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

object BookListPage:
  trait Msg                                                         extends App.Msg
  case class SetErrorStatus(e: String)                              extends Msg
  case class AddBooks(list: List[Book], canLoadMore: Boolean)       extends Msg
  case object LoadMoreBooks                                         extends Msg
  case class FilterBooks(selectedFilters: Map[String, Set[String]]) extends Msg

  object Endpoints:
    def getBooks(limit: Int = Constants.defaultPageSize, skip: Int = 0) = new Endpoint[Msg]:
      override val location: String = Constants.endpoints.books + s"?limit=$limit&skip=$skip"
      override val method: Method   = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[List[Book], Msg](
          list => AddBooks(list, canLoadMore = skip == 0 || !list.isEmpty),
          SetErrorStatus(_)
        )

  object Commands:
    def getBooks(
        filter: BookFilter = BookFilter(),
        limit: Int = Constants.defaultPageSize,
        skip: Int = 0
    ): Cmd[IO, Msg] =
      Endpoints.getBooks(limit, skip).call(filter)
