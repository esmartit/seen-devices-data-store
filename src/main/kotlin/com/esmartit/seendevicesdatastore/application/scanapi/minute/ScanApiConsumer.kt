package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivityRepository
import com.esmartit.seendevicesdatastore.domain.DeviceLocation
import com.esmartit.seendevicesdatastore.domain.SensorActivity
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class ScanApiConsumer(
    private val sensorActivityRepository: SensorActivityRepository
) {

    @StreamListener(Sink.INPUT)
    fun handle(event: SensorActivityEvent) {
        sensorActivityRepository.save(event.toSensorActivity())
    }
}

private fun SensorActivityEvent.toSensorActivity(): SensorActivity {
    return SensorActivity(
        id = "${device.clientMac};${device.seenTime.epochSecond}",
        clientMac = device.clientMac,
        seenTime = device.seenTime,
        apMac = apMac,
        rssi = device.rssi,
        ssid = device.ssid,
        manufacturer = device.manufacturer,
        os = device.os,
        ipv4 = device.ipv4,
        ipv6 = device.ipv6,
        location = device.location.let { DeviceLocation(it.lat, it.lng, it.unc, it.x, it.y) },
        apFloors = apFloors
    )
}

