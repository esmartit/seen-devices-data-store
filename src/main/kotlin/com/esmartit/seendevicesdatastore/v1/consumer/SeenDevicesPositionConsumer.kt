package com.esmartit.seendevicesdatastore.v1.consumer

import com.esmartit.seendevicesdatastore.v1.application.incomingevents.DeviceWithPresenceEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(SeenDevicesPositionInput::class)
class SeenDevicesPositionConsumer(
    private val seenDevicesPositionService: SeenDevicesPositionService
) {

    @StreamListener(SeenDevicesPositionInput.DEVICE_POSITION_INPUT)
    fun handle(event: DeviceWithPresenceEvent) {
        seenDevicesPositionService.handle(event)
    }
}

interface SeenDevicesPositionInput {
    @Input(DEVICE_POSITION_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val DEVICE_POSITION_INPUT = "device-position-input"
    }
}
