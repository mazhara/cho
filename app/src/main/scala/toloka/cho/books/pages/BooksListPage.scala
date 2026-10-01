package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.book.{Book, BookCopy, BookFilter, BookInfo, BookSort}
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
    sort: BookSort = BookSort.New,
    section: BookListPage.BooksSection = BookListPage.BooksSection.All,
    books: List[Book] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page:

  import toloka.cho.books.pages.BookListPage._

  private implicit val language: Language = lang

  private def sectionLabelKey(section: BooksSection): String = section match
    case BooksSection.All      => "books.subheader.all"
    case BooksSection.Category => "books.subheader.category"

  override def subHeader: Option[Html[App.Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem(
          BooksListPageTranslations.get("books.subheader.all"),
          "",
          Some(SelectSection(BooksSection.All))
        ),
        SubHeader.MenuItem(
          BooksListPageTranslations.get("books.subheader.category"),
          "",
          Some(SelectSection(BooksSection.Category))
        )
      ),
      activeItem = BooksListPageTranslations.get(sectionLabelKey(section))
    )
  )

  override def initCmd: Cmd[IO, App.Msg] = Commands.getBooks(bookFilter, sort = sort)

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AddBooks(list, clm, requestedSort, requestedSearch)
        if requestedSort == sort && requestedSearch == bookFilter.search =>
      (
        setSuccessStatus("Loaded").copy(books = this.books ++ list, canLoadMore = clm),
        Cmd.None
      )
    case AddBooks(_, _, _, _) => (this, Cmd.None)
    case ChangeSort(nextSort) if nextSort != sort =>
      val nextPage = copy(
        sort = nextSort,
        books = List.empty,
        canLoadMore = true,
        status = Some(Page.Status.LOADING)
      )
      (nextPage, Commands.getBooks(bookFilter, sort = nextSort))
    case SelectSection(BooksSection.All) if bookFilter.search.nonEmpty =>
      val nextFilter = bookFilter.copy(search = None)
      val nextPage = copy(
        section = BooksSection.All,
        bookFilter = nextFilter,
        books = List.empty,
        canLoadMore = true,
        status = Some(Page.Status.LOADING)
      )
      (nextPage, Commands.getBooks(nextFilter, sort = sort))
    case SelectSection(nextSection) => (copy(section = nextSection), Cmd.None)
    case SetErrorStatus(e) => (setErrorStatus(e), Cmd.None)
    case LoadMoreBooks     => (this, Commands.getBooks(bookFilter, skip = books.length, sort = sort))
    case _                 => (this, Cmd.None)

  override def submitHeaderSearch(query: String): (Page, Cmd[IO, App.Msg]) =
    val nextFilter = bookFilter.copy(search = Option(query.trim).filter(_.nonEmpty))
    val nextPage = copy(
      bookFilter = nextFilter,
      books = List.empty,
      canLoadMore = true,
      status = Some(Page.Status.LOADING)
    )
    (nextPage, Commands.getBooks(nextFilter, sort = sort))

  override def view(): Html[App.Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")(BooksListPageTranslations.get("books.title")),
      hr(cls := "title-hr"),
      div(cls := "sorting-options")(
        span(BooksListPageTranslations.get("books.sort")),
        a(
          href := "#",
          cls := (if (sort == BookSort.New) "sort-option active" else "sort-option"),
          onClick(ChangeSort(BookSort.New))
        )(BooksListPageTranslations.get("books.subheader.new")),
        a(
          href := "#",
          cls := (if (sort == BookSort.Author) "sort-option active" else "sort-option"),
          onClick(ChangeSort(BookSort.Author))
        )(BooksListPageTranslations.get("books.subheader.author")),
        a(
          href := "#",
          cls := (if (sort == BookSort.Name) "sort-option active" else "sort-option"),
          onClick(ChangeSort(BookSort.Name))
        )(BooksListPageTranslations.get("books.subheader.name"))
      ),
      div(cls := "book-grid")(
        books.map(bookCardView) ++
          (if (books.isEmpty && bookFilter.search.nonEmpty && status.exists {
             case Page.Status(_, Page.StatusKind.SUCCESS) => true
             case _                                        => false
           })
             List(div(cls := "search-no-results")(BooksListPageTranslations.get("books.search.noResults")))
           else List.empty)
      ),
      loadMoreButtonView.getOrElse(div())
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
  enum BooksSection:
    case All, Category

  case class SetErrorStatus(e: String)                              extends Msg
  case class AddBooks(
      list: List[Book],
      canLoadMore: Boolean,
      sort: BookSort,
      search: Option[String]
  ) extends Msg
  case object LoadMoreBooks                                         extends Msg
  case class ChangeSort(sort: BookSort)                             extends Msg
  case class SelectSection(section: BooksSection)                   extends Msg
  case class FilterBooks(selectedFilters: Map[String, Set[String]]) extends Msg

  object Endpoints:
    def getBooks(
        limit: Int = Constants.defaultPageSize,
        skip: Int = 0,
        sort: BookSort = BookSort.New,
        search: Option[String] = None
    ) = new Endpoint[Msg]:
      override val location: String =
        Constants.endpoints.books + s"?limit=$limit&skip=$skip&sort=${sort.queryValue}"
      override val method: Method   = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[List[Book], Msg](
          list =>
            println(s" Books size $skip, books ${list.map(b => s"${b.bookInfo.title} by ${b.bookInfo.authors}")}")
            AddBooks(list, list.size == limit, sort, search),
          SetErrorStatus(_)
        )

  object Commands:
    def getBooks(
        filter: BookFilter = BookFilter(),
        limit: Int = Constants.defaultPageSize,
        skip: Int = 0,
        sort: BookSort = BookSort.New
    ): Cmd[IO, Msg] =
      Endpoints.getBooks(limit, skip, sort, filter.search).call(filter)
