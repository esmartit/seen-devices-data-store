package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository
import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.count
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity")
class DetectedController(
    private val commonService: CommonService,
    private val scanApiService: ScanApiService,
    private val brandsRepository: BrandsRepository,
    private val clock: ClockService,
    private val template: ReactiveMongoTemplate
) {

    @GetMapping(path = ["/total-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        val aggregation = newAggregation(
            match(where("status").ne(NO_POSITION)),
            group("clientMac"),
            count().`as`("total")
        )
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
            .flatMap { template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java) }
            .map { TotalDevices(it.getInteger("total"), clock.now()) }
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
        filters: FilterRequest
    ): Flux<NowPresence> {
        val todayDetected = commonService.todayFluxGrouped(filters)
        val fifteenSeconds = Duration.ofSeconds(15)
        val latest = Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap { todayDetected.last(NowPresence(UUID.randomUUID().toString())) }
        return Flux.concat(todayDetected, latest)
    }

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        filters: FilterRequest
    ): Flux<DailyDevices> {
        val fifteenSeconds = Duration.ofSeconds(15)
        return Flux.interval(Duration.ofSeconds(0), fifteenSeconds).onBackpressureDrop()
            .flatMap {
                commonService.todayFlux(filters)
                    .map { it.clientMac }
                    .distinct()
                    .count()
                    .map { DailyDevices(it, clock.now()) }
            }
    }

    @GetMapping(path = ["/today-brands"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyBrands(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15))
        .flatMap {
            scanApiService.hourlyFilteredFlux(
                startDateTimeFilter = clock.startOfDay(zoneId).toInstant(),
                endDateTimeFilter = null,
                filters = null
            ).map { brandsRepository.findByName(it.brand ?: "other") }
                .groupBy { it.name }
                .flatMap { group -> group.count().map { BrandCount(group.key()!!, it) } }
                .collectList()
        }


}

data class BrandCount(val name: String, val value: Long)

enum class FilterDateGroup {
    BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}