package com.esmartit.seendevicesdatastore

import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.extra.math.max
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/sensor-activity")
class SensorActivityController(
    private val reactiveRepository: DeviceStatReactiveRepository,
    private val repo: DeviceStatRepository
) {

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

        val truncatedTime = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val p = repo.findByAccessPointAndDeviceMacAddressAndSeenTime(repo.findAll(PageRequest.of(1,10)).first().accessPoint,"cc:44:63:9a:da:67", Instant.parse("2020-03-13T20:33:13Z"))
        return reactiveRepository.findBySeenTimeBetween(startDate.toInstant(), endDate.toInstant())
            .map { presence(it) }
            .window(Duration.ofMillis(100))
            .flatMap { w ->
                w.groupBy { it.macAddress }
                    .flatMap { group ->
                        group.max { stat, stat2 -> stat.position.value.compareTo(stat2.position.value) }
                            .map { DeviceState(it.macAddress, it.position) }
                    }
            }
            .scan(DailyStat(ZonedDateTime.now()), ::scanDailyStat)
            .map { dailyStat->dailyStat.devices.values.groupingBy { it }.eachCount() }
            .doFinally { println("END!!!!") }
    }

    fun scanDailyStat(dailyStat: DailyStat, deviceState: DeviceState): DailyStat {

        return dailyStat.apply {
            dailyStat.devices.compute(deviceState.macAddress) { k, v ->
                val oldVal = v ?: Position.NO_POSITION
                if (deviceState.position.value > oldVal.value) {
                    deviceState.position
                } else {
                    oldVal
                }
            }
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
    IN(3), LIMIT(2), OUT(1), NO_POSITION(-1)
}

data class Stat(val macAddress: String, val position: Position, val hour: Int, val seenTime: ZonedDateTime)


data class DailyStat(
    val date: ZonedDateTime,
    val devices: MutableMap<String, Position> = ConcurrentHashMap()
)


data class DeviceState(
    val macAddress: String,
    val position: Position
)