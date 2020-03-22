package com.esmartit.seendevicesdatastore

import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload

@EnableBinding(Sink::class)
class SeenDevicesConsumer {

    private val logger = LoggerFactory.getLogger(SeenDevicesConsumer::class.java)
    @StreamListener(Sink.INPUT)
    fun handle(
        @Payload observation: DeviceSeenEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) part: String,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) key: ByteArray
    ) {
        logger.info("Received: $observation")
    }
}