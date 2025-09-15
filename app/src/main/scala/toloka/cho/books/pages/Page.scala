package toloka.cho.books.pages

import cats.effect.*
import toloka.cho.books.App
import toloka.cho.books.common.Language.Language
import toloka.cho.books.components.Component
import tyrian.*

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }

  case class Status(message: String, kind: StatusKind)
  object Status {
    val LOADING: Status = Status("Loading", StatusKind.LOADING)
  }

  object Urls {
    val EMPTY            = ""
    val HOME             = "/" // Changed from ""
    val BOOKS            = "/books" // Changed from "books"
    val EVENTS           = "/events" // New
    val HASH             = "#"
    def BOOK(id: String) = s"/books/$id" // Changed from "books/$id"
  }

  import Urls.*
  def get(location: String, lang: Language): Page = location match {
    case `HOME`       => AboutPage(lang)
    case `EMPTY`      => BooksListPage(lang)
    case `BOOKS`      => BooksListPage(lang)
    case `EVENTS`     => EventListPage(lang)
    case _            => NotFoundPage(lang)
  }
}

abstract class Page extends Component[App.Msg, Page] {
  def subHeader: Option[Html[App.Msg]] = None
}
