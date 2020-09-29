package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.application.scanapi.hourly.HourlyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDevice
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDeviceReactiveRepository
import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.HourlyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.RegisteredInfo
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Component
class ScanApiStoreService(
    private val repository: ScanApiReactiveRepository,
    private val radiusActivityRepository: RadiusActivityRepository,
    private val registeredUserRepository: RegisteredUserRepository,
    private val hourlyScanApiRepository: HourlyScanApiReactiveRepository,
    private val dailyScanApiRepository: DailyScanApiReactiveRepository,
    private val uniqueDeviceRepository: UniqueDeviceReactiveRepository,
    private val clock: Clock
) {

    fun save(newScanApiEvent: ScanApiActivity): Mono<UniqueDevice> {
        return createScanApiActivity(newScanApiEvent)
            .flatMap { saveHourlyActivity(it) }
            .flatMap { saveDailyActivity(it) }
            .flatMap { saveUniqueDevice(newScanApiEvent) }
            .onErrorResume(DuplicateKeyException::class.java) {
                Mono.just(UniqueDevice(id = newScanApiEvent.clientMac))
            }
    }

    fun createScanApiActivity(event: ScanApiActivity): Mono<ScanApiActivity> {

        val clientMac = event.clientMac
        val clientMacNormalized = clientMac.replace(":", "").toLowerCase()
        val radiusActivity = radiusActivityRepository.findLastByClientMac(clientMacNormalized, PageRequest.of(0, 1))
        val registeredInfo =
            radiusActivity.firstOrNull()?.info?.username?.let { registeredUserRepository.findByInfoUsername(it)?.info }

        val scanApiEvent = event.toScanApiActivity(clock, registeredInfo)
        return repository.save(scanApiEvent)
    }

    private fun saveHourlyActivity(newScanApiEvent: ScanApiActivity): Mono<HourlyScanApiActivity> {
        val clientMac = newScanApiEvent.clientMac
        val seenTimeHour = newScanApiEvent.seenTime.truncatedTo(ChronoUnit.HOURS)
        return hourlyScanApiRepository.findByClientMacAndSeenTime(clientMac, seenTimeHour)
            .defaultIfEmpty(
                HourlyScanApiActivity(
                    id = "$clientMac;${seenTimeHour.epochSecond}",
                    clientMac = clientMac, seenTime = seenTimeHour
                )
            )
            .flatMap { hourlyScanApiRepository.save(it.addActivity(newScanApiEvent)) }
    }

    private fun saveDailyActivity(hourlyScanApiActivity: HourlyScanApiActivity): Mono<DailyScanApiActivity> {
        val clientMac = hourlyScanApiActivity.clientMac
        val seenTimeDay = hourlyScanApiActivity.seenTime.truncatedTo(ChronoUnit.DAYS)
        return dailyScanApiRepository.findByClientMacAndSeenTime(clientMac, seenTimeDay)
            .defaultIfEmpty(
                DailyScanApiActivity(
                    id = "$clientMac;${seenTimeDay.epochSecond}",
                    clientMac = clientMac, seenTime = seenTimeDay
                )
            )
            .flatMap { dailyScanApiRepository.save(it.addActivity(hourlyScanApiActivity)) }
    }

    private fun saveUniqueDevice(event: ScanApiActivity): Mono<UniqueDevice> {
        return uniqueDeviceRepository.save(UniqueDevice(id = event.clientMac))
    }
}

private fun ScanApiActivity.toScanApiActivity(
    clock: Clock,
    userInfo: RegisteredInfo?
): ScanApiActivity {
    return this.copy(age = userInfo?.dateOfBirth?.let { clock.instant().atZone(ZoneOffset.UTC).year - it.year } ?: 1900,
        gender = userInfo?.gender,
        memberShip = userInfo?.memberShip,
        username = userInfo?.username)
}