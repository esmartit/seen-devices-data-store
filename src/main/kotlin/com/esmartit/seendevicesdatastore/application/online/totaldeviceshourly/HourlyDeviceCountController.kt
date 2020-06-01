package com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly

import com.esmartit.seendevicesdatastore.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.repository.DeviceWithPosition
import com.esmartit.seendevicesdatastore.repository.Position.IN
import com.esmartit.seendevicesdatastore.repository.Position.LIMIT
import com.esmartit.seendevicesdatastore.repository.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.repository.Position.OUT
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity")
class HourlyDeviceCountController(
    private val hourlyDeltaCounter: HourlyDeltaCounter,
    private val repository: HourlyDeviceCountTailableRepository,
    private val rxRepo: DevicePositionReactiveRepository,
    private val clock: Clock
) {

    @GetMapping(path = ["/hourly-device-presence-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<HourlyDeviceCountTailable> {
        return repository.findWithTailableCursorBy()
    }

    @GetMapping(path = ["/hourly-device-presence-delta"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivityDelta(): Flux<HourlyDeviceCountTailable> {
        return hourlyDeltaCounter.getCounter()
    }

    @GetMapping(path = ["/today-hourly-device-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getHourlyPresence(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<HourlyDeviceCountTailable> {

        val startOfDay = clock.instant().atZone(zoneId).truncatedTo(ChronoUnit.DAYS).toInstant()

        val counter = rxRepo.findBySeenTimeGreaterThanEqual(startOfDay)
            .groupBy { it.seenTime }
            .flatMap(this::reduceByHour)
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }

        val latest = Flux.interval(Duration.ofSeconds(1), Duration.ofSeconds(15)).onBackpressureDrop()
            .flatMap { counter.last() }

        return Flux.concat(counter, latest)
    }

    private fun reduceByHour(g: GroupedFlux<Instant, DeviceWithPosition>): Mono<HourlyDeviceCountTailable> {
        val id = UUID.randomUUID().toString().replace("-", "")
        return g.reduce(HourlyDeviceCountTailable(id = id, time = g.key()!!), this::aggregatePresence)
    }

    private fun aggregatePresence(t: HourlyDeviceCountTailable, u: DeviceWithPosition): HourlyDeviceCountTailable {
        return when (u.position) {
            IN -> t.copy(inCount = t.inCount + 1)
            LIMIT -> t.copy(limitCount = t.limitCount + 1)
            OUT -> t.copy(outCount = t.outCount + 1)
            NO_POSITION -> t
        }
    }
}
