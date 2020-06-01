package com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(UniqueDevicesDetectedCountInput::class)
class UniqueDevicesDetectedCountConsumer(private val repository: UniqueDevicesDetectedCountRepository) {

    @StreamListener(UniqueDevicesDetectedCountInput.UNIQUE_DEVICES_DETECTED_COUNT_INPUT)
    fun handle(count: UniqueDevicesDetectedCount) {
        repository.save(count)
    }
}

interface UniqueDevicesDetectedCountInput {
    @Input(UNIQUE_DEVICES_DETECTED_COUNT_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val UNIQUE_DEVICES_DETECTED_COUNT_INPUT = "unique-devices-detected-count-input"
    }
}
