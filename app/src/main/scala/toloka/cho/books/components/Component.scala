package toloka.cho.books.components

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

trait Component[Msg, +Model] {
  def initCmd: Cmd[IO, Msg]
  def update(msg: Msg): (Model, Cmd[IO, Msg])
  def view(): Html[Msg]
}
