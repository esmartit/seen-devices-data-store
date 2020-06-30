package com.esmartit.seendevicesdatastore.application.dashboard.dailyregistered

import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUser
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserReactiveRepository
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class DailyRegisteredServiceTest {

    @Test
    fun `daily registered count`() {

        val clock = Clock.fixed(Instant.parse("2020-03-13T20:36:31Z"), ZoneOffset.UTC)
        val repo = mock<RegisteredUserReactiveRepository>()
        val zoneId = ZoneOffset.UTC
        val dailyRegisteredService = DailyRegisteredService(repo, clock)
        val startOfDay = clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()
        val now = clock.instant()

        val firstRegistered =
            Flux.just(mock<RegisteredUser>(), mock<RegisteredUser>(), mock<RegisteredUser>())

        val currentRegistered =
            Flux.just(mock<RegisteredUser>(), mock<RegisteredUser>())

        whenever(repo.findByInfoSeenTimeGreaterThanEqual(startOfDay)).thenReturn(firstRegistered)
        whenever(repo.findByInfoSeenTimeGreaterThanEqual(now)).thenReturn(currentRegistered)

        dailyRegisteredService.getDailyRegisteredCount(zoneId)
            .subscribe {
                println(it)
            }
//        TimeUnit.SECONDS.sleep(30)
    }
}