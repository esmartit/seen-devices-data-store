package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.application.incomingevents.DeviceWithPresenceEvent
import com.esmartit.seendevicesdatastore.repository.DevicePositionRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position
import com.esmartit.seendevicesdatastore.application.incomingevents.EventToSensorActivity
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(SeenDevicesPositionInput::class)
class SeenDevicesPositionConsumer(
    private val eventToSensorActivity: EventToSensorActivity,
    private val repository: DevicePositionRepository
) {

    @StreamListener(SeenDevicesPositionInput.DEVICE_POSITION_INPUT)
    fun handle(event: DeviceWithPresenceEvent) {

        val incomingSensorActivity = eventToSensorActivity.convertToSensorActivity(event.deviceDetectedEvent)
        val existingSensorActivity = repository.findByMacAddressAndSeenTime(
            incomingSensorActivity.device.macAddress,
            incomingSensorActivity.seenTime
        )
        val existingRSSI = existingSensorActivity?.position ?: Position.NO_POSITION

        if (event.position.value > existingRSSI.value) {
            repository.save(
                DeviceWithPosition(
                    id = existingSensorActivity?.id,
                    macAddress = incomingSensorActivity.device.macAddress,
                    position = event.position,
                    seenTime = incomingSensorActivity.seenTime
                )
            )
        }
    }
}

interface SeenDevicesPositionInput {
    @Input(DEVICE_POSITION_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val DEVICE_POSITION_INPUT = "device-position-input"
    }
}
