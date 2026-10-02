package com.toloka.cho.server.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    pass: String,
    frontendUrl: String
) derives ConfigReader
