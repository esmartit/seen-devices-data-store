package com.esmartit.seendevicesdatastore.infrastructure

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Configuration
class TimeConfig {

    @Bean
    @Profile("dev")
    fun devClock(): Clock {
        return Clock.fixed(Instant.parse("2020-03-13T20:36:31Z"), ZoneOffset.UTC)
    }

    @Bean
    @Profile("!dev")
    fun prodClock(): Clock {
        return Clock.systemUTC()
    }
}