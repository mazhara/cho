package toloka.cho.books.components

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
import toloka.cho.books.components.Component


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
    div(`class` := "accordion accordion-flush", id := "accordionFlushExample")(
    div(`class` := "accordion-item")(
      h2(`class` := "accordion-header", id := "flush-headingOne")(
        button(
          `class` := "accordion-button",
          id      := "accordion-search-filter",
          `type`  := "button",
          attribute("data-bs-toggle", "collapse"),
          attribute("data-bs-target", "#flush-collapseOne"),
          attribute("aria-expanded", "true"),
          attribute("aria-controls", "flush-collapseOne")
        )(
          div(`class` := "jvm-recent-books-accordion-body-heading")(
            h3(span("Search"), text(" Filters"))
          )
        )
      ),
       
      div(
        `class` := "accordion-collapse collapse show",
        id      := "flush-collapseOne",
        attribute("aria-labelledby", "flush-headingOne"),
        attribute("data-bs-parent", "#accordionFlushExample")
      )(
        div(`class` := "accordion-body p-0")(
          maybeRenderError(),
          renderYearFilter(),
          renderInHallOnlyCheckbox(),
          renderCheckboxGroup("Authors", possibleFilters.authors),
          renderCheckboxGroup("Publishers", possibleFilters.publishers),
          renderCheckboxGroup("Tags", possibleFilters.tags),
          renderApplyFiltersButton()
        )
      )
    )
      
  )

  private def renderFilterGroup(groupName: String, contents: Html[App.Msg]) =
    div(`class` := "accordion-item")(
      h2(`class` := "accordion-header", id := s"heading$groupName")(
        button(
          `class` := "accordion-button collapsed",
          `type`  := "button",
          attribute("data-bs-toggle", "collapse"),
          attribute("data-bs-target", s"#collapse$groupName"),
          attribute("aria-expanded", "false"),
          attribute("aria-controls", s"collapse$groupName")
        )(
          groupName
        )
      ),
      div(
        `class` := "accordion-collapse collapse",
        id      := s"collapse$groupName",
        attribute("aria-labelledby", "headingOne"),
        attribute("data-bs-parent", "#accordionExample")
      )(
        div(`class` := "accordion-body")(
          contents
        )
      )
    )
 

  private def renderYearFilter() =
    renderFilterGroup(
      "Year",
      div(`class` := "mb-3")(
        label(`class` := "form-check-label", `for` := "filter-year")("Min"),
        input(
          `type` := "number",
          id     := "filter-year",
          onInput(s => UpdateYearInput(if (s.isEmpty()) 0 else s.toInt))
        )
      )
  )

  private def renderInHallOnlyCheckbox() =
   renderFilterGroup(
      "InHallOnly",
      div(`class` := "form-check")(
        label(`for` := "filter-checkbox")("Remote"),
        input(
          `class` := "form-check-input",
          `type`  := "checkbox",
          id      := "filter-checkbox",
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
    )

  private def renderCheckboxGroup(groupName: String, values: List[String]) = {
    val selectedValues = selectedFilters.get(groupName).getOrElse(Set())
    renderFilterGroup(
      groupName,
      div(`class` := "mb-3")(
        values.map(value => renderCheckbox(groupName, value, selectedValues))
      )
    )
  }

  private def renderCheckbox(groupName: String, value: String, selectedValues: Set[String]) =
    div(`class` := "form-check")(
      label(`class` := "form-check-label", `for` := s"filter-$groupName-$value")(value),
      input(
        `class` := "form-check-input",
        `type`  := "checkbox",
        id      := s"filter-$groupName-$value",
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
     div(`class` := "jvm-accordion-search-btn")(
      button(
        `class` := "btn btn-primary",
        `type`  := "button",
        disabled(!dirty),
        onClick(TriggerFilter)
      )("Apply Filters")
    )

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
