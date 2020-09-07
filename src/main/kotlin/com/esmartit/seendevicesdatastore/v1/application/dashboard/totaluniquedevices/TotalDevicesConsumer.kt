package com.esmartit.seendevicesdatastore.v1.application.dashboard.totaluniquedevices

import com.esmartit.seendevicesdatastore.v1.application.dashboard.totaluniquedevices.TotalDevicesInput.Companion.TOTAL_DEVICES_INPUT
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(TotalDevicesInput::class)
class TotalDevicesConsumer(private val repository: TotalDevicesRepository) {

    @StreamListener(TOTAL_DEVICES_INPUT)
    fun handle(count: TotalDevices) {
        repository.save(count)
    }
}

interface TotalDevicesInput {
    @Input(TOTAL_DEVICES_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val TOTAL_DEVICES_INPUT = "unique-devices-detected-count-input"
    }
}
