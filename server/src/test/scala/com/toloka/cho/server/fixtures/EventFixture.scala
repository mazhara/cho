package com.toloka.cho.server.fixtures

import com.toloka.cho.domain.event.Event

import java.util.UUID

trait EventFixture {
  val Event1 = Event(
    id = UUID.fromString("40000000-0000-4000-8000-000000000001"),
    title = "Зустріч читацького клубу: сучасна українська проза",
    date = "2026-10-18 14:00",
    location = "Культурний центр «Толока», Нант",
    afisheUrl = Some("/img/events.png"),
    isOnline = false,
    language = "Українська",
    description = "Обговоримо нові голоси української літератури та оберемо книжку для наступної зустрічі. Приєднуйтеся з власними рекомендаціями.",
    offlineAddress = Some("8 Rue de la Paix, 44000 Nantes"),
    registrationUrl = None,
    isOffline = true
  )

  val Event2 = Event(
    id = UUID.fromString("40000000-0000-4000-8000-000000000002"),
    title = "Онлайн-розмова з перекладачкою",
    date = "2026-10-25 18:30",
    location = "Онлайн та наживо у культурному центрі «Толока», Нант",
    afisheUrl = Some("/img/events.png"),
    isOnline = true,
    language = "Українська та французька",
    description = "Поговоримо про те, як українські книжки знаходять нових читачів у Франції, і про виклики літературного перекладу.",
    offlineAddress = Some("8 Rue de la Paix, 44000 Nantes"),
    registrationUrl = None,
    isOffline = true
  )

  val Event3 = Event(
    id = UUID.fromString("40000000-0000-4000-8000-000000000003"),
    title = "Казкова субота для дітей",
    date = "2026-11-07 11:00",
    location = "Медіатека Жака Демі, Нант",
    afisheUrl = Some("/img/events.png"),
    isOnline = false,
    language = "Українська",
    description = "Читаємо українські казки, малюємо улюблених героїв і знайомимося з книжками для всієї родини. Подія для дітей від 5 років.",
    offlineAddress = Some("24 Quai de la Fosse, 44000 Nantes"),
    registrationUrl = None,
    isOffline = true
  )

  val Event4 = Event(
    id = UUID.fromString("40000000-0000-4000-8000-000000000004"),
    title = "Літературний вечір: Україна і Франція",
    date = "2026-11-21 19:00",
    location = "Maison de l’Europe, Нант",
    afisheUrl = Some("/img/events.png"),
    isOnline = false,
    language = "Українська та французька",
    description = "Вечір читань українською та французькою мовами про дім, пам’ять і нові початки. Тексти читатимуть учасники клубу.",
    offlineAddress = Some("33 Rue de Strasbourg, 44000 Nantes"),
    registrationUrl = None,
    isOffline = true
  )

  val Event5 = Event(
    id = UUID.fromString("40000000-0000-4000-8000-000000000005"),
    title = "Відкрита онлайн-полиця: що читаємо цієї осені",
    date = "2026-12-05 17:00",
    location = "Онлайн-зустріч у Google Meet",
    afisheUrl = Some("/img/events.png"),
    isOnline = true,
    language = "Українська",
    description = "Учасники бібліотеки діляться книжками, які їх захопили цієї осені. Можна долучитися, щоб розповісти про свою знахідку або просто послухати.",
    offlineAddress = None,
    registrationUrl = None,
    isOffline = false
  )

  val AllEvents: List[Event] = List(Event1, Event2, Event3, Event4, Event5)
}
