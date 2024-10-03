package toloka.cho.books.componenets

import cats.effect.IO
import io.circe.generic.auto.*
import tyrian.*
import tyrian.http.*
import tyrian.Html.*
import toloka.cho.books.common.Endpoint
import com.toloka.cho.domain.book.BookFilter
import toloka.cho.books.common.Constants
import toloka.cho.books.App

final case class FilterPanel(
    possibleFilters: BookFilter = BookFilter(),
    maybeError: Option[String] = None
) extends Component[App.Msg, FilterPanel] {
  import FilterPanel.*
  override def initCmd: Cmd[IO, App.Msg] =
    Commands.getFilters
  override def update(msg: App.Msg): (FilterPanel, Cmd[IO, App.Msg]) = msg match {
    case SetPossibleFitlers(possibleFilters) =>
      (this.copy(possibleFilters = possibleFilters), Cmd.None)
    case FilterPanelError(e) => (this.copy(maybeError = Some(e)), Cmd.None)
    case _                   => (this, Cmd.None)
  }
  override def view(): Html[App.Msg] =
    div(`class` := "filter-panel-container")(
      maybeRenderError(),
      div(possibleFilters.toString)
    )
  private def maybeRenderError() =
    maybeError
      .map { e =>
        div(`class` := "filter-panel-error")(e)
      }
      .getOrElse(div())
}
object FilterPanel {
  trait Msg                                                 extends App.Msg
  case class FilterPanelError(error: String)                extends Msg
  case class SetPossibleFitlers(possibleFitlers: BookFilter) extends Msg
  object Endpoints {
    val getFilters = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.getFilters
      override val method: Method            = Method.Get
      override val onError: HttpError => Msg = e => FilterPanelError(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[BookFilter, Msg](
          SetPossibleFitlers(_),
          FilterPanelError(_)
        )
    }
  }
  object Commands {
    def getFilters = Endpoints.getFilters.call()
  }
}
