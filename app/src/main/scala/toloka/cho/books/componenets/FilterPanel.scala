package toloka.cho.books.componenets

import cats.effect.IO
import io.circe.generic.auto.*
import tyrian.*
import tyrian.http.*
import tyrian.Html.*
import tyrian.cmds.Logger
import toloka.cho.books.common.Endpoint
import com.toloka.cho.domain.book.BookFilter
import toloka.cho.books.common.Constants
import toloka.cho.books.App
import org.scalajs.dom.HTMLInputElement


final case class FilterPanel(
    possibleFilters: BookFilter = BookFilter(),
    selectedFilters: Map[String, Set[String]] = Map(),
    maybeError: Option[String] = None,
    year: Int = 0,
    inHallOnly: Boolean = false,
    dirty: Boolean = false,
    filterAction: Map[String, Set[String]] => App.Msg = _ => App.NoOp
) extends Component[App.Msg, FilterPanel] {
  import FilterPanel.*
  override def initCmd: Cmd[IO, App.Msg] =
    Commands.getFilters
  override def update(msg: App.Msg): (FilterPanel, Cmd[IO, App.Msg]) = msg match {
    case SetPossibleFitlers(possibleFilters) =>
      (this.copy(possibleFilters = possibleFilters), Cmd.None)
    case TriggerFilter =>
      (this.copy(dirty = false), Cmd.Emit(filterAction(selectedFilters)))
    case FilterPanelError(e)  => (this.copy(maybeError = Some(e)), Cmd.None)
    case UpdateYearInput(s) => (this.copy(year = s, dirty = true), Cmd.None)
    case UpdateInHallOnly(r)      => (this.copy(inHallOnly = r, dirty = true), Cmd.None)
    case UpdateValueChecked(groupName, value, checked) =>
      val oldGroup  = selectedFilters.get(groupName).getOrElse(Set())
      val newGroup  = if (checked) oldGroup + value else oldGroup - value
      val newGroups = selectedFilters + (groupName -> newGroup)
      (
        this.copy(selectedFilters = newGroups, dirty = true),
        Logger.consoleLog[IO](s"Filters: $newGroups")
      )
    case _ => (this, Cmd.None)
  }
  override def view(): Html[App.Msg] =
    div(`class` := "filter-panel-container")(
      maybeRenderError(),
      renderYearFilter(),
      renderInHallOnlyCheckbox(),
      renderCheckboxGroup("Authors", possibleFilters.authors),
      renderCheckboxGroup("Publishers", possibleFilters.publishers),
      renderCheckboxGroup("Tags", possibleFilters.tags),
      renderApplyFiltersButton()
    )

  private def renderYearFilter() =
    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")("Year"),
      div(`class` := "filter-group-content")(
        label(`for` := "filter-year")("Min"),
        input(
          `type` := "number",
          id     := "filter-year",
          onInput(s => UpdateYearInput(if (s.isEmpty()) 0 else s.toInt))
        )
      )
  )

  private def renderInHallOnlyCheckbox() =
    div(`class` := "filter-group-content")(
      label(`for` := "filter-checkbox")("In Hall Only"),
      input(
        `type` := "checkbox",
        id     := "filter-checkbox",
        checked(inHallOnly),
        onEvent(
          "change",
          event => {
            val checkbox = event.target.asInstanceOf[HTMLInputElement]
            UpdateInHallOnly(checkbox.checked)
          }
        )
      )
    )

  private def renderCheckboxGroup(groupName: String, values: List[String]) = {
    val selectedValues = selectedFilters.get(groupName).getOrElse(Set())
    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")(groupName),
      div(`class` := "filter-group-content")(
        values.map(value => renderCheckbox(groupName, value, selectedValues))
      )
    )
  }

  private def renderCheckbox(groupName: String, value: String, selectedValues: Set[String]) =
    div(`class` := "filter-group-content")(
      label(`for` := s"filter-$groupName-$value")(value),
      input(
        `type` := "checkbox",
        id     := s"filter-$groupName-$value",
        checked(selectedValues.contains(value)),
        onEvent(
          "change",
          event => {
            val checkbox = event.target.asInstanceOf[HTMLInputElement]
            UpdateValueChecked(groupName, value, checkbox.checked)
          }
        )
      )
    )

  private def renderApplyFiltersButton() =
    button(
      `type` := "button",
      disabled(!dirty),
      onClick(TriggerFilter)
    )("Apply Filters")

  private def maybeRenderError() =
    maybeError
      .map { e =>
        div(`class` := "filter-panel-error")(e)
      }
      .getOrElse(div())
}

object FilterPanel {
  trait Msg                                                                         extends App.Msg
  case object TriggerFilter                                                         extends Msg
  case class FilterPanelError(error: String)                                        extends Msg
  case class SetPossibleFitlers(possibleFitlers: BookFilter)                         extends Msg
  case class UpdateYearInput(year: Int)                                         extends Msg
  case class UpdateValueChecked(groupName: String, value: String, checked: Boolean) extends Msg
  case class UpdateInHallOnly(remote: Boolean)                                          extends Msg

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
