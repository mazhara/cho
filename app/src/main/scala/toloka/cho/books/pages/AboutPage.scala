package toloka.cho.books.pages

import cats.effect.IO
import toloka.cho.books.App
import tyrian.Html.*
import tyrian.{Cmd, Html}

case class AboutPage() extends Page {
  import App.Msg

  override def view(): Html[Msg] =
    section(cls := "bg-white dark:bg-gray-900")(
        div(cls := "py-8 px-4 mx-auto max-w-screen-xl lg:py-16 lg:px-12")(
            h2(cls := "mb-4 text-4xl tracking-tight text-center font-extrabold text-gray-900 dark:text-white")("Українська бібліотека міста Нант \"ШО\""),
            p(cls := "mb-4 text-lg font-normal text-gray-500 lg:text-xl sm:px-8 xl:px-24 dark:text-gray-400")(
                "\uD83D\uDCCD Мануфактура, Salle de la Cigarière, 3 cours Jules Durand"
            ),
            p(cls := "mb-4 text-lg font-normal text-gray-500 lg:text-xl sm:px-8 xl:px-24 dark:text-gray-400")(
                "Працюємо за розкладом TOLOKA Centre socio-culturel franco-ukrainien"
            ),
            p(cls := "mb-4 text-lg font-normal text-gray-500 lg:text-xl sm:px-8 xl:px-24 dark:text-gray-400")(
              "Користування послугами бібліотеки наразі абсолютно безкоштовне."
            ),
          p(cls := "mb-4 text-lg font-normal text-gray-500 lg:text-xl sm:px-8 xl:px-24 dark:text-gray-400")(
            "\uD83D\uDC99\uD83D\uDC9B Бібліотека поповнюється завдяки волонтерам та небайдужим людям. Не вагайтеся приносити книги, якими хочете поділитися!"
          )
        )
    )

  override def initCmd: Cmd[IO, Msg] = Cmd.None

  override def update(msg: Msg): (Page, Cmd[IO, Msg]) = ???
}