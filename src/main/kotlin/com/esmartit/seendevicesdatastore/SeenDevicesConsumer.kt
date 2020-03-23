package com.esmartit.seendevicesdatastore

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class SeenDevicesConsumer(private val repository: DeviceStatRepository) {

    @StreamListener(Sink.INPUT)
    fun handle(seenDevice: DeviceSeenEvent) {
        repository.save(DeviceStat(seenDevice = seenDevice))
    }
}