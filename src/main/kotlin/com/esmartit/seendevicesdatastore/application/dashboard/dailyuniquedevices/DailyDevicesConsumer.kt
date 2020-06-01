package com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices

import com.esmartit.seendevicesdatastore.application.dashboard.dailyuniquedevices.DailyDevicesInput.Companion.DAILY_DEVICES_INPUT
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(DailyDevicesInput::class)
class DailyDevicesConsumer(private val repository: DailyDevicesRepository) {

    @StreamListener(DAILY_DEVICES_INPUT)
    fun handle(event: DailyDevices) {
        repository.save(event)
    }
}

interface DailyDevicesInput {
    @Input(DAILY_DEVICES_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val DAILY_DEVICES_INPUT = "smartpoke-daily-unique-devices-detected-count-input"
    }
}
