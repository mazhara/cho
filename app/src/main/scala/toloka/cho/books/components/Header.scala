package toloka.cho.books.components

import toloka.cho.books.App
import toloka.cho.books.common.Constants
import tyrian.*
import tyrian.Html.*

object Header {

  def view(): Html[App.Msg] = header()(
    nav(
      cls := "bg-white border-gray-100 dark:bg-gray-900"
    )(
      div(
        cls := "flex justify-between items-center mx-auto max-w-screen-xl px-4 lg:px-6 py-2.5"
      )(
        a(
          href := "https://www.sholib.com/",
          cls := "flex items-center"
        )(
          img(
            src := Constants.choLogo,
            cls := "logo",
            alt := "Cho Logo"
          ),
          span(
            cls := "self-center text-xl font-semibold whitespace-nowrap dark:text-white"
          )("CHO")
        ),
        div(
          cls := "flex items-center w-auto",
          id := "menu"
        )(
          ul(
            cls := "flex flex-row font-medium space-x-8 m-0"
          )(
            li()(a(
              href := "#",
              cls := "block py-2 pr-4 pl-3 text-gray-700 hover:bg-gray-50 lg:hover:bg-transparent lg:border-0 lg:hover:text-primary-700 lg:p-0 dark:text-gray-400 lg:dark:hover:text-white dark:hover:bg-gray-700 dark:hover:text-white lg:dark:hover:bg-transparent"
            )("Home")),
            li()(a(
              href := "#/books",
              cls := "block py-2 pr-4 pl-3 text-gray-700 hover:bg-gray-50 lg:hover:bg-transparent lg:border-0 lg:hover:text-primary-700 lg:p-0 dark:text-gray-400 lg:dark:hover:text-white dark:hover:bg-gray-700 dark:hover:text-white lg:dark:hover:bg-transparent"
            )("Books"))
          )
        )
      )
    )
  )
}