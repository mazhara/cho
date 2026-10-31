package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.domain.{Author, AuthorInfo, AuthorType, CoverImage, Publisher, PublisherInfo}
import com.toloka.cho.domain.book.{Book, BookInfo}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.scalajs.dom
import com.toloka.cho.App
import com.toloka.cho.common.Constants
import com.toloka.cho.common.Language.Language
import scala.scalajs.js
import tyrian.Attribute
import tyrian.Cmd
import tyrian.Html
import tyrian.Html.*
import tyrian.http.{Header, Method}

import java.util.UUID
import scala.concurrent.duration.DurationInt

final case class AdminBooksPage(
    lang: Language,
    books: List[Book] = Nil,
    publishers: List[Publisher] = Nil,
    languageOptions: List[String] = Nil,
    selectedLanguages: List[String] = Nil,
    editingBook: Option[Book] = None,
    bookSaveRequest: Int = 0,
    search: String = "",
    submittedSearch: String = "",
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
    pasteCover: Boolean = false,
    coverReading: Boolean = false,
    coverRequest: Int = 0,
    coverError: Option[String] = None,
    publisherId: Option[Int] = None,
    publisherQuery: String = "",
    publisherDropdownOpen: Boolean = false,
    publisherActiveIndex: Int = -1,
    authorQuery: String = "",
    authorResults: List[Author] = Nil,
    authorSearching: Boolean = false,
    authorDropdownOpen: Boolean = false,
    authorSearchRequest: Int = 0,
    authorSearchError: Option[String] = None,
    authorActiveIndex: Int = -1,
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
  override def initCmd: Cmd[IO, App.Msg] =
    AdminBooksPage.loadBooks("", 0) |+| AdminBooksPage.loadPublishers() |+| AdminBooksPage.loadLanguages()

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AdminBooksPage.BooksLoaded(result, skip, limit) =>
      (copy(
        books = if skip == 0 then result else books ++ result,
        canLoadMore = result.size == limit,
        loading = false
      ), Cmd.None)
    case AdminBooksPage.PublishersLoaded(result) =>
      (copy(publishers = result), Cmd.None)
    case AdminBooksPage.LanguagesLoaded(result) => (copy(languageOptions = result), Cmd.None)
    case AdminBooksPage.ToggleBookLanguage(value) =>
      (copy(selectedLanguages = if selectedLanguages.contains(value) then selectedLanguages.filterNot(_ == value)
        else selectedLanguages :+ value), Cmd.None)
    case AdminBooksPage.AuthorsLoaded(request, result) if request == authorSearchRequest =>
      (copy(authorResults = result, authorSearching = false, authorSearchError = None, authorActiveIndex = -1), Cmd.None)
    case AdminBooksPage.AuthorSearchFailed(request, message) if request == authorSearchRequest =>
      (copy(authorResults = Nil, authorSearching = false, authorSearchError = Some(message)), Cmd.None)
    case AdminBooksPage.Failed(message) =>
      (copy(loading = false, saving = false, error = Some(message)), Cmd.None)
    case AdminBooksPage.SearchChanged(value) => (copy(search = value.take(100)), Cmd.None)
    case AdminBooksPage.SearchBooks =>
      (copy(submittedSearch = search.trim, loading = true, canLoadMore = true, error = None), AdminBooksPage.loadBooks(search.trim, 0))
    case AdminBooksPage.LoadMoreBooks if !loading && canLoadMore =>
      (copy(loading = true), AdminBooksPage.loadBooks(submittedSearch, books.length))
    case AdminBooksPage.OpenAdd =>
      (emptyBookForm.copy(addOpen = true), Cmd.None)
    case AdminBooksPage.EditBook(book) =>
      val info = book.bookInfo
      (emptyBookForm.copy(addOpen = true, editingBook = Some(book), selectedBook = None,
        title = info.title, isbn = info.isbn.getOrElse(""), description = info.description.getOrElse(""),
        genre = info.genre.getOrElse(""), publishedYear = info.publishedYear.map(_.toString).getOrElse(""),
        cover = info.image.getOrElse(""), publisherId = info.publisherId,
        selectedAuthors = info.authors.getOrElse(Map.empty), selectedLanguages = info.languages), Cmd.None)
    case AdminBooksPage.CloseAdd =>
      (copy(addOpen = false, saving = false, bookSaveRequest = bookSaveRequest + 1,
        error = None, coverReading = false, coverRequest = coverRequest + 1,
        publisherDropdownOpen = false,
        authorDropdownOpen = false, authorSearching = false, authorSearchRequest = authorSearchRequest + 1), Cmd.None)
    case AdminBooksPage.CloseAuthorDialog =>
      (copy(authorDialogOpen = false, error = None), Cmd.None)
    case AdminBooksPage.ClosePublisherDialog =>
      (copy(publisherDialogOpen = false, error = None), Cmd.None)
    case AdminBooksPage.TitleChanged(value) => (copy(title = value, error = None), Cmd.None)
    case AdminBooksPage.IsbnChanged(value) => (copy(isbn = value, error = None), Cmd.None)
    case AdminBooksPage.DescriptionChanged(value) => (copy(description = value, error = None), Cmd.None)
    case AdminBooksPage.GenreChanged(value) => (copy(genre = value, error = None), Cmd.None)
    case AdminBooksPage.YearChanged(value) => (copy(publishedYear = value.filter(_.isDigit).take(4), error = None), Cmd.None)
    case AdminBooksPage.CoverChanged(value) =>
      val request = coverRequest + 1
      CoverImage.normalize(value) match
        case Left(message) =>
          (copy(cover = value, coverError = Some(message), coverReading = false, coverRequest = request), Cmd.None)
        case Right(image) =>
          (copy(cover = value, coverError = None, coverReading = image.nonEmpty, coverRequest = request, error = None),
            if image.isEmpty then Cmd.None else AdminBooksPage.checkCover(image, request))
    case AdminBooksPage.CoverModeChanged(paste) =>
      (copy(pasteCover = paste), Cmd.None)
    case AdminBooksPage.ClearCover =>
      (copy(cover = "", coverError = None, coverReading = false, coverRequest = coverRequest + 1), Cmd.None)
    case AdminBooksPage.CoverSelected(file) =>
      val request = coverRequest + 1
      if file.size > CoverImage.maxBytes then
        (copy(coverError = Some(CoverImage.sizeError), coverReading = false, coverRequest = request), Cmd.None)
      else if file.size == 0 || (file.`type`.nonEmpty && file.`type` != "image/png" && file.`type` != "image/jpeg") then
        (copy(coverError = Some(CoverImage.formatError), coverReading = false, coverRequest = request), Cmd.None)
      else
        (copy(cover = "", coverReading = true, coverError = None, coverRequest = request), AdminBooksPage.readCover(file, request))
    case AdminBooksPage.CoverRead(request, result) if request == coverRequest =>
      result match
        case Right(value) => (copy(cover = value, coverReading = false, coverError = None), Cmd.None)
        case Left(message) => (copy(coverReading = false, coverError = Some(message)), Cmd.None)
    case AdminBooksPage.PublisherQueryChanged(value) =>
      (copy(publisherQuery = value.take(100), publisherDropdownOpen = value.trim.nonEmpty,
        publisherActiveIndex = -1), Cmd.None)
    case AdminBooksPage.PublisherFocus =>
      (copy(publisherDropdownOpen = publisherQuery.trim.nonEmpty), Cmd.None)
    case AdminBooksPage.ClosePublisherSuggestions =>
      (copy(publisherDropdownOpen = false, publisherActiveIndex = -1), Cmd.None)
    case AdminBooksPage.SelectPublisher(publisher) =>
      (copy(publisherId = Some(publisher.id), publisherQuery = "", publisherDropdownOpen = false,
        publisherActiveIndex = -1, error = None), Cmd.None)
    case AdminBooksPage.ClearPublisher =>
      (copy(publisherId = None, publisherQuery = "", publisherDropdownOpen = false,
        publisherActiveIndex = -1, error = None), Cmd.None)
    case AdminBooksPage.PublisherKey("ArrowDown") =>
      (copy(publisherDropdownOpen = publisherQuery.trim.nonEmpty,
        publisherActiveIndex = if matchingPublishers.isEmpty then -1 else (publisherActiveIndex + 1) % matchingPublishers.size), Cmd.None)
    case AdminBooksPage.PublisherKey("ArrowUp") =>
      (copy(publisherDropdownOpen = publisherQuery.trim.nonEmpty,
        publisherActiveIndex = if matchingPublishers.isEmpty then -1 else
          if publisherActiveIndex <= 0 then matchingPublishers.size - 1 else publisherActiveIndex - 1), Cmd.None)
    case AdminBooksPage.PublisherKey("Enter") if publisherDropdownOpen && publisherActiveIndex >= 0 =>
      matchingPublishers.lift(publisherActiveIndex).map(publisher => update(AdminBooksPage.SelectPublisher(publisher)))
        .getOrElse((this, Cmd.None))
    case AdminBooksPage.PublisherKey("Escape") =>
      (copy(publisherDropdownOpen = false, publisherActiveIndex = -1), Cmd.None)
    case AdminBooksPage.AuthorQueryChanged(value) =>
      val query = value.take(100)
      val request = authorSearchRequest + 1
      val next = copy(authorQuery = query, authorResults = Nil, authorSearching = query.trim.nonEmpty,
        authorDropdownOpen = query.trim.nonEmpty, authorSearchRequest = request, authorSearchError = None, authorActiveIndex = -1)
      val cmd =
        if query.trim.isEmpty then Cmd.None
        else Cmd.Run[IO, Unit, App.Msg](IO.sleep(250.millis))(_ => AdminBooksPage.SearchAuthors(request, query.trim))
      (next, cmd)
    case AdminBooksPage.SearchAuthors(request, query) if request == authorSearchRequest =>
      (this, AdminBooksPage.searchAuthors(query, request))
    case AdminBooksPage.AuthorFocus =>
      (copy(authorDropdownOpen = authorQuery.trim.nonEmpty), Cmd.None)
    case AdminBooksPage.CloseAuthorSuggestions =>
      (copy(authorDropdownOpen = false, authorActiveIndex = -1), Cmd.None)
    case AdminBooksPage.AuthorKey("ArrowDown") =>
      (copy(authorDropdownOpen = authorQuery.trim.nonEmpty,
        authorActiveIndex = if availableAuthors.isEmpty then -1 else (authorActiveIndex + 1) % availableAuthors.size), Cmd.None)
    case AdminBooksPage.AuthorKey("ArrowUp") =>
      (copy(authorDropdownOpen = authorQuery.trim.nonEmpty,
        authorActiveIndex = if availableAuthors.isEmpty then -1 else
          if authorActiveIndex <= 0 then availableAuthors.size - 1 else authorActiveIndex - 1), Cmd.None)
    case AdminBooksPage.AuthorKey("Enter") if authorDropdownOpen && authorActiveIndex >= 0 =>
      availableAuthors.lift(authorActiveIndex).map(author => update(AdminBooksPage.AddAuthor(author))).getOrElse((this, Cmd.None))
    case AdminBooksPage.AuthorKey("Escape") =>
      (copy(authorDropdownOpen = false, authorActiveIndex = -1), Cmd.None)
    case AdminBooksPage.AddAuthor(author) =>
      val id = author.id.toString
      val name = List(author.authorInfo.firstName.getOrElse(""), author.authorInfo.lastName).mkString(" ").trim
      (copy(selectedAuthors = selectedAuthors.updated(id, name), authorQuery = "", authorResults = Nil,
        authorDropdownOpen = false, authorSearching = false, authorSearchError = None,
        authorActiveIndex = -1, authorSearchRequest = authorSearchRequest + 1), Cmd.None)
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
      copy(saving = false, authorDialogOpen = false, authorFirstName = "", authorLastName = "")
        .update(AdminBooksPage.AuthorQueryChanged(authorLastName.trim))
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
      val publisher = Publisher(id, PublisherInfo(publisherName.trim,
        Option(publisherCountry.trim).filter(_.nonEmpty), None, None, None))
      (copy(saving = false, publisherDialogOpen = false, publisherName = "", publisherCountry = "",
        publishers = publishers.filterNot(_.id == id) :+ publisher,
        publisherId = Some(id), publisherQuery = "", publisherDropdownOpen = false,
        publisherActiveIndex = -1), AdminBooksPage.loadPublishers())
    case AdminBooksPage.SubmitBook if saving => (this, Cmd.None)
    case AdminBooksPage.SubmitBook =>
      val normalizedCover = CoverImage.normalize(cover)
      if coverReading then (copy(coverError = Some("Wait for the image to finish loading.")), Cmd.None)
      else if coverError.nonEmpty then (this, Cmd.None)
      else if normalizedCover.isLeft then
        (copy(coverError = normalizedCover.left.toOption), Cmd.None)
      else if title.trim.isEmpty then (copy(error = Some("Book title is required.")), Cmd.None)
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
              editingBook.flatMap(_.bookInfo.tags),
              normalizedCover.toOption.filter(_.nonEmpty),
              editingBook.flatMap(_.bookInfo.copies),
              selectedLanguages
            )
            val request = bookSaveRequest + 1
            val cmd = editingBook match
              case Some(book) => AdminBooksPage.updateBook(book.id, info, auth, request)
              case None => AdminBooksPage.createBook(info, auth, request)
            (copy(saving = true, error = None, bookSaveRequest = request), cmd)
          case None => (copy(error = Some("Your staff session has expired. Sign in again.")), Cmd.None)
    case AdminBooksPage.BookSaved(request) =>
      val next = if request == bookSaveRequest && addOpen then emptyBookForm.copy(addOpen = false) else this
      (next.copy(books = Nil, loading = true, canLoadMore = true), AdminBooksPage.loadBooks(submittedSearch, 0))
    case AdminBooksPage.BookSaveFailed(request, message) if request == bookSaveRequest && addOpen =>
      (copy(saving = false, error = Some(message)), Cmd.None)
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
      (copy(selectedBook = None, loading = true, canLoadMore = true), AdminBooksPage.loadBooks(submittedSearch, 0))
    case AdminBooksPage.CopyDeleted(_) =>
      (copy(selectedBook = None, loading = true, canLoadMore = true), AdminBooksPage.loadBooks(submittedSearch, 0))
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
          AdminViewPage(lang).currentUser,
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
              thead(tr(th("Book"), th("Author"), th("Language"), th("Availability"), th("Actions"))),
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
          info.image.map(image => img(src := CoverImage.dataUrl(image), alt := "", cls := "admin-book-thumb")).getOrElse(div(cls := "admin-book-thumb-placeholder")(i(cls := "fa-solid fa-book")(""))),
          div(strong(info.title), small(info.publisherName.getOrElse("Publisher not set")))
        )
      ),
      td(info.authors.map(_.values.mkString(", ")).getOrElse("—")),
      td(info.languages.mkString(", ") match
        case "" => "Not specified"
        case languages => languages),
      td(
        span(cls := (if available > 0 then "admin-book-availability" else "admin-book-unavailable"))(
          if copies.isEmpty then "No copies" else if available > 0 then "Available" else "Borrowed"
        ),
        small(cls := "admin-book-copy-count")(s"$available of ${copies.size} copies available")
      ),
      td(cls := "admin-book-actions")(
        button(`type` := "button", Attribute("title", "Edit book"), Attribute("aria-label", s"Edit ${info.title}"),
          onClick(AdminBooksPage.EditBook(book)))(i(cls := "fa-solid fa-pen")("")),
        button(`type` := "button", Attribute("title", "View book"), Attribute("aria-label", s"View ${info.title}"),
          onClick(AdminBooksPage.OpenBook(book)))(i(cls := "fa-regular fa-eye")("")),
        button(`type` := "button", Attribute("title", "Delete book"), Attribute("aria-label", s"Delete ${info.title}"),
          onClick(AdminBooksPage.ConfirmDelete(book)))(i(cls := "fa-regular fa-trash-can")(""))
      )
    )

  private def addBookDialog: Html[App.Msg] =
    modal(if editingBook.nonEmpty then "Edit book" else "Add a book", AdminBooksPage.CloseAdd)(
      div(cls := "admin-book-form-grid")(
        textField("Title", title, AdminBooksPage.TitleChanged(_), isRequired = true),
        textField("ISBN", isbn, AdminBooksPage.IsbnChanged(_)),
        textField("Genre", genre, AdminBooksPage.GenreChanged(_)),
        textField("Publication year", publishedYear, AdminBooksPage.YearChanged(_)),
        div(cls := "admin-book-field admin-book-field-wide")(
          label("Language"),
          div(cls := "admin-book-language-options")(
            (languageOptions ++ selectedLanguages).distinct.sorted.map { language =>
              label(
                input(`type` := "checkbox", checked(selectedLanguages.contains(language)),
                  onChange(AdminBooksPage.ToggleBookLanguage(language))),
                text(language)
              )
            }
          ),
          if languageOptions.isEmpty then small("Language options are loading. Reopen this page if loading fails.") else small()
        ),
        div(cls := "admin-book-field admin-book-field-wide")(
          label("Description"),
          input(`type` := "text", Attribute("value", description), onInput(AdminBooksPage.DescriptionChanged(_)))
        ),
        coverField,
        div(cls := "admin-book-field admin-book-field-wide")(
          label(Attribute("for", "book-publisher-search"))("Publisher"),
          div(cls := "admin-book-inline-controls")(
            publisherSearchField,
            button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.OpenPublisherDialog))("Add publisher")
          ),
          small("Start typing a publisher's name, then choose a suggestion."),
          div(cls := "admin-book-selected-authors")(
            publisherId.toList.map { selectedId =>
              val name = publishers.find(_.id == selectedId).map(_.info.name)
                .orElse(editingBook.flatMap(_.bookInfo.publisherName)).getOrElse(s"Publisher $selectedId")
              span(text(name), button(`type` := "button", Attribute("title", s"Remove $name"),
                onClick(AdminBooksPage.ClearPublisher))("×"))
            }
          )
        ),
        div(cls := "admin-book-field admin-book-field-wide")(
          label(Attribute("for", "book-author-search"))("Authors"),
          div(cls := "admin-book-inline-controls")(
            authorSearchField,
            button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.OpenAuthorDialog))("Add author")
          ),
          small("Start typing an author's name, then choose a suggestion. You can select multiple authors."),
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
        button(`type` := "button", cls := "admin-book-primary", disabled(saving || coverReading || coverError.nonEmpty), onClick(AdminBooksPage.SubmitBook))(
          if saving then "Saving…" else "Save book"
        )
      )
    )

  private def availableAuthors: List[Author] =
    authorResults.filterNot(author => selectedAuthors.contains(author.id.toString))

  private def emptyBookForm: AdminBooksPage =
    copy(editingBook = None, bookSaveRequest = bookSaveRequest + 1,
      title = "", isbn = "", description = "", genre = "", publishedYear = "",
      cover = "", pasteCover = false, coverReading = false, coverError = None, coverRequest = coverRequest + 1,
      publisherId = None, publisherQuery = "", publisherDropdownOpen = false, publisherActiveIndex = -1,
      selectedLanguages = Nil, selectedAuthors = Map.empty, authorQuery = "", authorResults = Nil,
      authorSearching = false, authorDropdownOpen = false, authorSearchError = None, authorActiveIndex = -1,
      authorSearchRequest = authorSearchRequest + 1, saving = false, error = None)

  private def matchingPublishers: List[Publisher] =
    val query = publisherQuery.trim.toLowerCase
    if query.isEmpty then Nil
    else publishers.filter(publisher => !publisherId.contains(publisher.id) &&
      publisher.info.name.toLowerCase.contains(query))

  private def publisherSearchField: Html[App.Msg] =
    div(cls := "admin-author-autocomplete",
      onEvent("focusout", (event: dom.FocusEvent) =>
        val container = event.currentTarget.asInstanceOf[dom.Node]
        if event.relatedTarget != null && container.contains(event.relatedTarget.asInstanceOf[dom.Node]) then App.NoOp
        else AdminBooksPage.ClosePublisherSuggestions
      )
    )(
      div(cls := "admin-author-search-input")(
        i(cls := "fa-solid fa-magnifying-glass", Attribute("aria-hidden", "true"))(""),
        input(
          id := "book-publisher-search", `type` := "search", Attribute("placeholder", "Type to search publishers..."),
          Attribute("value", publisherQuery), Attribute("autocomplete", "off"),
          Attribute("role", "combobox"), Attribute("aria-autocomplete", "list"),
          Attribute("aria-expanded", publisherDropdownOpen.toString), Attribute("aria-controls", "book-publisher-suggestions"),
          Attribute("aria-activedescendant", if publisherActiveIndex >= 0 then s"book-publisher-option-$publisherActiveIndex" else ""),
          onInput(AdminBooksPage.PublisherQueryChanged(_)), onFocus(AdminBooksPage.PublisherFocus),
          onEvent("keydown", (event: dom.KeyboardEvent) =>
            if Set("ArrowDown", "ArrowUp", "Enter", "Escape").contains(event.key) then
              event.preventDefault()
              AdminBooksPage.PublisherKey(event.key)
            else App.NoOp
          )
        )
      ),
      if publisherDropdownOpen then
        div(cls := "admin-author-dropdown")(
          div(Attribute("role", "status"), cls := "admin-author-search-status")(
            if matchingPublishers.isEmpty then "No matching publishers. Use Add publisher to create one."
            else "Choose a publisher below."
          ),
          div(id := "book-publisher-suggestions", Attribute("role", "listbox"), Attribute("aria-label", "Matching publishers"))(
            matchingPublishers.zipWithIndex.map { case (publisher, index) =>
              button(`type` := "button", id := s"book-publisher-option-$index",
                cls := (if index == publisherActiveIndex then "admin-author-option is-active" else "admin-author-option"),
                Attribute("role", "option"), Attribute("aria-selected", (index == publisherActiveIndex).toString),
                onClick(AdminBooksPage.SelectPublisher(publisher)))(publisher.info.name)
            }
          )
        )
      else div()
    )

  private def authorSearchField: Html[App.Msg] =
    div(cls := "admin-author-autocomplete",
      onEvent("focusout", (event: dom.FocusEvent) =>
        val container = event.currentTarget.asInstanceOf[dom.Node]
        if event.relatedTarget != null && container.contains(event.relatedTarget.asInstanceOf[dom.Node]) then App.NoOp
        else AdminBooksPage.CloseAuthorSuggestions
      )
    )(
      div(cls := "admin-author-search-input")(
        i(cls := "fa-solid fa-magnifying-glass", Attribute("aria-hidden", "true"))(""),
        input(
          id := "book-author-search", `type` := "search", Attribute("placeholder", "Type to search authors..."),
          Attribute("value", authorQuery), Attribute("autocomplete", "off"),
          Attribute("role", "combobox"), Attribute("aria-autocomplete", "list"),
          Attribute("aria-expanded", authorDropdownOpen.toString), Attribute("aria-controls", "book-author-suggestions"),
          Attribute("aria-activedescendant", if authorActiveIndex >= 0 then s"book-author-option-$authorActiveIndex" else ""),
          onInput(AdminBooksPage.AuthorQueryChanged(_)), onFocus(AdminBooksPage.AuthorFocus),
          onEvent("keydown", (event: dom.KeyboardEvent) =>
            if Set("ArrowDown", "ArrowUp", "Enter", "Escape").contains(event.key) then
              event.preventDefault()
              AdminBooksPage.AuthorKey(event.key)
            else App.NoOp
          )
        )
      ),
      if authorDropdownOpen then
        div(cls := "admin-author-dropdown")(
          div(Attribute("role", "status"), cls := "admin-author-search-status")(
            if authorSearching then "Searching authors..."
            else authorSearchError.getOrElse(
              if availableAuthors.isEmpty then "No matching authors. Use Add author to create one." else "Choose an author below."
            )
          ),
          div(id := "book-author-suggestions", Attribute("role", "listbox"), Attribute("aria-label", "Matching authors"))(
            availableAuthors.zipWithIndex.map { case (author, index) =>
              val name = List(author.authorInfo.firstName.getOrElse(""), author.authorInfo.lastName).mkString(" ").trim
              button(`type` := "button", id := s"book-author-option-$index",
                cls := (if index == authorActiveIndex then "admin-author-option is-active" else "admin-author-option"),
                Attribute("role", "option"), Attribute("aria-selected", (index == authorActiveIndex).toString),
                onClick(AdminBooksPage.AddAuthor(author)))(name)
            }
          )
        )
      else div()
    )

  private def coverField: Html[App.Msg] =
    div(cls := "admin-book-field admin-book-field-wide")(
      label("Cover image (optional)"),
      fieldset(cls := "admin-cover-source")(
        legend("Choose how to add your image"),
        div(cls := "admin-cover-source-options")(
          label(cls := (if !pasteCover then "admin-cover-source-option is-selected" else "admin-cover-source-option"))(
            input(`type` := "radio", name := "cover-source", checked(!pasteCover),
              onChange(AdminBooksPage.CoverModeChanged(false))),
            i(cls := "fa-solid fa-arrow-up-from-bracket", Attribute("aria-hidden", "true"))(""),
            span(strong("Upload a file"), small("Choose an image from your device"))
          ),
          label(cls := (if pasteCover then "admin-cover-source-option is-selected" else "admin-cover-source-option"))(
            input(`type` := "radio", name := "cover-source", checked(pasteCover),
              onChange(AdminBooksPage.CoverModeChanged(true))),
            i(cls := "fa-solid fa-code", Attribute("aria-hidden", "true"))(""),
            span(strong("Paste base64"), small("Use an existing encoded image"))
          )
        )
      ),
      small(
        if pasteCover then "Paste your image below. PNG or JPEG, maximum 2 MB decoded."
        else "Select a PNG or JPEG file, maximum 2 MB. We convert it to base64 for you."
      ),
      if pasteCover then
        textarea(
          Attribute("aria-label", "Cover image base64"),
          Attribute("placeholder", "Paste raw base64 or a data:image/png;base64,... / data:image/jpeg;base64,... URL"),
          Attribute("value", cover),
          onInput(AdminBooksPage.CoverChanged(_))
        )(cover)
      else
        input(
          `type` := "file",
          Attribute("accept", "image/png,image/jpeg"),
          Attribute("aria-label", "Upload cover image"),
          disabled(coverReading),
          onEvent("change", (event: dom.Event) =>
            val element = event.target.asInstanceOf[dom.html.Input]
            val files = element.files
            val selected = if files != null && files.length > 0 then Some(files(0)) else None
            element.value = ""
            selected.map(AdminBooksPage.CoverSelected.apply).getOrElse(App.NoOp)
          )
        ),
      if coverReading then span(Attribute("role", "status"))("Reading image...") else span(),
      coverError.map(message => div(cls := "admin-books-error", Attribute("role", "alert"))(message)).getOrElse(div()),
      CoverImage.normalize(cover).toOption.filter(image => image.nonEmpty && !coverReading && coverError.isEmpty).map { image =>
        img(src := CoverImage.dataUrl(image), alt := "Cover preview", cls := "admin-book-cover-preview")
      }.getOrElse(div()),
      if cover.nonEmpty || coverError.nonEmpty || coverReading then
        button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.ClearCover))("Remove image")
      else div()
    )

  private def bookDialog(book: Book): Html[App.Msg] =
    modal("View book", AdminBooksPage.CloseBook)(
      div(cls := "admin-book-detail")(
        book.bookInfo.image.map(image =>
          img(src := CoverImage.dataUrl(image), alt := book.bookInfo.title, cls := "admin-book-detail-cover")
        ).getOrElse(div(cls := "admin-book-detail-placeholder")(i(cls := "fa-solid fa-book-open")(""))),
        div(
          h3(book.bookInfo.title),
          p(cls := "admin-book-id")(s"Book ID: ${book.id}"),
          p(s"Author(s): ${book.bookInfo.authors.map(_.values.mkString(", ")).getOrElse("Not specified")}"),
          p(s"Language: ${if book.bookInfo.languages.isEmpty then "Not specified" else book.bookInfo.languages.mkString(", ")}"),
          p(s"Availability: ${if book.bookInfo.copies.exists(_.exists(_.available)) then "Available" else if book.bookInfo.copies.exists(_.nonEmpty) then "Borrowed" else "No copies"}"),
          p(s"Copies: ${book.bookInfo.copies.fold(0)(_.size)}")
        )
      ),
      p(s"ISBN: ${book.bookInfo.isbn.getOrElse("—")}"),
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
      ),
      dialogError,
      div(cls := "admin-book-dialog-actions")(
        button(`type` := "button", cls := "admin-book-secondary", onClick(AdminBooksPage.CloseBook))("Close"),
        button(`type` := "button", cls := "admin-book-primary", onClick(AdminBooksPage.EditBook(book)))("Edit book")
      )
    )

  private def deleteDialog(book: Book): Html[App.Msg] =
    modal("Delete book?", AdminBooksPage.CancelDelete)(
      p(s"“${book.bookInfo.title}” and its copies will be permanently removed."),
      dialogError,
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
  final case class LanguagesLoaded(value: List[String]) extends Msg
  final case class ToggleBookLanguage(value: String) extends Msg
  final case class EditBook(book: Book) extends Msg
  final case class BookSaved(request: Int) extends Msg
  final case class BookSaveFailed(request: Int, message: String) extends Msg
  final case class AuthorsLoaded(request: Int, value: List[Author]) extends Msg
  final case class AuthorSearchFailed(request: Int, message: String) extends Msg
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
  final case class CoverModeChanged(paste: Boolean) extends Msg
  final case class CoverSelected(file: dom.File) extends Msg
  final case class CoverRead(request: Int, result: Either[String, String]) extends Msg
  case object ClearCover extends Msg
  final case class PublisherQueryChanged(value: String) extends Msg
  case object PublisherFocus extends Msg
  case object ClosePublisherSuggestions extends Msg
  final case class PublisherKey(key: String) extends Msg
  final case class SelectPublisher(publisher: Publisher) extends Msg
  case object ClearPublisher extends Msg
  final case class AuthorQueryChanged(value: String) extends Msg
  final case class SearchAuthors(request: Int, query: String) extends Msg
  case object AuthorFocus extends Msg
  case object CloseAuthorSuggestions extends Msg
  final case class AuthorKey(key: String) extends Msg
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

  private def validateCover(image: String): IO[Either[String, String]] =
    IO.async_[Either[String, String]] { complete =>
      val preview = dom.document.createElement("img").asInstanceOf[dom.html.Image]
      preview.onload = (_: dom.Event) =>
        complete(Right(if preview.naturalWidth > 0 && preview.naturalHeight > 0 then Right(image)
          else Left(CoverImage.formatError)))
      preview.addEventListener("error", (_: dom.Event) =>
        complete(Right(Left(CoverImage.formatError))))
      preview.src = CoverImage.dataUrl(image)
    }

  private def checkCover(image: String, request: Int): Cmd[IO, App.Msg] =
    Cmd.Run[IO, Either[String, String], App.Msg](validateCover(image))(result => CoverRead(request, result))

  private def readCover(file: dom.File, request: Int): Cmd[IO, App.Msg] =
    val read = IO.async_[Either[String, String]] { complete =>
      val reader = new dom.FileReader()
      reader.onload = (_: dom.Event) =>
        complete(Right(CoverImage.normalize(reader.result.toString)))
      reader.onerror = (_: dom.Event) =>
        complete(Right(Left("Could not read the image. Please select it again.")))
      reader.onabort = (_: dom.Event) =>
        complete(Right(Left("Image reading was cancelled. Please select it again.")))
      reader.readAsDataURL(file)
    }
    val validated = read.flatMap {
      case Right(image) => validateCover(image)
      case Left(message) => IO.pure(Left(message))
    }
    Cmd.Run[IO, Either[String, String], App.Msg](validated)(result => CoverRead(request, result))

  private def loadBooks(search: String, skip: Int): Cmd[IO, App.Msg] =
    val onResponse: tyrian.http.Response => Msg = response =>
      if response.status.code >= 200 && response.status.code < 300 then
        io.circe.parser.parse(response.body).flatMap(_.as[List[Book]]) match
          case Right(value) => BooksLoaded(value, skip, pageSize)
          case Left(error) => Failed(s"Could not read books: ${error.getMessage}")
      else Failed(AuthApi.failure(response))
    AuthSession.authorization match
      case Some(auth) =>
        AuthApi.request(
          Constants.endpoints.books + s"/manage?limit=$pageSize&skip=$skip&sort=new",
          Method.Post,
          Some(com.toloka.cho.domain.book.BookFilter(search = Option(search).filter(_.nonEmpty)).asJson.noSpaces),
          onResponse,
          error => Failed(error.toString),
          List(Header("Authorization", auth))
        )
      case None =>
        Cmd.Run[IO, Unit, App.Msg](IO.unit)(_ => Failed("Your staff session has expired. Sign in again."))

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

  private def loadLanguages(): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.books}/languages", Method.Get, None,
      response =>
        if response.status.code == 200 then
          io.circe.parser.decode[List[String]](response.body) match
            case Right(value) => LanguagesLoaded(value)
            case Left(error) => Failed(s"Could not read languages: ${error.getMessage}")
        else Failed(AuthApi.failure(response)),
      error => Failed(error.toString)
    )

  private def updateBook(id: UUID, info: BookInfo, auth: String, request: Int): Cmd[IO, App.Msg] =
    AuthApi.post[BookInfo, Msg](
      s"${Constants.endpoints.books}/$id", info,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then BookSaved(request)
        else BookSaveFailed(request, AuthApi.failure(response)),
      error => BookSaveFailed(request, error.toString),
      List(Header("Authorization", auth)), Method.Put
    )

  private def searchAuthors(query: String, request: Int): Cmd[IO, App.Msg] =
    AuthApi.request(
      s"${Constants.endpoints.authors}/search?query=${js.URIUtils.encodeURIComponent(query)}",
      Method.Get,
      None,
      response =>
        if response.status.code >= 200 && response.status.code < 300 then
          io.circe.parser.decode[List[Author]](response.body) match
            case Right(value) => AuthorsLoaded(request, value)
            case Left(error) => AuthorSearchFailed(request, s"Could not read authors: ${error.getMessage}")
        else if response.status.code == 404 then AuthorsLoaded(request, Nil)
        else AuthorSearchFailed(request, AuthApi.failure(response)),
      error => AuthorSearchFailed(request, error.toString)
    )

  private def createBook(info: BookInfo, auth: String, request: Int): Cmd[IO, App.Msg] =
    AuthApi.post[BookInfo, Msg](
      s"${Constants.endpoints.books}/create",
      info,
      response =>
        if response.status.code == 201 then
          io.circe.parser.decode[UUID](response.body) match
            case Right(_) => BookSaved(request)
            case Left(error) => BookSaveFailed(request, s"Book was created but its response could not be read: ${error.getMessage}")
        else BookSaveFailed(request, AuthApi.failure(response)),
      error => BookSaveFailed(request, error.toString),
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
