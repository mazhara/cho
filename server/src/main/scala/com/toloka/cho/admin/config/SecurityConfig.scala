package com.toloka.cho.admin.config

import scala.concurrent.duration.FiniteDuration
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class SecurityConfig(secret: String, jwtExpiryDuration: FiniteDuration) derives ConfigReader
