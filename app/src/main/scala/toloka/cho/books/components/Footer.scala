package toloka.cho.books.components

import toloka.cho.books.App
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js


object Footer {

  def view(): Html[App.Msg] =
    footer(`class` := "bg-white dark:bg-gray-900")(
      div(`class` := "mx-auto w-full max-w-screen-xl px-4 lg:px-6 py-6 lg:py-8")(
        div(`class` := "md:flex md:justify-between")(
          div(`class` := "mb-6 md:mb-0")(
            a(
              href := "https://toloka.fr/",
              `class` := "flex items-center"
            )
            ( //fixme
              img(src := "assets/static/img/toloka.png", `class` := "h-8 me-3", alt := "Toloka Logo"),
            )
          ),
          div(`class` := "grid grid-cols-2 gap-8 sm:gap-6 sm:grid-cols-3")(
            // Follow Us
            div()(
              h2(`class` := "mb-6 text-sm font-semibold text-gray-900 uppercase dark:text-white")("Follow us"),
              ul(`class` := "text-gray-500 dark:text-gray-400 font-medium")(
                li(`class` := "mb-4")(a(href := "https://www.instagram.com/chobiblioteque/", `class` := "hover:underline")("Instagram")),
              )
            )
          )
        ),
        hr(`class` := "my-6 border-gray-200 sm:mx-auto dark:border-gray-700 lg:my-8"),
        div(`class` := "sm:flex sm:items-center sm:justify-between")(
          span(`class` := "text-sm text-gray-500 sm:text-center dark:text-gray-400")(
            text("© 2025 "),
            a(href := "https://toloka.fr/", `class` := "hover:underline")("Toloka"),
            text(". All Rights Reserved.")
          )
        )
      )
    )
}
