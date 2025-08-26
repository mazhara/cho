package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.book.{Book, BookFilter, BookInfo, BookCopy}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import toloka.cho.books._
import toloka.cho.books.components.SubHeader
import tyrian.{Cmd, Html}
import tyrian.Html._
import tyrian.cmds.Logger
import tyrian.http._
import java.util.UUID
import scala.scalajs.js.Math


final case class BooksListPage(
    jobFilter: BookFilter = BookFilter(),
    books: List[Book] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page:

  // Helper to generate a dummy UUID for Scala.js environment
  private def generateRandomUUID(): UUID = {
    // This is a simplified UUID generation for dummy data, not cryptographically secure
    // It generates a random string and converts it to a UUID.
    // In a real application, you'd use a proper UUID library for Scala.js.
    val randomString = Math.random().toString.substring(2) + Math.random().toString.substring(2) +
                       Math.random().toString.substring(2) + Math.random().toString.substring(2)
    // Pad or truncate to make it look like a UUID string
    val uuidString = (randomString + "00000000000000000000000000000000").substring(0, 32)
    UUID.fromString(s"${uuidString.substring(0,8)}-${uuidString.substring(8,12)}-${uuidString.substring(12,16)}-${uuidString.substring(16,20)}-${uuidString.substring(20,32)}")
  }

  override def subHeader: Option[Html[App.Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem("Надходження", ""),
        SubHeader.MenuItem("Автор", ""),
        SubHeader.MenuItem("Назва", "")
      ),
      activeItem = "Надходження"
    )
  )

  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")("шокайте на здоровля"),
      hr(cls := "title-hr"),
      div(cls := "sorting-options")(
        span("Сортувати за "),
        a(href := "#", cls := "sort-option active")("Надходження"),
        a(href := "#", cls := "sort-option")("Автор"),
        a(href := "#", cls := "sort-option")("Назва")
      ),
      div(cls := "book-grid")(
        // Placeholder for book cards
        // I'll generate some dummy books for now
        (1 to 10).map { i =>
          bookCardView(
            Book(
              id = generateRandomUUID(),
              bookInfo = BookInfo(
                title = s"Book Title $i",
                isbn = None,
                description = None,
                authors = Some(Map("author1" -> s"Author Name $i")),
                publisherId = None,
                publisherName = None,
                genre = None,
                publishedYear = Some(2023),
                tags = None,
                image = Some(s"/static/img/book.png"), // Placeholder image
                copies = Some(List(BookCopy(generateRandomUUID(), 1, i % 3 == 0, false))) // Every 3rd book is taken
              )
            )
          )
        }.toList
      ),
      div(cls := "load-more-container")(
        button(cls := "load-more-button")("Завантажити більше")
      )
    )


  private def bookCardView(book: Book): Html[App.Msg] =
    div(cls := "book-card")(
      div(cls := "book-cover-container")(
        book.bookInfo.image.map { imgUrl =>
          img(src := imgUrl, alt := book.bookInfo.title, cls := "book-cover")
        }.getOrElse(div(cls := "no-cover")("No Cover"))
      ),
      div(cls := "book-details")(
        p(cls := "book-title")(book.bookInfo.title),
        p(cls := "book-author")(book.bookInfo.authors.map(_.values.mkString(", ")).getOrElse("Unknown Author")),
        book.bookInfo.copies.exists(_.exists(!_.available)) match {
          case true => span(cls := "book-taken-label")("Читають")
          case false => span() // Empty span if not taken
        }
      )
    )

  private def loadMoreButtonView(): Html[App.Msg] =
    div(cls := "load-more-container")(
      button(cls := "load-more-button")("Завантажити більше")
    )
