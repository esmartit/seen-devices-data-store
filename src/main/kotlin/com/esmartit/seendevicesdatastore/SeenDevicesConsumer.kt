package com.esmartit.seendevicesdatastore

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@EnableBinding(Sink::class)
class SeenDevicesConsumer(private val repository: DeviceStatRepository) {
//    private val scheduler = Schedulers.newBoundedElastic(100, Int.MAX_VALUE, "bounded-consumer")
    @StreamListener(Sink.INPUT)
    fun handle(seenDevice: DeviceSeenEvent) {
        Mono.just(seenDevice)
            .map { createSensorActivity(it) }
            .flatMap { repository.save(it) }
            .block()
    }

    private fun createSensorActivity(it: DeviceSeenEvent): SensorActivity {
        return SensorActivity(
            accessPoint = createAccessPoint(it),
            device = createDevice(it),
            rssi = it.device.rssi.toString(),
            seenTime = it.device.seenTime,
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