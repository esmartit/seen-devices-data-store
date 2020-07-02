package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.application.incomingevents.DeviceWithPresenceEvent
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(SeenDevicesPositionInput::class)
class SeenDevicesPositionConsumer(
    private val seenDevicesPositionService: SeenDevicesPositionService
) {

    private val logger = LoggerFactory.getLogger(SeenDevicesPositionConsumer::class.java)

    @StreamListener(SeenDevicesPositionInput.DEVICE_POSITION_INPUT)
    fun handle(event: DeviceWithPresenceEvent) {
        seenDevicesPositionService.handle(event)
        logger.info("Event Consumed: $event")
    }
}

interface SeenDevicesPositionInput {
    @Input(DEVICE_POSITION_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val DEVICE_POSITION_INPUT = "device-position-input"
    }
}
