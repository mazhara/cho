package toloka.cho.books.components

import tyrian.*
import tyrian.Html.*
import com.toloka.cho.domain.book.Book
import toloka.cho.books.App
import toloka.cho.books.pages.Page
import toloka.cho.books.common.Constants


object BookComponents:
  def card(book: Book): Html[App.Msg] =
    div(`class` := "jvm-recent-books-cards")(
      div(`class` := "jvm-recent-books-card-img")(
        img(
          `class` := "img-fluid",
          src     := book.bookInfo.image.getOrElse(Constants.bookImagePlaceholder),
          alt     := book.bookInfo.title
        )
      ),
      div(`class` := "jvm-recent-books-card-contents")(
        h4(
          Anchors.renderSimpleNavLink(
            s"${book.bookInfo.title} - ${book.bookInfo.authors.getOrElse(Map.empty).values.mkString(" ,")}",
            Page.Urls.BOOK(book.id.toString()),
            "book-title-link"
          )
        ),
        renderBookSummary(book)
      ),
      div(`class` := "jvm-recent-books-card-btn-apply")(
        a(href := s"${Page.Urls.BOOK(book.id.toString())}", target := "blank")(
          button(`type` := "button", `class` := "btn btn-danger")("View")
        )
      )
    )
  def renderBookSummary(book: Book): Html[App.Msg] =
    div(`class` := "book-summary")(
      renderDetail("year", fullYearString(book)),
      renderDetail("publisher", book.bookInfo.publisherName.getOrElse("")),
      maybeRenderDetail("tags", book.bookInfo.tags.map(_.mkString(", ")))
    )
  def maybeRenderDetail(icon: String, maybeValue: Option[String]): Html[App.Msg] =
    maybeValue.map(value => renderDetail(icon, value)).getOrElse(div())

  def renderDetail(icon: String, value: String): Html[App.Msg] =
    div(`class` := "book-detail")(
      i(`class` := s"fa fa-$icon book-detail-icon")(),
      p(`class` := "book-detail-value")(value)
    )
  private def fullYearString(book: Book) =
    s"${book.bookInfo.publishedYear}"
