package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.consumer.DailyUniqueDevicesCountInput.Companion.DAILY_UNIQUE_DEVICES_DETECTED_COUNT_INPUT
import com.esmartit.seendevicesdatastore.repository.DailyUniqueDevicesDetectedCount
import com.esmartit.seendevicesdatastore.repository.DailyUniqueDevicesDetectedCountRepository
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(DailyUniqueDevicesCountInput::class)
class DailyUniqueDevicesCountConsumer(private val repository: DailyUniqueDevicesDetectedCountRepository) {

    @StreamListener(DAILY_UNIQUE_DEVICES_DETECTED_COUNT_INPUT)
    fun handle(count: DailyUniqueDevicesDetectedCount) {
        repository.save(count)
    }
}

interface DailyUniqueDevicesCountInput {
    @Input(DAILY_UNIQUE_DEVICES_DETECTED_COUNT_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val DAILY_UNIQUE_DEVICES_DETECTED_COUNT_INPUT = "smartpoke-daily-unique-devices-detected-count-input"
    }
}