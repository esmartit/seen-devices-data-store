package com.esmartit.seendevicesdatastore

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import java.time.temporal.ChronoUnit

@EnableBinding(Sink::class)
class SeenDevicesConsumer(private val repository: DeviceStatRepository) {

    @StreamListener(Sink.INPUT)
    fun handle(seenDevice: DeviceSeenEvent) {

        val incomingSensorActivity = createSensorActivity(seenDevice)
        val existingSensorActivity = repository.findByDeviceMacAddressAndSeenTime(
            incomingSensorActivity.device.macAddress,
            incomingSensorActivity.seenTime
        )
        val existingRSSI = existingSensorActivity?.rssi ?: -1000

        if (incomingSensorActivity.rssi > existingRSSI) {
            repository.save(incomingSensorActivity.copy(id = existingSensorActivity?.id))
        }
    }

    private fun createSensorActivity(it: DeviceSeenEvent): SensorActivity {
        return SensorActivity(
            accessPoint = createAccessPoint(it),
            device = createDevice(it),
            rssi = it.device.rssi -95,
            seenTime = it.device.seenTime.truncatedTo(ChronoUnit.HOURS),
            location = createLocation(it)
        )
    }

    private fun createDevice(it: DeviceSeenEvent) = Device(
        macAddress = it.device.clientMac,
        ipv4 = it.device.ipv4,
        ipv6 = it.device.ipv6,
        os = it.device.os,
        manufacturer = it.device.manufacturer
    )

    private fun createAccessPoint(it: DeviceSeenEvent) = AccessPoint(
        macAddress = it.apMac,
        groupName = it.groupName,
        sensorName = it.sensorName,
        spotId = it.spotId,
        hotSpot = it.hotSpot,
        floors = it.apFloors
    )

    private fun createLocation(it: DeviceSeenEvent) = with(it.device.location) {
        Location(position = listOf(lat, lng), unc = unc)
    }
}