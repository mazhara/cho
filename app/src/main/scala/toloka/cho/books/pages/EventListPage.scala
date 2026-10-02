package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.event.Event
import io.circe.generic.auto._
import toloka.cho.books.App
import toloka.cho.books.common.{Endpoint, EventListPageTranslations, Language}
import toloka.cho.books.common.Language.Language
import toloka.cho.books.components.SubHeader
import tyrian.Html._
import tyrian.http._
import tyrian.{Cmd, Html}

final case class EventListPage(
    lang: Language,
    events: List[Event] = List.empty,
    filter: EventListPage.EventFilter = EventListPage.EventFilter.All,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page:

  import EventListPage._

  private implicit val language: Language = lang

  private def filterLabelKey: String = filter match
    case EventFilter.All     => "events.subheader.all"
    case EventFilter.Online  => "events.subheader.online"
    case EventFilter.Offline => "events.subheader.offline"

  private def visibleEvents: List[Event] = filter match
    case EventFilter.All     => events
    case EventFilter.Online  => events.filter(_.isOnline)
    case EventFilter.Offline => events.filter(_.isOffline)

  override def subHeader: Option[Html[App.Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem(
          EventListPageTranslations.get("events.subheader.all"),
          "",
          Some(SelectEventFilter(EventFilter.All))
        ),
        SubHeader.MenuItem(
          EventListPageTranslations.get("events.subheader.online"),
          "",
          Some(SelectEventFilter(EventFilter.Online))
        ),
        SubHeader.MenuItem(
          EventListPageTranslations.get("events.subheader.offline"),
          "",
          Some(SelectEventFilter(EventFilter.Offline))
        )
      ),
      activeItem = EventListPageTranslations.get(filterLabelKey)
    )
  )

  override def initCmd: Cmd[IO, App.Msg] = Commands.getEvents

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case AddEvents(loadedEvents) =>
      (copy(events = loadedEvents, status = Some(Page.Status("Loaded", Page.StatusKind.SUCCESS))), Cmd.None)
    case SetErrorStatus(error) =>
      (copy(status = Some(Page.Status(error, Page.StatusKind.ERROR))), Cmd.None)
    case SelectEventFilter(nextFilter) =>
      (copy(filter = nextFilter), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")(EventListPageTranslations.get("events.title")),
      hr(cls := "title-hr"),
      status match
        case Some(Page.Status(message, Page.StatusKind.LOADING)) =>
          div(cls := "event-status")(EventListPageTranslations.get("events.loading"))
        case Some(Page.Status(message, Page.StatusKind.ERROR)) =>
          div(cls := "event-status event-status-error")(message)
        case _ if visibleEvents.isEmpty =>
          div(cls := "event-status")(EventListPageTranslations.get("events.empty"))
        case _ =>
          div(cls := "event-grid")(visibleEvents.map(eventCardView))
    )

  private def eventCardView(event: Event): Html[App.Msg] =
    article(cls := "event-card")(
      event.afisheUrl match
        case Some(imageUrl) =>
          div(cls := "event-afishe-container")(
            img(src := imageUrl, alt := event.title, cls := "event-afishe")
          )
        case None =>
          div(cls := "event-afishe-container")(
            img(
              src := "/img/events.png",
              alt := EventListPageTranslations.get("events.image.placeholder"),
              cls := "event-afishe"
            )
          ),
      div(cls := "event-details")(
        div(cls := "event-copy")(
          div(cls := "event-labels")(
            (
              (if (event.isOnline)
                 List(span(cls := "event-type-label")(EventListPageTranslations.get("events.online")))
               else List.empty) ++
                (if (event.isOffline)
                   List(span(cls := "event-type-label")(EventListPageTranslations.get("events.offline")))
                 else List.empty) ++
                List(span(cls := "event-language-label")(event.language))
            )
          ),
          h3(cls := "event-title")(event.title),
          p(cls := "event-description")(event.description),
          div(cls := "event-meta")(
            span(cls := "event-meta-item")(
              i(cls := "fa-solid fa-location-dot event-meta-icon")(""),
              span(event.offlineAddress.getOrElse(event.location))
            ),
            span(cls := "event-meta-item")(
              i(cls := "fa-regular fa-calendar event-meta-icon")(""),
              span(event.date)
            )
          ),
          event.registrationUrl match
            case Some(url) if url.startsWith("https://") || url.startsWith("http://") =>
              a(
                href := url,
                target := "_blank",
                rel := "noopener noreferrer",
                cls := "event-register-button"
              )(
                span(EventListPageTranslations.get("events.register")),
                i(cls := "fa-solid fa-arrow-right event-register-icon")("")
              )
            case _ =>
              button(
                `type` := "button",
                cls := "event-register-button",
                disabled(true),
                title := EventListPageTranslations.get("events.registration.unavailable")
              )(
                span(EventListPageTranslations.get("events.register")),
                i(cls := "fa-solid fa-arrow-right event-register-icon")("")
              )
        )
      )
    )

object EventListPage:
  trait Msg extends App.Msg
  enum EventFilter:
    case All, Online, Offline

  case class AddEvents(events: List[Event]) extends Msg
  case class SetErrorStatus(error: String) extends Msg
  case class SelectEventFilter(filter: EventFilter) extends Msg

  object Endpoints:
    val getEvents = new Endpoint[Msg]:
      override val location: String = toloka.cho.books.common.Constants.endpoints.events
      override val method: Method = Method.Get
      override val onError: HttpError => Msg = error => SetErrorStatus(error.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[List[Event], Msg](AddEvents(_), SetErrorStatus(_))

  object Commands:
    def getEvents: Cmd[IO, Msg] = Endpoints.getEvents.call()
