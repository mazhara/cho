package com.toloka.cho.server.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.FiniteDuration

final case class TokenConfig(tokenDuration: FiniteDuration) derives ConfigReader
