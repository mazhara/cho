package com.toloka.cho.server.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class AppConfig (
    postgresConfig: PostgressConfig,
    emberConfig: EmberConfig,
    securityConfig: SecurityConfig,
    tokenConfig: TokenConfig,
    emailServiceConfig: EmailServiceConfig
) derives ConfigReader
