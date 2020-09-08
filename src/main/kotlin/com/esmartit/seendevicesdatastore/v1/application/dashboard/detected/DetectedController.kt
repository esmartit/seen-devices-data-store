package com.esmartit.seendevicesdatastore.v1.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.v1.application.brands.BrandsRepository
import com.esmartit.seendevicesdatastore.v1.application.dashboard.totaluniquedevices.TotalDevices
import com.esmartit.seendevicesdatastore.v1.application.dashboard.totaluniquedevices.TotalDevicesReactiveRepository
import com.esmartit.seendevicesdatastore.v1.repository.DevicePositionReactiveRepository
import com.esmartit.seendevicesdatastore.v1.services.BigDataService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.function.BiFunction

@RestController
@RequestMapping("/sensor-activity")
class DetectedController(
    private val repository: DevicePositionReactiveRepository,
    private val totalCountRepo: TotalDevicesReactiveRepository,
    private val clock: Clock,
    private val brandsRepository: BrandsRepository,
    private val bigDataService: BigDataService
) {

    @GetMapping(path = ["/total-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        val ticker = Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop()
        val counter = totalCountRepo.findWithTailableCursorBy()
        return Flux.combineLatest(ticker, counter, BiFunction { _: Long, b: TotalDevices -> b })
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
        requestFilters: FilterRequest
    ): Flux<NowPresence> {

        val todayDetected =
            bigDataService.fluxByDateTime(startOfDay(requestFilters.timezone), null, requestFilters)
                .let { bigDataService.presenceFlux(it) }
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap { todayDetected.last(NowPresence(UUID.randomUUID().toString())) }

        return Flux.concat(todayDetected, latest)
    }

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        val startOfDay = startOfDay(zoneId)
        val earlyFlux = repository.findBySeenTimeGreaterThanEqual(startOfDay)
            .scan(0L) { acc, _ -> acc + 1 }
        val nowFlux = { repository.findBySeenTimeGreaterThanEqual(startOfDay).count() }
        val fifteenSecs = Duration.ofSeconds(15)
        val ticker = Flux.interval(fifteenSecs).flatMap { nowFlux() }

        return Flux.concat(earlyFlux, ticker).map {
            DailyDevices(it, clock.instant())
        }
    }

    @GetMapping(path = ["/today-brands"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyBrands(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
        .flatMap {
            repository.findBySeenTimeGreaterThanEqual(startOfDay(zoneId))
                .map { brandsRepository.findByName(it.activity?.device?.manufacturer ?: "other") }
                .groupBy { it.name }
                .flatMap { group -> group.count().map { BrandCount(group.key()!!, it) } }
                .collectList()
        }


    private fun startOfDay(zoneId: ZoneId) =
        clock.instant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
}

data class BrandCount(val name: String, val value: Long)

enum class FilterDateGroup {
    BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}