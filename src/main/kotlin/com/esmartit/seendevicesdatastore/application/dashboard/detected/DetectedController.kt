package com.esmartit.seendevicesdatastore.application.dashboard.detected

import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository
import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position.IN
import com.esmartit.seendevicesdatastore.domain.Position.LIMIT
import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.Position.OUT
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.DeviceAndPosition
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import com.esmartit.seendevicesdatastore.v2.application.filter.BrandFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.CustomDateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import com.esmartit.seendevicesdatastore.v2.application.filter.HourFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.LocationFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.StatusFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.UserInfoFilterBuilder
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
import reactor.core.publisher.Flux.interval
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration.ofSeconds
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/sensor-activity")
class DetectedController(
    private val commonService: CommonService,
    private val scanApiService: ScanApiService,
    private val brandsRepository: BrandsRepository,
    private val clock: ClockService,
    private val template: ReactiveMongoTemplate,
    private val queryService: QueryService
) {

    @GetMapping(path = ["/total-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAllSensorActivity(): Flux<TotalDevices> {
        val aggregation = newAggregation(
            match(where("status").ne(NO_POSITION)),
            group("clientMac"),
            count().`as`("total")
        )
        return interval(ofSeconds(0), ofSeconds(15))
            .flatMap { template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java) }
            .map { TotalDevices(it.getInteger("total"), clock.now()) }
    }

    @GetMapping(path = ["/today-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetected(
        filters: FilterRequest
    ): Flux<NowPresence> {
        return todayFlux(filters).concatWith(interval(ofSeconds(0), ofSeconds(15))
            .flatMap { todayFlux(filters).last() })
    }

    @GetMapping(path = ["/today-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyDetectedCount(
        filters: FilterRequest
    ): Flux<DailyDevices> {
        val fifteenSeconds = ofSeconds(15)
        return interval(ofSeconds(0), fifteenSeconds)
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
    ) = interval(ofSeconds(0), ofSeconds(15))
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

    private fun todayFlux(filters: FilterRequest): Flux<NowPresence> {
        return queryService.find(todayContext(filters))
            .groupBy { it.group }
            .flatMap { g -> groupByTime(g) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun todayContext(filters: FilterRequest): FilterContext {
        return FilterContext(
            filterRequest = filters.copy(groupBy = FilterDateGroup.BY_HOUR),
            chain = listOf(
                CustomDateFilterBuilder(clock.startOfDay(filters.timezone).toInstant()),
                HourFilterBuilder(),
                LocationFilterBuilder(),
                BrandFilterBuilder(),
                StatusFilterBuilder(),
                UserInfoFilterBuilder()
            )
        )
    }

    fun groupByTime(group: GroupedFlux<String, DeviceAndPosition>): Mono<NowPresence> {
        return group.reduce(
            NowPresence(
                id = UUID.randomUUID().toString(),
                time = group.key()!!
            )
        ) { acc, curr ->
            when (curr.position) {
                IN -> acc.copy(inCount = acc.inCount + 1)
                LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                OUT -> acc.copy(outCount = acc.outCount + 1)
                NO_POSITION -> acc
            }
        }
    }
}

data class BrandCount(val name: String, val value: Long)

enum class FilterDateGroup {
    BY_MINUTE, BY_HOUR, BY_DAY, BY_WEEK, BY_MONTH, BY_YEAR
}