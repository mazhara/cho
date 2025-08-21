package toloka.cho.books.pages

import cats.effect.IO
import com.toloka.cho.domain.book.{Book, BookFilter}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import toloka.cho.books.*
import tyrian.{Cmd, Html}
import tyrian.Html.*
import tyrian.cmds.Logger
import tyrian.http.*


final case class BooksListPage(
    jobFilter: BookFilter = BookFilter(),
    books: List[Book] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page:


  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(cls := "relative overflow-x-auto shadow-md sm:rounded-lg")(
      filterView(),
      searchInputView(),
      table(
        cls := "w-full text-sm text-left rtl:text-right text-gray-500 dark:text-gray-400"
      )(
        bookTableHeadView(),
        bookTableBodyRowView()
      )
    )


  private def bookTableBodyRowView(): Html[Nothing] =
    tbody()(
      tr(cls := "bg-white border-b dark:bg-gray-800 dark:border-gray-700 border-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600")(
        th(
          scope := "row",
          cls := "flex items-center px-6 py-4 text-gray-900 whitespace-nowrap dark:text-white"
        )(
          img(
            cls := "w-10 h-10 rounded-full",
            src := "/docs/images/people/profile-picture-1.jpg",
            alt := "Jese image"
          )
        ),
        td(cls := "px-6 py-4")("Book Title"),
        td(cls := "px-6 py-4")("Author name"),
        td(cls := "px-6 py-4")("Publisher"),
        td(cls := "px-6 py-4")("Year")
      )
    )

  private def bookTableHeadView(): Html[Nothing] =
      thead(
        cls := "text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400"
      )(
        tr()(
          th(scope := "col", cls := "px-6 py-3")("Cover"),
          th(scope := "col", cls := "px-6 py-3")("Book Title"),
          th(scope := "col", cls := "px-6 py-3")("Author"),
          th(scope := "col", cls := "px-6 py-3")("Publisher"),
          th(scope := "col", cls := "px-6 py-3")("Year")
        )
      )
  private def searchInputView(): Html[Nothing] =
    div()(
      label(
        cls := "sr-only"
      )("Search"),

      div(cls := "relative")(
        input(
          typ := "text",
          id := "table-search-users",
          placeholder := "Search for users",
          cls := "block p-2 ps-10 text-sm text-gray-900 border border-gray-300 rounded-lg w-80 bg-gray-50 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
        )
      )
    )
  private def filterView(): Html[App.Msg] =
      div(
        button(
          id := "dropdownActionButton",
          cls := "inline-flex items-center text-gray-500 bg-white border border-gray-300 focus:outline-none hover:bg-gray-100 focus:ring-4 focus:ring-gray-100 font-medium rounded-lg text-sm px-3 py-1.5 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-600 dark:hover:bg-gray-700 dark:hover:border-gray-600 dark:focus:ring-gray-700",
          typ := "button"//,
         // onClick(Msg.ToggleDropdown)
        )(
          span(cls := "sr-only")("Action button"),
          text("Action")
        ),
        div(
          id := "dropdownAction",
          //fixme
          cls := s"z-10 ${if true then "" else "hidden"} bg-white divide-y divide-gray-100 rounded-lg shadow-sm w-44 dark:bg-gray-700 dark:divide-gray-600"
        )(
          // fixme here we will have filters
          ul(
            cls := "py-1 text-sm text-gray-700 dark:text-gray-200"
          )(
            li()(
              a(
                href := "#",
                cls := "block px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-600 dark:hover:text-white"
              )("Reward")
            ),
            li()(
              a(
                href := "#",
                cls := "block px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-600 dark:hover:text-white"
              )("Promote")
            ),
            li()(
              a(
                href := "#",
                cls := "block px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-600 dark:hover:text-white"
              )("Activate account")
            )
          ),
          div(cls := "py-1")(
            a(
              href := "#",
              cls := "block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 dark:text-gray-200 dark:hover:text-white"
            )("Delete User")
          )
        )
      )
