package com.esmartit.seendevicesdatastore.http

import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountReactiveRepository
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountTailable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.BiFunction

@RestController
@RequestMapping("/sensor-activity")
class HourlyDeviceCountController(private val repository: HourlyDeviceCountReactiveRepository) {

    @GetMapping(path = ["/hourly-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<HourlyDeviceCountTailable> {
        return repository.findWithTailableCursorBy()
    }

    @GetMapping(path = ["/hourly-device-presence-delta"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivityDelta(): Flux<HourlyDeviceCountTailable> {
        val stats = repository.findWithTailableCursorBy().scan(DeltaHelper(), ::calculate)
            .map { it.current.copy(time = Instant.now()) }
        val ticker = Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop()
        return Flux.combineLatest(ticker, stats, BiFunction { _: Long, b: HourlyDeviceCountTailable -> b })
            .map { zeroIfMoreThanAMinute(it) }
    }

    private fun zeroIfMoreThanAMinute(count: HourlyDeviceCountTailable): HourlyDeviceCountTailable {
        return if (Instant.now().minusSeconds(60).isBefore(count.time)) {
            count
        } else {
            HourlyDeviceCountTailable()
        }
    }

    private fun calculate(acc: DeltaHelper, current: HourlyDeviceCountTailable): DeltaHelper {
        val previous = acc.previous
        if (current.time.truncatedTo(ChronoUnit.HOURS).isAfter(previous.time.truncatedTo(ChronoUnit.HOURS))) {
            return acc.copy(previous = acc.current, current = current)
        }
        val deltaIn = (current.inCount - previous.inCount).takeIf { it > 0 } ?: 0
        val deltaLimit = (current.limitCount - previous.limitCount).takeIf { it > 0 } ?: 0
        val deltaOut = (current.outCount - previous.outCount).takeIf { it > 0 } ?: 0
        return acc.copy(
            previous = current,
            current = HourlyDeviceCountTailable(inCount = deltaIn, limitCount = deltaLimit, outCount = deltaOut)
        )
    }
}

data class DeltaHelper(
    val previous: HourlyDeviceCountTailable = HourlyDeviceCountTailable(),
    val current: HourlyDeviceCountTailable = HourlyDeviceCountTailable()
)