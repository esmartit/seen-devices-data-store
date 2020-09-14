package com.esmartit.seendevicesdatastore.v2.application.scanapi.minute

import com.esmartit.seendevicesdatastore.domain.incomingevents.DeviceLocation
import com.esmartit.seendevicesdatastore.domain.incomingevents.DeviceSeen
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import com.esmartit.seendevicesdatastore.v1.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.v1.application.radius.registered.RegisteredInfo
import com.esmartit.seendevicesdatastore.v1.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.v1.application.sensorsettings.SensorSetting
import com.esmartit.seendevicesdatastore.v1.application.sensorsettings.SensorSettingRepository
import com.esmartit.seendevicesdatastore.v1.application.uniquedevices.UniqueDevice
import com.esmartit.seendevicesdatastore.v1.application.uniquedevices.UniqueDeviceRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.daily.DailyScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.daily.DailyScanApiRepository
import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.scanapi.hourly.HourlyScanApiRepository
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class ScanApiConsumer(
    private val repository: ScanApiRepository,
    private val sensorSettingRepository: SensorSettingRepository,
    private val radiusActivityRepository: RadiusActivityRepository,
    private val registeredUserRepository: RegisteredUserRepository,
    private val hourlyScanApiRepository: HourlyScanApiRepository,
    private val dailyScanApiRepository: DailyScanApiRepository,
    private val uniqueDeviceRepository: UniqueDeviceRepository,
    private val clock: Clock
) {

    @StreamListener(Sink.INPUT)
    fun handle(event: SensorActivityEvent) {

        saveUniqueDevice(event)

        val sensorSetting = sensorSettingRepository.findByApMac(event.apMac)

        val clientMac = event.device.clientMac
        val seenTime = event.device.seenTime
        val clientMacNormalized = clientMac.replace(":", "").toLowerCase()
        val radiusActivity = radiusActivityRepository.findLastByClientMac(clientMacNormalized, PageRequest.of(0, 1))
        val registeredInfo =
            radiusActivity.firstOrNull()?.info?.username?.let { registeredUserRepository.findByInfoUsername(it)?.info }

        val scanApiEvent = event.toScanApiActivity(sensorSetting, registeredInfo)
        val maybeScanApi = repository.findByDeviceClientMacAndSeenTime(
            scanApiEvent.device.clientMac,
            scanApiEvent.seenTime
        )
        val existingRSSI = maybeScanApi?.rssi ?: -1000
        var newScanApiEvent = scanApiEvent.copy(id = maybeScanApi?.id)
        if (scanApiEvent.rssi > existingRSSI) {
            newScanApiEvent = repository.save(newScanApiEvent)
        }

        val hourlyScanApiActivity = saveHourlyActivity(seenTime, clientMac, newScanApiEvent)

        saveDailyActivity(seenTime, clientMac, hourlyScanApiActivity)
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

    private fun saveUniqueDevice(event: SensorActivityEvent) {
        val uniqueDevice = uniqueDeviceRepository.findByClientMac(event.device.clientMac)
            ?: UniqueDevice(clientMac = event.device.clientMac, created = clock.instant())
        if (uniqueDevice.id.isNullOrBlank()) {
            uniqueDeviceRepository.save(uniqueDevice)
        }
    }
}

private fun SensorActivityEvent.toScanApiActivity(
    sensorSetting: SensorSetting?,
    userInfo: RegisteredInfo?
): ScanApiActivity {
    return ScanApiActivity(
        apMac = apMac,
        ssid = device.ssid,
        location = device.location.toLocation(),
        seenTime = device.seenTime.truncatedTo(ChronoUnit.MINUTES),
        seenEpoch = device.seenEpoch,
        rssi = device.rssi,
        device = device.toDevice(),
        apFloors = apFloors,
        sensorSetting = sensorSetting,
        userInfo = userInfo
    )
}

private fun DeviceSeen.toDevice(): Device {
    return Device(
        clientMac = clientMac,
        manufacturer = manufacturer,
        os = os,
        ipv6 = ipv6,
        ipv4 = ipv4
    )
}

private fun DeviceLocation.toLocation(): Location {
    return Location(
        lat = lat,
        lng = lng,
        y = y,
        x = x,
        unc = unc
    )
}
