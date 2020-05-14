package com.esmartit.seendevicesdatastore.consumer

import com.esmartit.seendevicesdatastore.consumer.HourlyDeviceCountCountInput.Companion.HOURLY_DEVICE_COUNT_INPUT
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCount
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountReactiveRepository
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountRepository
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountTailable
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel
import java.time.Instant

@EnableBinding(HourlyDeviceCountCountInput::class)
class HourlyDeviceCountConsumer(
    private val repo: HourlyDeviceCountRepository,
    private val reactiveRepo: HourlyDeviceCountReactiveRepository
) {

    @StreamListener(HOURLY_DEVICE_COUNT_INPUT)
    fun handle(count: HourlyDevicePresenceStat) {
        with(count) {
            reactiveRepo.save(HourlyDeviceCountTailable(null, time, inCount, limitCount, outCount))
                .subscribe()
            val findByTime = repo.findByTime(count.time) ?: HourlyDeviceCount()
            repo.save(findByTime.copy(time = time, inCount = inCount, limitCount = limitCount, outCount = outCount))
        }
    }
}

data class HourlyDevicePresenceStat(
    val time: Instant,
    val inCount: Long,
    val limitCount: Long,
    val outCount: Long
)

interface HourlyDeviceCountCountInput {
    @Input(HOURLY_DEVICE_COUNT_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val HOURLY_DEVICE_COUNT_INPUT = "hourly-device-presence-count-input"
    }
}