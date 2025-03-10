package com.toloka.cho.admin.config

import pureconfig.ConfigSource
import pureconfig.ConfigReader
import cats.MonadError
import cats.MonadThrow
import cats.implicits.*
import cats.effect.*
import pureconfig.error.ConfigReaderException
import scala.reflect.ClassTag

object syntax {
  extension (sourse: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: Concurrent[F], tag: ClassTag[A]): F[A] = 
        F.pure(sourse.load[A]).flatMap {
            case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
            case Right(value) => F.pure(value)
        }
}
