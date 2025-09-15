package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.event.Event // Import the new Event case class
import toloka.cho.books.App
import toloka.cho.books.common.EventListPageTranslations
import toloka.cho.books.common.Language.Language
import toloka.cho.books.components.SubHeader
import tyrian.Html._
import tyrian.{Cmd, Html}

import java.util.UUID // For UUID
import scala.scalajs.js.Math

final case class EventListPage(lang: Language, events: List[Event] = List()) extends Page:

  private implicit val language: Language = lang

  override def subHeader: Option[Html[App.Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem(EventListPageTranslations.get("events.subheader.all"), ""),
        SubHeader.MenuItem(EventListPageTranslations.get("events.subheader.online"), ""),
        SubHeader.MenuItem(EventListPageTranslations.get("events.subheader.offline"), "")
      ),
      activeItem = EventListPageTranslations.get("events.subheader.all")
    )
  )

  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None)

  // Helper to generate a dummy UUID for Scala.js environment
  private def generateRandomUUID(): UUID = {
    val randomString = Math.random().toString.substring(2) + Math.random().toString.substring(2) +
      Math.random().toString.substring(2) + Math.random().toString.substring(2)
    val uuidString = (randomString + "00000000000000000000000000000000").substring(0, 32)
    UUID.fromString(
      s"${uuidString.substring(0, 8)}-${uuidString.substring(8, 12)}-${uuidString.substring(12, 16)}-${uuidString.substring(16, 20)}-${uuidString.substring(20, 32)}"
    )
  }

  override def view(): Html[App.Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")(EventListPageTranslations.get("events.title")),
      hr(cls := "title-hr"),
      div(cls := "event-grid")(
        // Dummy events
        (1 to 5).map {
          i =>
            eventCardView(
              Event(
                id = generateRandomUUID(),
                title = s"Подія $i: Назва події",
                date = s"2025-08-${26 + i}", // Changed
                location = if (i % 2 == 0) EventListPageTranslations.get("events.online")
                else "м. Нант, Франція",
                afisheUrl = Some(s"/static/img/event_placeholder.png"), // Placeholder image
                isOnline = i % 2 == 0,
                language = if (i % 3 == 0) EventListPageTranslations.get("events.ukrainian")
                else EventListPageTranslations.get("events.french"),
                description = s"Це опис події $i. Тут буде більше інформації про подію, її мету та що очікувати. Можливо, це буде довгий текст, який потрібно буде обрізати і показати кнопку 'Читати далі'.",
                offlineAddress = if (i % 2 == 0) Some("м. Київ, вул. Хрещатик, 1")
                else None // Dummy offline address
              )
            )
        }.toList
      )
    )

  private def eventCardView(event: Event): Html[App.Msg] =
    div(cls := "event-card")(
      div(cls := "event-afishe-container")(
        event.afisheUrl
          .map {
            imgUrl =>
              img(src := imgUrl, alt := event.title, cls := "event-afishe")
          }
          .getOrElse(div(cls := "no-afishe")("No Afishe"))
      ),
      div(cls := "event-details")(
        p(cls := "event-title")(event.title),
        p(cls := "event-date")(event.date),
        p(cls := "event-location")(event.location),
        p(cls := "event-description")(event.description),
        a(href := "#", cls := "read-more-link")(EventListPageTranslations.get("events.read.more")), // Moved before labels
        event.offlineAddress
          .map {
            address => // Conditionally display offline address
              p(cls := "offline-address")(
                EventListPageTranslations.get("events.offline.address") + address
              )
          }
          .getOrElse(span()),
        div(cls := "event-labels")(
          span(cls := "event-type-label")(
            if (event.isOnline) EventListPageTranslations.get("events.online")
            else EventListPageTranslations.get("events.offline")
          ),
          span(cls := "event-language-label")(event.language)
        )
      )
    )
