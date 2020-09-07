package com.esmartit.seendevicesdatastore.v1.application.incomingevents

import com.esmartit.seendevicesdatastore.v1.application.sensoractivity.AccessPoint
import com.esmartit.seendevicesdatastore.v1.application.sensoractivity.Device
import com.esmartit.seendevicesdatastore.v1.application.sensoractivity.Location
import com.esmartit.seendevicesdatastore.v1.application.sensoractivity.SensorActivity
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

@Service
class EventToSensorActivity {

    fun convertToSensorActivity(sensorActivityEvent: SensorActivityEvent): SensorActivity {

        return SensorActivity(
            accessPoint = createAccessPoint(sensorActivityEvent),
            device = createDevice(sensorActivityEvent),
            rssi = sensorActivityEvent.device.rssi,
            seenTime = sensorActivityEvent.device.seenTime.truncatedTo(ChronoUnit.HOURS),
            location = createLocation(sensorActivityEvent),
            lastUpdate = sensorActivityEvent.device.seenTime,
            ssid = sensorActivityEvent.device.ssid
        )
    }

    private fun createDevice(it: SensorActivityEvent) =
        Device(
            macAddress = it.device.clientMac,
            ipv4 = it.device.ipv4,
            ipv6 = it.device.ipv6,
            os = it.device.os,
            manufacturer = it.device.manufacturer
        )

    private fun createAccessPoint(it: SensorActivityEvent) =
        AccessPoint(
            macAddress = it.apMac,
            groupName = it.groupName,
            sensorName = it.sensorName,
            spotId = it.spotId,
            hotSpot = it.hotSpot,
            floors = it.apFloors,
            countryLocation = it.countryLocation
        )

    private fun createLocation(it: SensorActivityEvent) = with(it.device.location) {
        Location(position = listOf(lat, lng), unc = unc)
    }
}