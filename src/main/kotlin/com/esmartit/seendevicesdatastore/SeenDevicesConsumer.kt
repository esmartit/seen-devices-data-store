package com.esmartit.seendevicesdatastore

import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink

@EnableBinding(Sink::class)
class SeenDevicesConsumer(private val repository: DeviceStatRepository) {
    private val logger = LoggerFactory.getLogger(SeenDevicesConsumer::class.java)
    @StreamListener(Sink.INPUT)
    fun handle(seenDevice: DeviceSeenEvent) {
        logger.info(seenDevice.toString())
    }
}