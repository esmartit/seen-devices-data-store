package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.bigdata.AverageHourlyPresence
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_DAY
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_HOUR
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MINUTE
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MONTH
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_WEEK
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_YEAR
import com.esmartit.seendevicesdatastore.application.smartpoke.SmartPokeHourlyDevice
import com.esmartit.seendevicesdatastore.domain.*
import com.esmartit.seendevicesdatastore.v2.application.filter.*
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.Aggregation.unwind
import org.springframework.data.mongodb.core.aggregation.AggregationOptions.builder
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Service
class QueryHourlyService(
        private val template: ReactiveMongoTemplate,
        private val clockService: ClockService
) {

    fun findHourly(hourlyContext: FilterHourlyContext): Flux<DeviceTotalHourly> {

        return findHourlyRaw(hourlyContext)
                .map {
                    DeviceTotalHourly(
                            group = it["groupDate", ""],
                            clientMac = it["clientMac", ""],
                            position = Position.byValue(it["statusNumeral", 0])
                    )
                }
    }

    fun findHourlyRaw(hourlyContext: FilterHourlyContext): Flux<Document> {
        val filtersHourly = hourlyContext.filterHourlyRequest
        hourlyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(filtersHourly),
                match(hourlyContext.criteria),
                group("clientMac")
                        .addToSet("dateAtZone").`as`("dateAtZone")
                        .addToSet(Document.parse("{dateAtZone:\"\$dateAtZone\",groupDate:\"\$groupDate\",statusNumeral:\"\$statusNumeral\"}"))
                        .`as`("root"),
                project("_id", "root").and("dateAtZone").size().`as`("presence"),
                match(filtersHourly.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                unwind("root"),
                project("root.dateAtZone", "root.groupDate", "root.statusNumeral").and("_id").`as`("clientMac")
                        .andExclude("_id"),
                group("dateAtZone", "groupDate", "clientMac").max("statusNumeral").`as`("statusNumeral"),
                project("_id.groupDate", "_id.clientMac", "statusNumeral").andExclude("_id"),
                sort(Sort.Direction.ASC, "dateAtZone", "groupDate")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityH::class.java, Document::class.java)
    }

    fun getDetailedReportwithTime(hourlyContext: FilterHourlyContext): Flux<Document> {
        val filtersHourly = hourlyContext.filterHourlyRequest
        hourlyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(filtersHourly),
                match(hourlyContext.criteria)
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityH::class.java, Document::class.java)
    }

    fun findHourlySmartPokeRaw(filtersHourly: FilterHourlyRequest): Flux<SmartPokeHourlyDevice> {
        val hourlyContext = createContext(filtersHourly)
        hourlyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(filtersHourly),
                match(hourlyContext.criteria),
                group("username")
                        .addToSet("dateAtZone").`as`("dateAtZone")
                        .addToSet(Document.parse("{spotId:\"\$spotId\",sensorId:\"\$sensorId\",dateAtZone:\"\$dateAtZone\"}"))
                        .`as`("root"),
                match(Criteria.where("_id").ne("")),
                project("_id", "root").and("dateAtZone").size().`as`("presence"),
                match(filtersHourly.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                unwind("root"),
                project("root.spotId", "root.sensorId").and("_id").`as`("userName")
                        .andExclude("_id"),
                group("spotId", "sensorId", "userName"),
                project("_id.spotId", "_id.sensorId", "_id.userName").andExclude("_id")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityH::class.java, Document::class.java)
                .map {
                    SmartPokeHourlyDevice(
                            spot = it["spotId", ""],
                            sensor = it["sensorId", ""],
                            userName = it["userName", ""]
                    )
                }
    }

    fun avgHourlyDwellTime(hourlyFilters: FilterHourlyRequest): Flux<AverageHourlyPresence> {
        val hourlyContext = createContext(hourlyFilters)
        hourlyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(hourlyFilters),
                match(hourlyContext.criteria),
                group("dateAtZone", "clientMac", "totalTime"),
                project("_id")
                        .andExpression("_id.totalTime / 1000").`as`("dwellTime"),
                group().avg("dwellTime").`as`("avgHourlyDwellTime")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityH::class.java, Document::class.java)
                .map { AverageHourlyPresence(value = it["avgHourlyDwellTime", 0.0]) }
    }

    fun todayDetected(hourlyFilters: FilterHourlyRequest): Flux<NowPresence> {
        val hourlyContext = createContext(hourlyFilters)
        return findHourly(hourlyContext)
                .groupBy { it.group }
                .flatMap { g -> groupByTime(g) }
                .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    fun getTotalDevicesHourly(hourlyFilters: FilterHourlyRequest): Flux<HourlyDevices> {
        val hourlyContext = createContext(hourlyFilters).also { it.next() }
        val aggregation = newAggregation(
                match(hourlyContext.criteria),
                group("clientMac"),
                Aggregation.count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityH::class.java, Document::class.java)
                .map { HourlyDevices(it.getInteger("total"), clockService.now()) }
    }

    private fun groupByTime(group: GroupedFlux<String, DeviceTotalHourly>): Mono<NowPresence> {
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
                Position.NO_POSITION -> acc
            }
        }
    }

    private fun scanApiProjection(filtersHourly: FilterHourlyRequest): ProjectionOperation {

        val format = when (filtersHourly.groupBy) {
            BY_MINUTE -> "%Y-%m-%dT%H:%M"
            BY_HOUR -> "%Y-%m-%dT%H:00"
            BY_DAY -> "%Y-%m-%d"
            BY_WEEK -> "%V"
            BY_MONTH -> "%Y-%m"
            BY_YEAR -> "%Y"
        }

        return project(ScanApiActivityH::class.java)
                .andExclude("_id")
                .andExpression("{\$hour: { date: \"\$dateAtZone\", timezone: \"${filtersHourly.timezone}\" }}")
                .`as`("hourAtZone")
                .andExpression("{ \$dateToString: { format: \"$format\", date: \"\$dateAtZone\" } }")
                .`as`("groupDate")
                .and(
                        ConditionalOperators.Switch.switchCases(
                                ConditionalOperators.Switch.CaseOperator.`when`(ComparisonOperators.Eq.valueOf("status").equalToValue("OUT")).then(1),
                                ConditionalOperators.Switch.CaseOperator.`when`(ComparisonOperators.Eq.valueOf("status").equalToValue("LIMIT")).then(2),
                                ConditionalOperators.Switch.CaseOperator.`when`(ComparisonOperators.Eq.valueOf("status").equalToValue("IN")).then(3)
                        ).defaultTo(-1)
                ).`as`("statusNumeral")
    }

    fun createContext(hourlyFilters: FilterHourlyRequest): FilterHourlyContext {
        return FilterHourlyContext(
                filterHourlyRequest = hourlyFilters,
                chain = listOf(
                        DateFilterHourlyBuilder(),
                        HourFilterHourlyBuilder(),
                        LocationFilterHourlyBuilder(),
                        BrandFilterHourlyBuilder(),
                        StatusFilterHourlyBuilder(),
                        UserInfoFilterHourlyBuilder()
                )
        )
    }

}

data class DeviceTotalHourly(val group: String, val clientMac: String, val position: Position)