package com.toloka.cho.admin.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class AppConfig(
    postgresConfig: PostgressConfig,
    emberConfig: EmberConfig
) derives ConfigReader
