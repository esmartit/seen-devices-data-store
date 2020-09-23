package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.application.scanapi.hourly.HourlyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSetting
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSettingRepository
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDevice
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDeviceReactiveRepository
import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.HourlyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.RegisteredInfo
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
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
    private val sensorSettingRepository: SensorSettingRepository,
    private val radiusActivityRepository: RadiusActivityRepository,
    private val registeredUserRepository: RegisteredUserRepository,
    private val hourlyScanApiRepository: HourlyScanApiReactiveRepository,
    private val dailyScanApiRepository: DailyScanApiReactiveRepository,
    private val uniqueDeviceRepository: UniqueDeviceReactiveRepository,
    private val clock: Clock
) {

    fun save(newScanApiEvent: ScanApiActivity): Mono<UniqueDevice> {
        return saveHourlyActivity(newScanApiEvent)
            .flatMap { saveDailyActivity(it) }
            .flatMap { saveUniqueDevice(newScanApiEvent) }
            .onErrorResume(DuplicateKeyException::class.java) {
                Mono.just(UniqueDevice(clientMac = newScanApiEvent.clientMac, created = clock.instant()))
            }
    }

    fun createScanApiActivity(event: SensorActivityEvent): Mono<ScanApiActivity> {
        val sensorSetting = sensorSettingRepository.findByApMac(event.apMac)

        val clientMac = event.device.clientMac
        val clientMacNormalized = clientMac.replace(":", "").toLowerCase()
        val radiusActivity = radiusActivityRepository.findLastByClientMac(clientMacNormalized, PageRequest.of(0, 1))
        val registeredInfo =
            radiusActivity.firstOrNull()?.info?.username?.let { registeredUserRepository.findByInfoUsername(it)?.info }

        val scanApiEvent = event.toScanApiActivity(clock, sensorSetting, registeredInfo)
        return repository.findByClientMacAndSeenTime(scanApiEvent.clientMac, scanApiEvent.seenTime)
            .defaultIfEmpty(scanApiEvent)
            .flatMap {
                when {
                    it.id.isNullOrBlank() -> {
                        repository.save(it)
                    }
                    scanApiEvent.rssi > it.rssi -> {
                        repository.save(scanApiEvent.copy(id = it.id))
                    }
                    else -> {
                        Mono.empty()
                    }
                }
            }.onErrorResume(DuplicateKeyException::class.java) { Mono.empty() }
    }

    private fun saveHourlyActivity(newScanApiEvent: ScanApiActivity): Mono<HourlyScanApiActivity> {
        val clientMac = newScanApiEvent.clientMac
        val seenTimeHour = newScanApiEvent.seenTime.truncatedTo(ChronoUnit.HOURS)
        return hourlyScanApiRepository.findByClientMacAndSeenTime(clientMac, seenTimeHour)
            .defaultIfEmpty(HourlyScanApiActivity(clientMac = clientMac, seenTime = seenTimeHour))
            .flatMap { hourlyScanApiRepository.save(it.addActivity(newScanApiEvent)) }
    }

    private fun saveDailyActivity(hourlyScanApiActivity: HourlyScanApiActivity): Mono<DailyScanApiActivity> {
        val clientMac = hourlyScanApiActivity.clientMac
        val seenTimeDay = hourlyScanApiActivity.seenTime.truncatedTo(ChronoUnit.DAYS)
        return dailyScanApiRepository.findByClientMacAndSeenTime(clientMac, seenTimeDay)
            .defaultIfEmpty(DailyScanApiActivity(clientMac = clientMac, seenTime = seenTimeDay))
            .flatMap { dailyScanApiRepository.save(it.addActivity(hourlyScanApiActivity)) }
    }

    private fun saveUniqueDevice(event: ScanApiActivity): Mono<UniqueDevice> {
        return uniqueDeviceRepository.findByClientMac(event.clientMac)
            .switchIfEmpty(
                uniqueDeviceRepository.save(UniqueDevice(clientMac = event.clientMac, created = clock.instant()))
            )
    }
}

private fun SensorActivityEvent.toScanApiActivity(
    clock: Clock,
    sensorSetting: SensorSetting?,
    userInfo: RegisteredInfo?
): ScanApiActivity {
    return ScanApiActivity(
        clientMac = device.clientMac,
        seenTime = device.seenTime.truncatedTo(ChronoUnit.MINUTES),
        age = userInfo?.dateOfBirth?.let { clock.instant().atZone(ZoneOffset.UTC).year - it.year } ?: 1900,
        gender = userInfo?.gender,
        brand = device.manufacturer,
        status = sensorSetting?.presence(device.rssi) ?: Position.NO_POSITION,
        memberShip = userInfo?.memberShip,
        spotId = sensorSetting?.tags?.get("spot_id"),
        sensorId = sensorSetting?.tags?.get("sensorname"),
        countryId = sensorSetting?.tags?.get("country"),
        stateId = sensorSetting?.tags?.get("state"),
        cityId = sensorSetting?.tags?.get("city"),
        zipCode = sensorSetting?.tags?.get("zipcode"),
        groupName = sensorSetting?.tags?.get("groupname"),
        hotspot = sensorSetting?.tags?.get("hotspot"),
        zone = sensorSetting?.tags?.get("zone"),
        isConnected = !device.ssid.isNullOrBlank(),
        username = userInfo?.username,
        rssi = device.rssi
    )
}
