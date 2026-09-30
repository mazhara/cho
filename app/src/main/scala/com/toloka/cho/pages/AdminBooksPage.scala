package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.domain.{Author, AuthorInfo, AuthorType, Publisher, PublisherInfo}
import com.toloka.cho.domain.book.{Book, BookInfo}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.scalajs.dom
import com.toloka.cho.App
import com.toloka.cho.common.{AdminPageTranslations, Constants}
import com.toloka.cho.common.Language.Language
import scala.scalajs.js
import tyrian.Attribute
import tyrian.Cmd
import tyrian.Html
import tyrian.Html.*
import tyrian.http.{Header, Method}

import java.util.UUID

final case class AdminBooksPage(
    lang: Language,
    books: List[Book] = Nil,
    publishers: List[Publisher] = Nil,
    search: String = "",
    canLoadMore: Boolean = true,
    loading: Boolean = true,
    error: Option[String] = None,
    addOpen: Boolean = false,
    title: String = "",
    isbn: String = "",
    description: String = "",
    genre: String = "",
    publishedYear: String = "",
    cover: String = "",
    publisherId: Option[Int] = None,
    authorQuery: String = "",
    authorResults: List[Author] = Nil,
    selectedAuthors: Map[String, String] = Map.empty,
    authorFirstName: String = "",
    authorLastName: String = "",
    authorDialogOpen: Boolean = false,
    publisherDialogOpen: Boolean = false,
    publisherName: String = "",
    publisherCountry: String = "",
    selectedBook: Option[Book] = None,
    deleteBook: Option[Book] = None,
    saving: Boolean = false
) extends Page:
  private implicit val language: Language = lang

  override def isStandalone: Boolean = true
  override def initCmd: Cmd[IO, App.Msg] = AdminBooksPage.loadBooks("", 0) |+| AdminBooksPage.loadPublishers()

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AdminBooksPage.BooksLoaded(result, skip, limit) =>
      (copy(
        books = if skip == 0 then result else books ++ result,
        canLoadMore = result.size == limit,
        loading = false,
        error = None
      ), Cmd.None)
    case AdminBooksPage.PublishersLoaded(result) =>
      (copy(publishers = result, error = None), Cmd.None)
    case AdminBooksPage.AuthorsLoaded(result) =>
      (copy(authorResults = result, error = None), Cmd.None)
    case AdminBooksPage.Failed(message) =>
      (copy(loading = false, saving = false, error = Some(message)), Cmd.None)
    case AdminBooksPage.SearchChanged(value) => (copy(search = value.take(100)), Cmd.None)
    case AdminBooksPage.SearchBooks =>
      (copy(loading = true, canLoadMore = true, error = None), AdminBooksPage.loadBooks(search.trim, 0))
    case AdminBooksPage.LoadMoreBooks if !loading && canLoadMore =>
      (copy(loading = true), AdminBooksPage.loadBooks(search.trim, books.length))
    case AdminBooksPage.OpenAdd => (copy(addOpen = true, error = None), Cmd.None)
    case AdminBooksPage.CloseAdd =>
      (copy(addOpen = false, error = None), Cmd.None)
    case AdminBooksPage.CloseAuthorDialog =>
      (copy(authorDialogOpen = false, error = None), Cmd.None)
    case AdminBooksPage.ClosePublisherDialog =>
      (copy(publisherDialogOpen = false, error = None), Cmd.None)
    case AdminBooksPage.TitleChanged(value) => (copy(title = value, error = None), Cmd.None)
    case AdminBooksPage.IsbnChanged(value) => (copy(isbn = value, error = None), Cmd.None)
    case AdminBooksPage.DescriptionChanged(value) => (copy(description = value, error = None), Cmd.None)
    case AdminBooksPage.GenreChanged(value) => (copy(genre = value, error = None), Cmd.None)
    case AdminBooksPage.YearChanged(value) => (copy(publishedYear = value.filter(_.isDigit).take(4), error = None), Cmd.None)
    case AdminBooksPage.CoverChanged(value) => (copy(cover = value, error = None), Cmd.None)
    case AdminBooksPage.PublisherChanged(value) =>
      (copy(publisherId = value.toIntOption, error = None), Cmd.None)
    case AdminBooksPage.AuthorQueryChanged(value) => (copy(authorQuery = value.take(100), error = None), Cmd.None)
    case AdminBooksPage.SearchAuthors =>
      val query = authorQuery.trim
      if query.length < 2 then (copy(error = Some("Enter at least two characters to search authors.")), Cmd.None)
      else (copy(error = None), AdminBooksPage.searchAuthors(query))
    case AdminBooksPage.AddAuthor(author) =>
      val id = author.id.toString
      val name = List(author.authorInfo.firstName.getOrElse(""), author.authorInfo.lastName).mkString(" ").trim
      (copy(selectedAuthors = selectedAuthors.updated(id, name)), Cmd.None)
    case AdminBooksPage.RemoveAuthor(id) => (copy(selectedAuthors = selectedAuthors - id), Cmd.None)
    case AdminBooksPage.OpenAuthorDialog => (copy(authorDialogOpen = true), Cmd.None)
    case AdminBooksPage.AuthorFirstNameChanged(value) => (copy(authorFirstName = value), Cmd.None)
    case AdminBooksPage.AuthorLastNameChanged(value) => (copy(authorLastName = value), Cmd.None)
    case AdminBooksPage.CreateAuthor =>
      if authorLastName.trim.isEmpty then (copy(error = Some("Author last name is required.")), Cmd.None)
      else
        AuthSession.authorization match
          case Some(auth) =>
            val info = AuthorInfo(Option(authorFirstName.trim).filter(_.nonEmpty), authorLastName.trim, AuthorType.Writer)
            (copy(saving = true, error = None), AdminBooksPage.createAuthor(info, auth))
          case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case AdminBooksPage.AuthorCreated(_) =>
      (copy(saving = false, authorDialogOpen = false, authorFirstName = "", authorLastName = ""), AdminBooksPage.searchAuthors(authorQuery.trim))
    case AdminBooksPage.OpenPublisherDialog => (copy(publisherDialogOpen = true), Cmd.None)
    case AdminBooksPage.PublisherNameChanged(value) => (copy(publisherName = value), Cmd.None)
    case AdminBooksPage.PublisherCountryChanged(value) => (copy(publisherCountry = value), Cmd.None)
    case AdminBooksPage.CreatePublisher =>
      if publisherName.trim.isEmpty then (copy(error = Some("Publisher name is required.")), Cmd.None)
      else
        AuthSession.authorization match
          case Some(auth) =>
            val info = PublisherInfo(publisherName.trim, Option(publisherCountry.trim).filter(_.nonEmpty), None, None, None)
            (copy(saving = true, error = None), AdminBooksPage.createPublisher(info, auth))
          case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case AdminBooksPage.PublisherCreated(id) =>
      (copy(saving = false, publisherDialogOpen = false, publisherName = "", publisherCountry = "", publisherId = Some(id)), AdminBooksPage.loadPublishers())
    case AdminBooksPage.SubmitBook =>
      if title.trim.isEmpty then (copy(error = Some("Book title is required.")), Cmd.None)
      else if selectedAuthors.isEmpty then (copy(error = Some("Select at least one author.")), Cmd.None)
      else if publishedYear.nonEmpty && publishedYear.toIntOption.isEmpty then
        (copy(error = Some("Publication year must be a valid year.")), Cmd.None)
      else
        AuthSession.authorization match
          case Some(auth) =>
            val info = BookInfo(
              title.trim,
              Option(isbn.trim).filter(_.nonEmpty),
              Option(description.trim).filter(_.nonEmpty),
              Some(selectedAuthors),
              publisherId,
              publishers.find(_.id == publisherId.getOrElse(-1)).map(_.info.name),
              Option(genre.trim).filter(_.nonEmpty),
              publishedYear.toIntOption,
              None,
              Option(cover.trim).filter(_.nonEmpty),
              None
            )
            (copy(saving = true, error = None), AdminBooksPage.createBook(info, auth))
          case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case AdminBooksPage.BookCreated(_) =>
      (copy(
        books = Nil,
        loading = true,
        saving = false,
        addOpen = false,
        title = "",
        isbn = "",
        description = "",
        genre = "",
        publishedYear = "",
        cover = "",
        publisherId = None,
        selectedAuthors = Map.empty
      ), AdminBooksPage.loadBooks(search.trim, 0))
    case AdminBooksPage.OpenBook(book) => (copy(selectedBook = Some(book)), Cmd.None)
    case AdminBooksPage.CloseBook => (copy(selectedBook = None), Cmd.None)
    case AdminBooksPage.ConfirmDelete(book) => (copy(deleteBook = Some(book)), Cmd.None)
    case AdminBooksPage.CancelDelete => (copy(deleteBook = None), Cmd.None)
    case AdminBooksPage.DeleteConfirmed(bookId) =>
      (copy(deleteBook = None, saving = false, books = books.filterNot(_.id.toString == bookId)), Cmd.None)
    case AdminBooksPage.DeleteBook(book) =>
      AuthSession.authorization match
        case Some(auth) => (copy(saving = true, error = None), AdminBooksPage.deleteBook(book.id, auth))
        case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case AdminBooksPage.AddCopy(bookId) =>
      AuthSession.authorization match
        case Some(auth) => (this, AdminBooksPage.addCopy(bookId, auth))
        case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case AdminBooksPage.CopyAdded(_) =>
      (copy(selectedBook = None, loading = true, canLoadMore = true), AdminBooksPage.loadBooks(search.trim, 0))
    case AdminBooksPage.CopyDeleted(_) =>
      (copy(selectedBook = None, loading = true, canLoadMore = true), AdminBooksPage.loadBooks(search.trim, 0))
    case AdminBooksPage.DeleteCopy(bookId, copyId) =>
      AuthSession.authorization match
        case Some(auth) => (this, AdminBooksPage.deleteCopy(bookId, copyId, auth))
        case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "admin-dashboard")(
      AdminViewPage(lang).sidebarForBooks,
      div(cls := "admin-workspace")(
        header(cls := "admin-topbar")(
          div(cls := "admin-current-user")(
            div(cls := "admin-avatar")("NG"),
            div(strong("Nisal Gunasekara"), span(AdminPageTranslations.get("admin.role.admin")))
          ),
          div(cls := "admin-workspace-title")("Book management"),
          div(cls := "admin-topbar-actions")(
            div(cls := "admin-clock")(i(cls := "fa-regular fa-clock")(""), span(AdminViewPage.localTime)),
            a(href := "/admin", cls := "admin-settings", Attribute("title", "Dashboard"))(i(cls := "fa-solid fa-house")(""))
          )
        ),
        main(cls := "admin-books-content")(
          div(cls := "admin-books-heading")(
            div(h1("Books"), p("Manage the library catalog and physical copies.")),
            button(`type` := "button", cls := "admin-book-primary", onClick(AdminBooksPage.OpenAdd))(
              i(cls := "fa-solid fa-plus")(""), text(" Add a book")
            )
          ),
          div(cls := "admin-books-toolbar")(
            label(cls := "admin-books-search")(
              i(cls := "fa-solid fa-magnifying-glass")(""),
              input(
                `type` := "search",
                Attribute("placeholder", "Search by title or author"),
                Attribute("value", search),
                onInput(AdminBooksPage.SearchChanged(_))
              )
            ),
            button(`type` := "button", cls := "admin-book-secondary", disabled(loading), onClick(AdminBooksPage.SearchBooks))("Search"),
            span(s"${books.length} books")
          ),
          error.map(message => div(cls := "admin-books-error", Attribute("role", "alert"))(message)).getOrElse(div()),
          if loading && books.isEmpty then div(cls := "admin-books-empty")("Loading books…")
          else if books.isEmpty then div(cls := "admin-books-empty")("No books found.")
          else div(cls := "admin-books-table-wrap")(
            table(cls := "admin-books-table")(
              thead(tr(th("Book"), th("Author"), th("ISBN"), th("Copies"), th("Availability"), th("Actions"))),
              tbody(books.map(bookRow))
            )
          )
        ),
        if canLoadMore then div(cls := "admin-book-load-more")(
          button(`type` := "button", cls := "admin-book-secondary", disabled(loading), onClick(AdminBooksPage.LoadMoreBooks))(
            if loading then "Loading…" else "Load more"
          )
        ) else div(),
        if addOpen then addBookDialog else div(),
        if authorDialogOpen then authorDialog else div(),
        if publisherDialogOpen then publisherDialog else div(),
        selectedBook.map(bookDialog).getOrElse(div()),
        deleteBook.map(deleteDialog).getOrElse(div())
      )
    )

  private def bookRow(book: Book): Html[App.Msg] =
    val info = book.bookInfo
    val copies = info.copies.getOrElse(Nil)
    val available = copies.count(_.available)
    tr(
      td(
        div(cls := "admin-book-title-cell")(
          info.image.map(image => img(src := s"data:image/png;base64,$image", alt := "", cls := "admin-book-thumb")).getOrElse(div(cls := "admin-book-thumb-placeholder")(i(cls := "fa-solid fa-book")(""))),
          div(strong(info.title), small(info.publisherName.getOrElse("Publisher not set")))
        )
      ),
      td(info.authors.map(_.values.mkString(", ")).getOrElse("—")),
      td(info.isbn.getOrElse("—")),
      td(copies.size.toString),
      td(span(cls := (if available > 0 then "admin-book-availability" else "admin-book-unavailable"))(s"$available available")),
      td(cls := "admin-book-actions")(
        button(`type` := "button", Attribute("title", "View book and copies"), onClick(AdminBooksPage.OpenBook(book)))(i(cls := "fa-regular fa-eye")("")),
        button(`type` := "button", Attribute("title", "Delete book"), onClick(AdminBooksPage.ConfirmDelete(book)))(i(cls := "fa-regular fa-trash-can")(""))
      )
    )

  private def addBookDialog: Html[App.Msg] =
    val publisherOptions: List[Html[App.Msg]] =
      if publishers.isEmpty then List(span("No publishers yet"))
      else publishers.map { publisher =>
        button(
          `type` := "button",
          cls := (if publisherId.contains(publisher.id) then "admin-book-secondary admin-book-publisher-selected" else "admin-book-secondary"),
          onClick(AdminBooksPage.PublisherChanged(publisher.id.toString))
        )(publisher.info.name)
      }
    modal("Add a book", AdminBooksPage.CloseAdd)(
      div(cls := "admin-book-form-grid")(
        textField("Title", title, AdminBooksPage.TitleChanged(_), isRequired = true),
        textField("ISBN", isbn, AdminBooksPage.IsbnChanged(_)),
        textField("Genre", genre, AdminBooksPage.GenreChanged(_)),
        textField("Publication year", publishedYear, AdminBooksPage.YearChanged(_)),
        div(cls := "admin-book-field admin-book-field-wide")(
          label("Description"),
          input(`type` := "text", Attribute("value", description), onInput(AdminBooksPage.DescriptionChanged(_)))
        ),
        div(cls := "admin-book-field admin-book-field-wide")(
          label("Cover image (base64 data)"),
          input(`type` := "text", Attribute("value", cover), Attribute("placeholder", "Optional PNG/JPEG base64 data"), onInput(AdminBooksPage.CoverChanged(_)))
        ),
        div(cls := "admin-book-field admin-book-field-wide")(
          label("Publisher"),
          div(cls := "admin-book-inline-controls")(
            div(cls := "admin-book-publisher-options")(publisherOptions),
            button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.OpenPublisherDialog))("Add publisher")
          )
        ),
        div(cls := "admin-book-field admin-book-field-wide")(
          label("Authors"),
          div(cls := "admin-book-inline-controls")(
            input(`type` := "search", Attribute("placeholder", "Search authors"), Attribute("value", authorQuery), onInput(AdminBooksPage.AuthorQueryChanged(_))),
            button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.SearchAuthors))("Search"),
            button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.OpenAuthorDialog))("Add author")
          ),
          div(cls := "admin-book-author-results")(
            authorResults.map { author =>
              val id = author.id.toString
              val name = List(author.authorInfo.firstName.getOrElse(""), author.authorInfo.lastName).mkString(" ").trim
              button(`type` := "button", disabled(selectedAuthors.contains(id)), onClick(AdminBooksPage.AddAuthor(author)))(name)
            }
          ),
          div(cls := "admin-book-selected-authors")(
            selectedAuthors.toList.map { case (id, name) =>
              span(text(name), button(`type` := "button", Attribute("title", s"Remove $name"), onClick(AdminBooksPage.RemoveAuthor(id)))("×"))
            }
          )
        )
      ),
      dialogError,
      div(cls := "admin-book-dialog-actions")(
        button(`type` := "button", cls := "admin-book-secondary", disabled(saving), onClick(AdminBooksPage.CloseAdd))("Cancel"),
        button(`type` := "button", cls := "admin-book-primary", disabled(saving), onClick(AdminBooksPage.SubmitBook))(
          if saving then "Saving…" else "Save book"
        )
      )
    )

  private def bookDialog(book: Book): Html[App.Msg] =
    modal(book.bookInfo.title, AdminBooksPage.CloseBook)(
      p(s"ISBN: ${book.bookInfo.isbn.getOrElse("—")}"),
      p(s"Author(s): ${book.bookInfo.authors.map(_.values.mkString(", ")).getOrElse("—")}"),
      p(s"Publisher: ${book.bookInfo.publisherName.getOrElse("—")}"),
      p(s"Genre: ${book.bookInfo.genre.getOrElse("—")} · Year: ${book.bookInfo.publishedYear.map(_.toString).getOrElse("—")}"),
      p(book.bookInfo.description.getOrElse("No description provided.")),
      div(cls := "admin-book-copy-heading")(
        strong("Copies"),
        button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.AddCopy(book.id)))("Add copy")
      ),
      ul(cls := "admin-book-copy-list")(
        book.bookInfo.copies.getOrElse(Nil).map { copy =>
          li(
            span(s"Copy ${copy.exemplarNumber} · ${if copy.available then "Available" else "Checked out"}${if copy.inLibraryOnly then " · In-library only" else ""}"),
            button(`type` := "button", Attribute("title", "Delete copy"), onClick(AdminBooksPage.DeleteCopy(book.id, copy.copyId)))("Remove")
          )
        }
      )
    )

  private def deleteDialog(book: Book): Html[App.Msg] =
    modal("Delete book?", AdminBooksPage.CancelDelete)(
      p(s"“${book.bookInfo.title}” and its copies will be permanently removed."),
      div(cls := "admin-book-dialog-actions")(
        button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.CancelDelete))("Cancel"),
        button(`type` := "button", cls := "admin-book-danger", disabled(saving), onClick(AdminBooksPage.DeleteBook(book)))("Delete book")
      )
    )

  private def modal(titleText: String, close: App.Msg)(content: Html[App.Msg]*): Html[App.Msg] =
    div(
      cls := "admin-modal-backdrop",
      Attribute("role", "presentation"),
      onEvent("click", (event: dom.MouseEvent) => if event.target == event.currentTarget then close else App.NoOp)
    )(
      section(cls := "admin-book-dialog", Attribute("role", "dialog"), Attribute("aria-modal", "true"))(
        header(cls := "admin-book-dialog-header")(
          h2(titleText),
          button(`type` := "button", Attribute("title", "Close"), onClick(close))("×")
        ) :: content.toList
      )
    )

  private def textField(
      labelText: String,
      fieldValue: String,
      onChange: String => App.Msg,
      isRequired: Boolean = false
  ): Html[App.Msg] =
    div(cls := "admin-book-field")(
      label(labelText),
      input(`type` := "text", Attribute("value", fieldValue), required(isRequired), onInput(onChange))
    )

  private def dialogError: Html[App.Msg] =
    error.map(message => div(cls := "admin-books-error", Attribute("role", "alert"))(message)).getOrElse(div())

  private def authorDialog: Html[App.Msg] =
    modal("Add an author", AdminBooksPage.CloseAuthorDialog)(
      div(cls := "admin-book-form-grid")(
        textField("First name", authorFirstName, AdminBooksPage.AuthorFirstNameChanged(_)),
        textField("Last name", authorLastName, AdminBooksPage.AuthorLastNameChanged(_), isRequired = true)
      ),
      dialogError,
      div(cls := "admin-book-dialog-actions")(
        button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.CloseAuthorDialog))("Cancel"),
        button(`type` := "button", cls := "admin-book-primary", disabled(saving), onClick(AdminBooksPage.CreateAuthor))("Save author")
      )
    )

  private def publisherDialog: Html[App.Msg] =
    modal("Add a publisher", AdminBooksPage.ClosePublisherDialog)(
      div(cls := "admin-book-form-grid")(
        textField("Publisher name", publisherName, AdminBooksPage.PublisherNameChanged(_), isRequired = true),
        textField("Country", publisherCountry, AdminBooksPage.PublisherCountryChanged(_))
      ),
      dialogError,
      div(cls := "admin-book-dialog-actions")(
        button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.ClosePublisherDialog))("Cancel"),
        button(`type` := "button", cls := "admin-book-primary", disabled(saving), onClick(AdminBooksPage.CreatePublisher))("Save publisher")
      )
    )

