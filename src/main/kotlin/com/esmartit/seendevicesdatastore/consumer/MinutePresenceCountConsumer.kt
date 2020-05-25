package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.consumer.MinutePresenceCountInput.Companion.MINUTE_DEVICE_COUNT_INPUT
import com.esmartit.seendevicesdatastore.repository.MinutePresenceCountRepository
import com.esmartit.seendevicesdatastore.repository.MinutePresenceCountTailable
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

//@EnableBinding(MinutePresenceCountInput::class)
class MinutePresenceCountConsumer(
    private val reactiveRepo: MinutePresenceCountRepository
) {

//    @StreamListener(MINUTE_DEVICE_COUNT_INPUT)
    fun handle(count: HourlyDevicePresenceStat) {
        with(count) {
            println(count)
            reactiveRepo.save(MinutePresenceCountTailable(null, time, inCount, limitCount, outCount))
                .block()
        }
    }
}

interface MinutePresenceCountInput {
    @Input(MINUTE_DEVICE_COUNT_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val MINUTE_DEVICE_COUNT_INPUT = "minute-device-presence-count-input"
    }
}