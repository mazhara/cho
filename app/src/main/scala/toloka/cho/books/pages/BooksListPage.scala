// package toloka.cho.books.pages

// import tyrian.Html
// import tyrian.http.*
// import tyrian.Cmd
// import tyrian.Html.*
// import tyrian.cmds.Logger

// import cats.effect.IO
// import io.circe.syntax.*
// import io.circe.parser.*
// import io.circe.generic.auto.*

// import toloka.cho.books.*
// import toloka.cho.books.components.FilterPanel
// import com.toloka.cho.domain.book.Book
// import toloka.cho.books.common.Constants
// import toloka.cho.books.App
// import com.toloka.cho.domain.book.BookFilter
// import toloka.cho.books.common.Endpoint
// import toloka.cho.books.pages.BookListPage.FilterBooks
// import toloka.cho.books.components.BookComponents

// final case class BooksListPage (
//     filterPanel: FilterPanel = FilterPanel(
//       filterAction = FilterBooks(_)
//     ),
//     jobFilter: BookFilter = BookFilter(),
//     books: List[Book] = List(),
//     canLoadMore: Boolean = true,
//     status: Option[Page.Status] = Some(Page.Status.LOADING)
// ) extends Page {

//   import toloka.cho.books.pages.BookListPage.*

//   override def initCmd: Cmd[IO, App.Msg] =
//     filterPanel.initCmd |+| Commands.getBooks()
//   override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
//     case AddBooks(list, clm) =>
//       (
//         setSuccessStatus("Loaded").copy(books = this.books ++ list, canLoadMore = clm),
//         Cmd.None
//       )
//     case SetErrorStatus(e) => (setErrorStatus(e), Cmd.None)
//     case LoadMoreBooks      => (this, Commands.getBooks(skip = books.length))
//     case FilterBooks(selectedFilters) =>
//       val newBookFilter = createBookFilter(selectedFilters)
//       (this.copy(books = List(), jobFilter = newBookFilter), Commands.getBooks(filter = newBookFilter))
//     case msg: FilterPanel.Msg =>
//       val (newFilterPanel, cmd) = filterPanel.update(msg)
//       (this.copy(filterPanel = newFilterPanel), cmd)
//     case _ => (this, Cmd.None)
//   }

//   override def view(): Html[App.Msg] =
//     section(`class` := "section-1")(
//         div(`class` := "container book-list-hero")(
//         h1(`class` := "book-list-title")("Books by Toloka")
//       ),
//       div(`class` := "container")(
//         div(`class` := "row jvm-recent-books-body")(
//           div(`class` := "col-lg-4")(
//             filterPanel.view()
//           ),
//           div(`class` := "col-lg-8")(
//             books.map(renderBook) ++ maybeRenderLoadMore
//           )
//         )
//     )
//   )

//   private def renderBook(book: Book) =
//    BookComponents.card(book)


//   private def renderBookSummary(book: Book): Html[App.Msg] =
//     div(
//       BookComponents.renderDetail("location-dot", book.bookInfo.publisher) //fixme
//     )

//   private def createBookFilter(selectedFilters: Map[String, Set[String]]) =
//     BookFilter(
//       authors = selectedFilters.get("Authors").getOrElse(Set()).toList,
//       publishers = selectedFilters.get("Publishers").getOrElse(Set()).toList,
//       tags = selectedFilters.get("Tags").getOrElse(Set()).toList,
//       publishedYear = Some(filterPanel.year),
//       filterPanel.inHallOnly
//     )   

//   private def maybeRenderLoadMore: Option[Html[App.Msg]] = status.map { s =>
//     div(`class` := "load-more-action")(
//       s match {
//         case Page.Status(_, Page.StatusKind.LOADING) => div(`class` := "page-status-loading")("Loading...")
//         case Page.Status(e, Page.StatusKind.ERROR)   => div(`class` := "page-status-errors")(e)
//         case Page.Status(_, Page.StatusKind.SUCCESS) =>
//           if (canLoadMore)
//             button(`type` := "button", `class` := "load-more-btn", onClick(LoadMoreBooks))(
//               "Load more"
//             )
//           else
//             div("All books loaded")
//       }
//     )
//   }

//   private def setErrorStatus(message: String) =
//     this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))
//   private def setSuccessStatus(message: String) =
//       this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

// }
// object BookListPage {
//   trait Msg                                                 extends App.Msg
//   case class SetErrorStatus(e: String)                      extends Msg
//   case class AddBooks(list: List[Book], canLoadMore: Boolean) extends Msg
//   case object LoadMoreBooks   extends Msg
//   case class FilterBooks(selectedFilters: Map[String, Set[String]]) extends Msg

//   object Endpoints {
//     def getBooks(limit: Int = Constants.defaultPageSize, skip: Int = 0) = new Endpoint[Msg] {
//       override val location: String = Constants.endpoints.books + s"?limit=$limit&skip=$skip"
//       override val method: Method   = Method.Post
//       override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
//       override val onResponse: Response => Msg =
//         Endpoint.onResponse[List[Book], Msg](
//           list => AddBooks(list, canLoadMore = skip == 0 || !list.isEmpty),
//           SetErrorStatus(_)
//         )
//     }
//   }

//   object Commands {
//     def getBooks(
//         filter: BookFilter = BookFilter(),
//         limit: Int = Constants.defaultPageSize,
//         skip: Int = 0
//     ): Cmd[IO, Msg] =
//       Endpoints.getBooks(limit, skip).call(filter)
//   }
// }
