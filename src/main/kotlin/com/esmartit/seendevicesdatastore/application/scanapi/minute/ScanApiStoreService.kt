package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository
import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository.Companion.OTHERS_BRAND
import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.application.scanapi.daily.ScanApiActivityDailyRepository
import com.esmartit.seendevicesdatastore.application.scanapi.hourly.ScanApiActivityHourlyRepository
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSettingRepository
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDeviceReactiveRepository
import com.esmartit.seendevicesdatastore.domain.*
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class ScanApiStoreService(
        private val repository: ScanApiReactiveRepository,
        private val radiusActivityRepository: RadiusActivityRepository,
        private val registeredUserRepository: RegisteredUserRepository,
        private val uniqueDeviceRepository: UniqueDeviceReactiveRepository,
        private val clock: Clock,
        private val sensorSettingRepository: SensorSettingRepository,
        private val brandsRepository: BrandsRepository,
        private val scanApiActivityDailyRepository: ScanApiActivityDailyRepository,
        private val scanApiActivityHourlyRepository: ScanApiActivityHourlyRepository
) {

    fun save(event: SensorActivityEvent): Mono<UniqueDevice> {

        val scanApiActivity = event.toScanApiActivity()
        return scanApiActivity.toMono()
            .filter { !OTHERS_BRAND.name.equals(it.brand, true) }
            .filter { it -> it.status != Position.NO_POSITION }
            .flatMap { createScanApiActivity(it) }
            .doOnNext { saveScanActivityDaily(it) }
            .doOnNext { saveScanActivityHourly(it) }
            .flatMap { saveUniqueDevice(it) }
            .onErrorResume(DuplicateKeyException::class.java) {
                Mono.just(UniqueDevice(id = scanApiActivity.clientMac))
            }.defaultIfEmpty(UniqueDevice("no device"))
    }

    private fun saveScanActivityHourly(scanApiHourly: ScanApiActivity): ScanApiActivityH {
        val clientMac = scanApiHourly.clientMac

        val seenTime = scanApiHourly.seenTime
        val timeZone = "Europe/Madrid"
        val systemZone = ZoneId.of(timeZone)
        val dateWithZone = LocalDateTime.ofInstant(seenTime, ZoneId.of(timeZone))
        val dateAtZone = dateWithZone.truncatedTo(ChronoUnit.HOURS)
        val zoneOffset = systemZone.getRules().getOffset(dateWithZone)

        val spotId: String? = scanApiHourly.spotId
        val sensorId: String? = scanApiHourly.sensorId
        val status = scanApiHourly.status
        var minTime = scanApiHourly.seenTime
        var maxTime = scanApiHourly.seenTime
        var totalTime: Long = 60000

        var activityHourly: ScanApiActivityH?
        activityHourly = scanApiActivityHourlyRepository.findByClientMacAndDateAtZoneAndSpotIdAndSensorIdAndStatus(clientMac, dateAtZone, spotId, sensorId, status)

        if (activityHourly != null) {
            if (activityHourly.minTime != maxTime) {
                minTime = activityHourly.minTime
                totalTime = ChronoUnit.MILLIS.between(minTime, maxTime)
            }
        }
        val apiScanHourly = ScanApiActivityH(
                id = "$clientMac;${dateAtZone.toEpochSecond(zoneOffset)};$spotId;$sensorId;$status",
                clientMac = clientMac, dateAtZone = dateAtZone, timeZone = timeZone,
                spotId = spotId, sensorId = sensorId, status = status,
                zone = scanApiHourly.zone,
                countryId = scanApiHourly.countryId,
                stateId = scanApiHourly.stateId,
                cityId = scanApiHourly.cityId,
                zipCode = scanApiHourly.zipCode,
                brand = scanApiHourly.brand,
                username = scanApiHourly.username,
                ssid = scanApiHourly.ssid,
                isConnected = scanApiHourly.isConnected,
                age = scanApiHourly.age,
                gender = scanApiHourly.gender,
                memberShip = scanApiHourly.memberShip,
                userZipCode = scanApiHourly.userZipCode,
                minTime = minTime,
                maxTime = maxTime,
                totalTime = totalTime
        )

        return scanApiActivityHourlyRepository.save(apiScanHourly)
    }


    private fun saveScanActivityDaily(scanApiDaily: ScanApiActivity): ScanApiActivityD {
        val clientMac = scanApiDaily.clientMac

        val seenTime = scanApiDaily.seenTime
        val timeZone = "Europe/Madrid"
        val systemZone = ZoneId.of(timeZone)
        val dateWithZone = LocalDateTime.ofInstant(seenTime, ZoneId.of(timeZone))
        val dateAtZone = dateWithZone.truncatedTo(ChronoUnit.DAYS)
        val zoneOffset = systemZone.getRules().getOffset(dateWithZone)

        val spotId: String? = scanApiDaily.spotId
        val sensorId: String? = scanApiDaily.sensorId
        val status = scanApiDaily.status
        var minTime = scanApiDaily.seenTime
        var maxTime = scanApiDaily.seenTime
        var totalTime: Long = 60000

        var activityDaily: ScanApiActivityD?
        activityDaily = scanApiActivityDailyRepository.findByClientMacAndDateAtZoneAndSpotIdAndSensorIdAndStatus(clientMac, dateAtZone, spotId, sensorId, status)

        if (activityDaily != null) {
            if (activityDaily.minTime != maxTime) {
                minTime = activityDaily.minTime
                totalTime = ChronoUnit.MILLIS.between(minTime, maxTime)
            }
        }
        val apiScanDaily = ScanApiActivityD(
                id = "$clientMac;${dateAtZone.toEpochSecond(zoneOffset)};$spotId;$sensorId;$status",
                clientMac = clientMac, dateAtZone = dateAtZone, timeZone = timeZone,
                spotId = spotId, sensorId = sensorId, status = status,
                zone = scanApiDaily.zone,
                countryId = scanApiDaily.countryId,
                stateId = scanApiDaily.stateId,
                cityId = scanApiDaily.cityId,
                zipCode = scanApiDaily.zipCode,
                brand = scanApiDaily.brand,
                username = scanApiDaily.username,
                ssid = scanApiDaily.ssid,
                isConnected = scanApiDaily.isConnected,
                age = scanApiDaily.age,
                gender = scanApiDaily.gender,
                memberShip = scanApiDaily.memberShip,
                userZipCode = scanApiDaily.userZipCode,
                minTime = minTime,
                maxTime = maxTime,
                totalTime = totalTime
        )

        return scanApiActivityDailyRepository.save(apiScanDaily)
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

    private fun saveUniqueDevice(event: ScanApiActivity): Mono<UniqueDevice> {
        return uniqueDeviceRepository.save(UniqueDevice(id = event.clientMac))
    }

    private fun SensorActivityEvent.toScanApiActivity(): ScanApiActivity {
        val sensorSetting = sensorSettingRepository.findByApMac(apMac)
        val brand = brandsRepository.findByName(device.manufacturer ?: "")
        return ScanApiActivity(
                id = "${device.clientMac};${device.seenTime.epochSecond}",
                clientMac = device.clientMac,
                seenTime = device.seenTime,
                brand = brand.name,
                manufacturer = device.manufacturer,
                isConnected = !device.ssid.isNullOrBlank(),
                rssi = device.rssi,
                status = sensorSetting?.presence(device.rssi) ?: Position.NO_POSITION,
                spotId = sensorSetting?.tags?.get("spot_id"),
                sensorId = sensorSetting?.tags?.get("sensorname"),
                countryId = sensorSetting?.tags?.get("country"),
                stateId = sensorSetting?.tags?.get("state"),
                cityId = sensorSetting?.tags?.get("city"),
                zipCode = sensorSetting?.tags?.get("zipcode"),
                zone = sensorSetting?.tags?.get("zone"),
                groupName = sensorSetting?.tags?.get("groupname"),
                hotspot = sensorSetting?.tags?.get("hotspot"),
                ssid = device.ssid,
                location = device.location
        )
    }
}

private fun ScanApiActivity.toScanApiActivity(
        clock: Clock,
        userInfo: RegisteredInfo?
): ScanApiActivity {
    return this.copy(age = userInfo?.dateOfBirth?.let { clock.instant().atZone(ZoneOffset.UTC).year - it.year } ?: 1900,
            gender = userInfo?.gender,
            memberShip = userInfo?.memberShip,
            username = userInfo?.username,
            userZipCode = userInfo?.zipCode)
}

