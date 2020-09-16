package com.esmartit.seendevicesdatastore.application.dashboard.dailyregistered

//import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserReactiveRepository
import org.junit.jupiter.api.Test

class RegisteredServiceTest {

    @Test
    fun `daily registered count`() {

       /* val clock = Clock.fixed(Instant.parse("2020-03-13T20:36:31Z"), ZoneOffset.UTC)
        val repo = mock<RegisteredUserReactiveRepository>()
        val zoneId = ZoneOffset.UTC
        val dailyRegisteredService =
            RegisteredService(
                repo,
                clock
            )
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
//        TimeUnit.SECONDS.sleep(60)*/
    }
}