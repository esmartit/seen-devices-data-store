package com.esmartit.seendevicesdatastore.application.dashboard.registered.total

import com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices.TotalDevicesInput.Companion.TOTAL_DEVICES_INPUT
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(TotalRegisteredInput::class)
class TotalRegisteredConsumer(private val repository: TotalRegisteredRepository) {

    @StreamListener(TOTAL_DEVICES_INPUT)
    fun handle(count: TotalRegistered) {
        repository.save(count)
    }
}

interface TotalRegisteredInput {
    @Input(TOTAL_REGISTERED_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val TOTAL_REGISTERED_INPUT = "total-registered-input"
    }
}
