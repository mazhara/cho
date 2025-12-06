package toloka.cho.books.common

import scala.scalajs.{LinkingInfo, js}
import scala.scalajs.js.annotation.JSImport
import org.scalajs.dom.window

object Constants {
  @js.native
  @JSImport("/static/img/toloka.png", JSImport.Default)
  val tolokaLogo: String = js.native

  @js.native
  @JSImport("/static/img/cho.png", JSImport.Default)
  val choLogo: String = js.native

  val defaultPageSize = 20

  object endpoints {
    val root =
      if (LinkingInfo.developmentMode) "http://localhost:4041"
      else window.location.origin

    val books = s"$root/api/books"
    val getFilters = s"$root/api/books/filters"
  }
}
