package com.esmartit.seendevicesdatastore

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.extra.math.max
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

@RestController
@RequestMapping("/sensor-activity")
class SensorActivityController(private val reactiveRepository: DeviceStatReactiveRepository) {

    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<SensorActivity> {
        return reactiveRepository.findAll()
    }

    @GetMapping(path = ["/since"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllFromDateSensorActivity(
        @RequestParam("startDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Date,
        @RequestParam("endDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: Date
    ): Flux<*> {
//        return reactiveRepository.findBySeenTimeBetween(startDate.toInstant(), endDate.toInstant())
//            .map { presence(it) }
//            .window(Duration.ofMillis(500))
//            .flatMap { w ->
//                w.groupBy { it.position }
//                    .flatMap { group -> group.count().map { group.key() to it } }
//            }
//            .groupBy { it.first }
//            .flatMap { g->g.scan { a, b -> Pair(a.first, a.second + b.second) } }
        return reactiveRepository.findAll()
            .map { presence(it) }
            .groupBy { it.seenTime.truncatedTo(ChronoUnit.DAYS) }
            .flatMap { groupByDay ->
                groupByDay
                    .groupBy { it.macAddress }
                    .flatMap { deviceGroup -> deviceGroup.collectList().map { deviceGroup.key() to it } }
                    .scan(DailyStat(groupByDay.key()!!)) { a, b ->
                        a.apply {
                            when (b.second.maxBy { it.position }?.position) {
                                Position.IN -> inCounter.incrementAndGet()
                                Position.LIMIT -> limitCounter.incrementAndGet()
                                Position.OUT -> outCounter.incrementAndGet()
                            }
                        }
                    }
//                    .groupBy { it.macAddress }
//                    .flatMap { groupByDayAnDevice ->
//                        groupByDayAnDevice
//                    }
//                    .scan(mutableListOf<Stat>(), { a, c -> a.apply { add(c) } })
            }
    }

    private fun inHourRange(it: SensorActivity): Boolean {
        val hour = it.seenTime.atZone(ZoneOffset.UTC).hour
        return hour in 0..1
    }

    private fun presence(sensorActivity: SensorActivity): Stat {

        val power = sensorActivity.rssi - 95

        val position = when {
            power >= -35 -> Position.IN
            power >= -45 -> Position.LIMIT
            else -> Position.OUT
        }

        val seenTime = sensorActivity.seenTime.atZone(ZoneOffset.UTC)
        return Stat(sensorActivity.device.macAddress, position, seenTime.hour, seenTime)
    }
}

enum class Position(val value: Int) {
    IN(3), LIMIT(2), OUT(1)
}

data class Stat(val macAddress: String, val position: Position, val hour: Int, val seenTime: ZonedDateTime)


data class DailyStat(
    val date: ZonedDateTime,
    val inCounter: AtomicInteger = AtomicInteger(),
    val limitCounter: AtomicInteger = AtomicInteger(),
    val outCounter: AtomicInteger = AtomicInteger()
)