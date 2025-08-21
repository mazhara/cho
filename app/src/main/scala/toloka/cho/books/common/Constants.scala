package toloka.cho.books.common

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Constants {
  @js.native
  @JSImport("/static/img/toloka.png", JSImport.Default)
  val tolokaLogo: String = js.native

  @js.native
  @JSImport("/static/img/cho.png", JSImport.Default)
  val choLogo: String = js.native
}

