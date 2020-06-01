package com.esmartit.seendevicesdatastore.application.incomingevents

import com.esmartit.seendevicesdatastore.application.incomingevents.DeviceSeenEvent
import com.esmartit.seendevicesdatastore.application.sensoractivity.AccessPoint
import com.esmartit.seendevicesdatastore.application.sensoractivity.Device
import com.esmartit.seendevicesdatastore.application.sensoractivity.Location
import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivity
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

@Service
class EventToSensorActivity {

    fun convertToSensorActivity(deviceSeenEvent: DeviceSeenEvent): SensorActivity {

        return SensorActivity(
            accessPoint = createAccessPoint(deviceSeenEvent),
            device = createDevice(deviceSeenEvent),
            rssi = deviceSeenEvent.device.rssi,
            seenTime = deviceSeenEvent.device.seenTime.truncatedTo(ChronoUnit.HOURS),
            location = createLocation(deviceSeenEvent)
        )
    }

    private fun createDevice(it: DeviceSeenEvent) =
        Device(
            macAddress = it.device.clientMac,
            ipv4 = it.device.ipv4,
            ipv6 = it.device.ipv6,
            os = it.device.os,
            manufacturer = it.device.manufacturer
        )

    private fun createAccessPoint(it: DeviceSeenEvent) =
        AccessPoint(
            macAddress = it.apMac,
            groupName = it.groupName,
            sensorName = it.sensorName,
            spotId = it.spotId,
            hotSpot = it.hotSpot,
            floors = it.apFloors
        )

    private fun createLocation(it: DeviceSeenEvent) = with(it.device.location) {
        Location(
            position = listOf(
                lat,
                lng
            ), unc = unc
        )
    }
}