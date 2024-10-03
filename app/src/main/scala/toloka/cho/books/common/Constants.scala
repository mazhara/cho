package toloka.cho.books.common
import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {

  @js.native
  @JSImport("/static/img/logoproject.jpg", JSImport.Default)
  val logoImage: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val defaultPageSize = 20

  object endpoints {
    val root = "http://localhost:4041"
    val signup = s"$root/api/auth/users"
    val login = s"$root/api/auth/login"
    val logout = s"$root/api/auth/logout"
    val checkToken     = s"$root/api/auth/checkToken"
    val forgotPassword = s"$root/api/auth/reset"
    val resetPassword  = s"$root/api/auth/recover"
    val changePassword = s"$root/api/auth/users/password"
    val postBook        = s"$root/api/books/create"
    val books           = s"$root/api/books"
    val getFilters     = s"$root/api/books/filters"
  }

  object cookies {
    val duration = 10 * 24 * 3600 * 1000
    val email = "email"
    val token = "token"
  }
}