object AdminBooksPage:
  sealed trait Msg extends App.Msg
  final case class BooksLoaded(value: List[Book], skip: Int, limit: Int) extends Msg
  final case class PublishersLoaded(value: List[Publisher]) extends Msg
  final case class AuthorsLoaded(value: List[Author]) extends Msg
  final case class Failed(message: String) extends Msg
  final case class SearchChanged(value: String) extends Msg
  case object SearchBooks extends Msg
  case object LoadMoreBooks extends Msg
  case object OpenAdd extends Msg
  case object CloseAdd extends Msg
  case object CloseAuthorDialog extends Msg
  case object ClosePublisherDialog extends Msg
  final case class TitleChanged(value: String) extends Msg
  final case class IsbnChanged(value: String) extends Msg
  final case class DescriptionChanged(value: String) extends Msg
  final case class GenreChanged(value: String) extends Msg
  final case class YearChanged(value: String) extends Msg
  final case class CoverChanged(value: String) extends Msg
  final case class PublisherChanged(value: String) extends Msg
  final case class AuthorQueryChanged(value: String) extends Msg
  case object SearchAuthors extends Msg
  final case class AddAuthor(value: Author) extends Msg
  final case class RemoveAuthor(id: String) extends Msg
  case object OpenAuthorDialog extends Msg
  final case class AuthorFirstNameChanged(value: String) extends Msg
  final case class AuthorLastNameChanged(value: String) extends Msg
  case object CreateAuthor extends Msg
  final case class AuthorCreated(value: String) extends Msg
  case object OpenPublisherDialog extends Msg
  final case class PublisherNameChanged(value: String) extends Msg
  final case class PublisherCountryChanged(value: String) extends Msg
  case object CreatePublisher extends Msg
  final case class PublisherCreated(value: Int) extends Msg
  case object SubmitBook extends Msg
  final case class BookCreated(value: UUID) extends Msg
  final case class OpenBook(value: Book) extends Msg
  case object CloseBook extends Msg
  final case class ConfirmDelete(value: Book) extends Msg
  case object CancelDelete extends Msg
  final case class DeleteBook(value: Book) extends Msg
  final case class DeleteConfirmed(bookId: String) extends Msg
  final case class AddCopy(bookId: UUID) extends Msg
  final case class CopyAdded(copyId: UUID) extends Msg
  final case class DeleteCopy(bookId: UUID, copyId: UUID) extends Msg
  final case class CopyDeleted(copyId: UUID) extends Msg

  private val pageSize = 100

  private def loadBooks(search: String, skip: Int): Cmd[IO, App.Msg] =
    val onResponse: tyrian.http.Response => Msg = response =>
      if response.status.code >= 200 && response.status.code < 300 then
        io.circe.parser.parse(response.body).flatMap(_.as[List[Book]]) match
          case Right(value) => BooksLoaded(value, skip, pageSize)
          case Left(error) => Failed(s"Could not read books: ${error.getMessage}")
      else Failed(AuthApi.failure(response))
    AuthApi.request(
      Constants.endpoints.books + s"?limit=$pageSize&skip=$skip&sort=new",
      Method.Post,
      Some(com.toloka.cho.domain.book.BookFilter(search = Option(search).filter(_.nonEmpty)).asJson.noSpaces),
      onResponse,
      error => Failed(error.toString)
    )

  private def loadPublishers(): Cmd[IO, App.Msg] =
    AuthApi.request(
      Constants.endpoints.publishers,
      Method.Get,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then
          io.circe.parser.decode[List[Publisher]](response.body) match
            case Right(value) => PublishersLoaded(value)
            case Left(error) => Failed(s"Could not read publishers: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString)
    )

  private def searchAuthors(query: String): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.authors}/search?query=${js.URIUtils.encodeURIComponent(query)}",
      Method.Get,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then
          io.circe.parser.decode[List[Author]](response.body) match
            case Right(value) => AuthorsLoaded(value)
            case Left(error) => Failed(s"Could not read authors: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString)
    )

  private def createBook(info: BookInfo, auth: String): Cmd[IO, App.Msg] =
    AuthApi.post[BookInfo, Msg](
      s"${Constants.endpoints.books}/create",
      info,
      response =>
        if response.status.code == 201 then
          io.circe.parser.decode[UUID](response.body) match
            case Right(id) => BookCreated(id)
            case Left(error) => Failed(s"Book was created but its response could not be read: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )

  private def createAuthor(info: AuthorInfo, auth: String): Cmd[IO, App.Msg] =
    AuthApi.post[AuthorInfo, Msg](
      s"${Constants.endpoints.authors}/create",
      info,
      response =>
        if response.status.code == 201 then AuthorCreated(response.body.replace("\"", ""))
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )

  private def createPublisher(info: PublisherInfo, auth: String): Cmd[IO, App.Msg] =
    AuthApi.post[PublisherInfo, Msg](
      s"${Constants.endpoints.publishers}/create",
      info,
      response =>
        if response.status.code == 201 then
          io.circe.parser.decode[Int](response.body) match
            case Right(id) => PublisherCreated(id)
            case Left(error) => Failed(s"Publisher was created but its response could not be read: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )

  private def deleteBook(id: UUID, auth: String): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.books}/$id",
      Method.Delete,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then DeleteConfirmed(id.toString)
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )

  private def addCopy(bookId: UUID, auth: String): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.books}/$bookId/copies",
      Method.Post,
      None,
      response =>
        if response.status.code == 201 then
          io.circe.parser.decode[UUID](response.body) match
            case Right(id) => CopyAdded(id)
            case Left(error) => Failed(s"Copy was added but its response could not be read: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )

  private def deleteCopy(bookId: UUID, copyId: UUID, auth: String): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.books}/$bookId/copies/$copyId",
      Method.Delete,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then CopyDeleted(copyId)
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString),
      List(Header("Authorization", auth))
    )
