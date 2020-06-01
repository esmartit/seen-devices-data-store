package com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly

import com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly.HourlyDeviceCountCountInput.Companion.HOURLY_DEVICE_COUNT_INPUT
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel
import reactor.core.publisher.Mono
import java.time.Instant

@EnableBinding(HourlyDeviceCountCountInput::class)
class HourlyDeviceCountConsumer(
    private val repo: HourlyDeviceCountRepository,
    private val reactiveRepo: HourlyDeviceCountTailableRepository
) {

    @StreamListener(HOURLY_DEVICE_COUNT_INPUT)
    fun handle(count: HourlyDevicePresenceStat) {
        with(count) {
            repo.findByTime(count.time).switchIfEmpty(Mono.just(HourlyDeviceCount()))
                .map { it.copy(time = time, inCount = inCount, limitCount = limitCount, outCount = outCount) }
                .flatMap { repo.save(it) }
                .block()
            reactiveRepo.save(
                HourlyDeviceCountTailable(
                    null,
                    time,
                    inCount,
                    limitCount,
                    outCount
                )
            )
                .block()
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