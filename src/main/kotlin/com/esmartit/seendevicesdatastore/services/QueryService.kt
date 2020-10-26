package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository
import com.esmartit.seendevicesdatastore.application.dashboard.detected.BrandCount
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_DAY
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_HOUR
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MINUTE
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MONTH
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_WEEK
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_YEAR
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.v2.application.filter.BrandFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.CustomDateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.DateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import com.esmartit.seendevicesdatastore.v2.application.filter.HourFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.LocationFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.StatusFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.UserInfoFilterBuilder
import org.bson.Document
import org.bson.Document.parse
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.count
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.replaceRoot
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.Aggregation.unwind
import org.springframework.data.mongodb.core.aggregation.AggregationOptions.builder
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq.valueOf
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.CaseOperator.`when`
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.switchCases
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.time.ZoneId
import java.util.UUID

@Component
class QueryService(
    private val template: ReactiveMongoTemplate,
    private val brandsRepository: BrandsRepository,
    private val clockService: ClockService
) {

    fun find(context: FilterContext): Flux<DeviceAndPosition> {

        return findRaw(context)
            .map {
                DeviceAndPosition(
                    it["groupDate", ""],
                    it["clientMac", ""],
                    Position.byValue(it["statusNumeral", 0])
                )
            }
    }

    fun findRaw(context: FilterContext): Flux<Document> {
        val filters = context.filterRequest
        context.next(context)
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("clientMac")
                .addToSet("dateAtZone").`as`("dateAtZone")
                .addToSet(parse("{groupDate:\"\$groupDate\",dateAtZone:\"\$dateAtZone\",statusNumeral:\"\$statusNumeral\"}"))
                .`as`("root"),
            project("_id", "root").and("dateAtZone").size().`as`("presence"),
            match(filters.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                ?: Criteria()),
            unwind("root"),
            project("root.groupDate", "root.statusNumeral").and("_id").`as`("clientMac").andExclude("_id"),
            group("groupDate", "clientMac").max("statusNumeral").`as`("statusNumeral"),
            project("_id.groupDate", "_id.clientMac", "statusNumeral").andExclude("_id"),
            sort(Sort.Direction.ASC, "groupDate")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun findScanApi(context: FilterContext): Flux<Document> {
        context.next(context)
        val filters = context.filterRequest
        val aggregation = newAggregation(scanApiProjection(filters),
            match(context.criteria),
            group("clientMac")
                .addToSet("dateAtZone").`as`("dateAtZone")
                .addToSet("\$\$ROOT").`as`("root"),
            project("_id", "root").and("dateAtZone").size().`as`("presence"),
            match(filters.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                ?: Criteria()),
            unwind("root"),
            replaceRoot("root"),
            sort(Sort.Direction.ASC, "groupDate")
        )
            .withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun createContext(filters: FilterRequest): FilterContext {
        return FilterContext(
            filterRequest = filters,
            chain = listOf(
                DateFilterBuilder(),
                HourFilterBuilder(),
                LocationFilterBuilder(),
                BrandFilterBuilder(),
                StatusFilterBuilder(),
                UserInfoFilterBuilder()
            )
        )
    }

    fun createTodayContext(filters: FilterRequest): FilterContext {
        return FilterContext(
            filterRequest = filters.copy(groupBy = BY_HOUR),
            chain = listOf(
                CustomDateFilterBuilder(clockService.startOfDay(filters.timezone).toInstant()),
                HourFilterBuilder(),
                LocationFilterBuilder(),
                BrandFilterBuilder(),
                StatusFilterBuilder(),
                UserInfoFilterBuilder()
            )
        )
    }

    fun getTotalDevicesAll(): Flux<TotalDevices> {
        val aggregation = newAggregation(
            match(where("status").ne(NO_POSITION)),
            group("clientMac"),
            count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { TotalDevices(it.getInteger("total"), clockService.now()) }
    }

    fun getTotalDevicesToday(filters: FilterRequest): Flux<TotalDevices> {
        val context = createTodayContext(filters).also { it.next(it) }
        val aggregation = newAggregation(
            match(context.criteria),
            group("clientMac"),
            count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { TotalDevices(it.getInteger("total"), clockService.now()) }
    }

    fun getTodayDevicesGroupedByBrand(zoneId: ZoneId): Flux<List<BrandCount>> {
        val filters = FilterRequest(timezone = zoneId, groupBy = BY_DAY)
        val context = createTodayContext(filters).also { it.next(it) }

        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("clientMac").addToSet("brand").`as`("brand"),
            project("brand").andExclude("_id"),
            unwind("brand"),
            group("brand").count().`as`("count")
        ).withOptions(builder().allowDiskUse(true).build())

        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { BrandCount(it.getString("_id"), it.getInteger("count")) }
            .collectList()
            .toFlux()
    }

    fun todayDetected(filters: FilterRequest): Flux<NowPresence> {
        val context = createTodayContext(filters)
        return find(context)
            .groupBy { it.group }
            .flatMap { g -> groupByTime(g) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    private fun groupByTime(group: GroupedFlux<String, DeviceAndPosition>): Mono<NowPresence> {
        return group.reduce(
            NowPresence(
                id = UUID.randomUUID().toString(),
                time = group.key()!!
            )
        ) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                NO_POSITION -> acc
            }
        }
    }

    private fun scanApiProjection(filters: FilterRequest): ProjectionOperation {

        val format = when (filters.groupBy) {
            BY_MINUTE -> "%Y-%m-%dT%H:%M"
            BY_HOUR -> "%Y-%m-%dT%H:00"
            BY_DAY -> "%Y-%m-%d"
            BY_WEEK -> "%V"
            BY_MONTH -> "%Y-%m"
            BY_YEAR -> "%Y"
        }

        return project(ScanApiActivity::class.java)
            .andExclude("_id")
            .andExpression("{\$hour: { date: \"\$seenTime\", timezone: \"${filters.timezone}\" }}")
            .`as`("hourAtZone")
            .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$seenTime\", timezone: \"${filters.timezone}\" } }")
            .`as`("dateAtZone")
            .andExpression("{ \$dateToString: { format: \"$format\", date: \"\$seenTime\", timezone: \"${filters.timezone}\" } }")
            .`as`("groupDate")
            .and(
                switchCases(
                    `when`(valueOf("status").equalToValue("OUT")).then(1),
                    `when`(valueOf("status").equalToValue("LIMIT")).then(2),
                    `when`(valueOf("status").equalToValue("IN")).then(3)
                ).defaultTo(-1)
            ).`as`("statusNumeral")
    }
}

data class DeviceAndPosition(val group: String, val clientMac: String, val position: Position)
