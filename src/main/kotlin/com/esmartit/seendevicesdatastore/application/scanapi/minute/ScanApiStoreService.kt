package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.application.scanapi.daily.DailyScanApiRepository
import com.esmartit.seendevicesdatastore.application.scanapi.hourly.HourlyScanApiRepository
import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivityRepository
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSetting
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSettingRepository
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDevice
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDeviceRepository
import com.esmartit.seendevicesdatastore.domain.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.HourlyScanApiActivity
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.RegisteredInfo
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.SensorActivity
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Component
class ScanApiStoreService(
    private val repository: ScanApiRepository,
    private val sensorSettingRepository: SensorSettingRepository,
    private val radiusActivityRepository: RadiusActivityRepository,
    private val registeredUserRepository: RegisteredUserRepository,
    private val hourlyScanApiRepository: HourlyScanApiRepository,
    private val dailyScanApiRepository: DailyScanApiRepository,
    private val uniqueDeviceRepository: UniqueDeviceRepository,
    private val sensorActivityRepository: SensorActivityRepository,
    private val clock: Clock
) {

    fun save(event: SensorActivity) {
        val sensorSetting = sensorSettingRepository.findByApMac(event.apMac)

        val clientMac = event.device.clientMac
        val seenTime = event.device.seenTime
        val clientMacNormalized = clientMac.replace(":", "").toLowerCase()
        val radiusActivity = radiusActivityRepository.findLastByClientMac(clientMacNormalized, PageRequest.of(0, 1))
        val registeredInfo =
            radiusActivity.firstOrNull()?.info?.username?.let { registeredUserRepository.findByInfoUsername(it)?.info }

        val scanApiEvent = event.toScanApiActivity(clock, sensorSetting, registeredInfo)
        val maybeScanApi = repository.findByClientMacAndSeenTime(
            scanApiEvent.clientMac,
            scanApiEvent.seenTime
        )
        val existingRSSI = maybeScanApi?.rssi ?: -1000
        var newScanApiEvent = scanApiEvent.copy(id = maybeScanApi?.id)
        if (scanApiEvent.rssi > existingRSSI) {
            newScanApiEvent = repository.save(newScanApiEvent)
        }

        val hourlyScanApiActivity = saveHourlyActivity(seenTime, clientMac, newScanApiEvent)

        saveDailyActivity(seenTime, clientMac, hourlyScanApiActivity)

        saveUniqueDevice(event)

        val existingActivity = sensorActivityRepository.findByDeviceClientMacAndDeviceSeenTime(clientMac, seenTime)
        if (existingActivity.isEmpty()) {
            sensorActivityRepository.save(event)
        }
    }

    private fun saveHourlyActivity(
        seenTime: Instant,
        clientMac: String,
        newScanApiEvent: ScanApiActivity
    ): HourlyScanApiActivity {
        val seenTimeHour = seenTime.truncatedTo(ChronoUnit.HOURS)
        var hourlyScanApiActivity =
            hourlyScanApiRepository.findByClientMacAndSeenTime(clientMac, seenTimeHour) ?: HourlyScanApiActivity(
                clientMac = clientMac,
                seenTime = seenTimeHour
            )
        val newAct = hourlyScanApiActivity.activity.toMutableSet()
            .also { s -> s.removeIf { it.seenTime == newScanApiEvent.seenTime } }
            .also { it.add(newScanApiEvent) }
        hourlyScanApiActivity = hourlyScanApiRepository.save(hourlyScanApiActivity.copy(activity = newAct))
        return hourlyScanApiActivity
    }

    private fun saveDailyActivity(
        seenTime: Instant,
        clientMac: String,
        hourlyScanApiActivity: HourlyScanApiActivity
    ) {
        val seenTimeDay = seenTime.truncatedTo(ChronoUnit.DAYS)
        val dailyScanApiActivity =
            dailyScanApiRepository.findByClientMacAndSeenTime(clientMac, seenTimeDay) ?: DailyScanApiActivity(
                clientMac = clientMac,
                seenTime = seenTimeDay
            )
        val newDayAct = dailyScanApiActivity.activity.toMutableSet()
            .also { s -> s.removeIf { it.seenTime == hourlyScanApiActivity.seenTime } }
            .also { it.add(hourlyScanApiActivity) }
        dailyScanApiRepository.save(dailyScanApiActivity.copy(activity = newDayAct))
    }

    private fun saveUniqueDevice(event: SensorActivity) {
        val uniqueDevice = uniqueDeviceRepository.findByClientMac(event.device.clientMac)
            ?: UniqueDevice(clientMac = event.device.clientMac, created = clock.instant())
        if (uniqueDevice.id.isNullOrBlank()) {
            uniqueDeviceRepository.save(uniqueDevice)
        }
    }
}

private fun SensorActivity.toScanApiActivity(
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
