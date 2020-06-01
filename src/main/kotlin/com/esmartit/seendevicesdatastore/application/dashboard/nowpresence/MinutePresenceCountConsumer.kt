package com.esmartit.seendevicesdatastore.application.dashboard.nowpresence

import com.esmartit.seendevicesdatastore.application.dashboard.nowpresence.MinutePresenceCountInput.Companion.MINUTE_DEVICE_COUNT_INPUT
import com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly.HourlyDevicePresenceStat
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(MinutePresenceCountInput::class)
class MinutePresenceCountConsumer(
    private val reactiveRepo: MinutePresenceCountRepository
) {

    @StreamListener(MINUTE_DEVICE_COUNT_INPUT)
    fun handle(count: HourlyDevicePresenceStat) {
        reactiveRepo.save(count.minutePresenceCountTailable()).block()
    }

    private fun HourlyDevicePresenceStat.minutePresenceCountTailable(): MinutePresenceCountTailable {
        return MinutePresenceCountTailable(
            null,
            time,
            inCount,
            limitCount,
            outCount
        )
    }
}

interface MinutePresenceCountInput {
    @Input(MINUTE_DEVICE_COUNT_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val MINUTE_DEVICE_COUNT_INPUT = "minute-device-presence-count-input"
    }
}