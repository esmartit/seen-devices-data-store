package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.bigdata.AverageDailyPresence
import com.esmartit.seendevicesdatastore.application.bigdata.TotalDevicesDailyBigData
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterGroup
import com.esmartit.seendevicesdatastore.application.smartpoke.SmartPokeDailyDevice
import com.esmartit.seendevicesdatastore.domain.*
import com.esmartit.seendevicesdatastore.v2.application.filter.*
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
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

@Service
class QueryDailyService(
        private val template: ReactiveMongoTemplate
) {

    fun findBigData(dailyContext: FilterDailyContext): Flux<DeviceTotalBigData> {

        return findBigDataRaw(dailyContext)
                .map {
                    DeviceTotalBigData(
                            group = it["groupDate", ""],
                            clientMac = it["clientMac", ""],
                            position = Position.byValue(it["statusNumeral", 0])
                    )
                }
    }

    fun findBigDataRaw(dailyContext: FilterDailyContext): Flux<Document> {
        val filtersDaily = dailyContext.filterDailyRequest
        dailyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                group("clientMac")
                        .addToSet("dateAtZone").`as`("dateAtZone")
                        .addToSet(Document.parse("{dateAtZone:\"\$dateAtZone\",groupDate:\"\$groupDate\",statusNumeral:\"\$statusNumeral\"}"))
                        .`as`("root"),
                project("_id", "root").and("dateAtZone").size().`as`("presence"),
                match(filtersDaily.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                unwind("root"),
                project("root.dateAtZone", "root.groupDate", "root.statusNumeral").and("_id").`as`("clientMac")
                        .andExclude("_id"),
                group("dateAtZone", "groupDate", "clientMac").max("statusNumeral").`as`("statusNumeral"),
                project("_id.groupDate", "_id.clientMac", "statusNumeral").andExclude("_id"),
                sort(Sort.Direction.ASC, "dateAtZone", "groupDate")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
    }

    fun findTotalSmartPokeRaw(filtersDaily: FilterDailyRequest): Flux<SmartPokeDailyDevice> {
        val dailyContext = createContext(filtersDaily)
        dailyContext.next()
        val smartpokeDailyContext =
                FilterDailyContext(filterDailyRequest = filtersDaily, chain = listOf(SmartPokeDateFilterDailyBuilder())).apply { next() }
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                group("username")
                        .addToSet("dateAtZone").`as`("dateAtZone")
                        .addToSet(Document.parse("{spotId:\"\$spotId\",sensorId:\"\$sensorId\",dateAtZone:\"\$dateAtZone\"}"))
                        .`as`("root"),
                match(Criteria.where("_id").ne("")),
                project("_id", "root").and("dateAtZone").size().`as`("presence"),
                match(filtersDaily.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                unwind("root"),
                project("root.spotId", "root.sensorId").and("_id").`as`("userName")
                        .andExclude("_id"),
                group("spotId", "sensorId", "userName"),
                project("_id.spotId", "_id.sensorId", "_id.userName").andExclude("_id")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map {
                    SmartPokeDailyDevice(
                            spot = it["spotId", ""],
                            sensor = it["sensorId", ""],
                            userName = it["userName", ""]
                    )
                }
    }

    fun getDetailedReportwithTime(dailyContext: FilterDailyContext): Flux<Document> {
        val filtersDaily = dailyContext.filterDailyRequest
        dailyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria)
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
    }


    fun avgDwellTime(dailyFilters: FilterDailyRequest): Flux<AverageDailyPresence> {
        val dailyContext = createContext(dailyFilters)
        dailyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(dailyFilters),
                match(dailyContext.criteria),
                group("dateAtZone", "clientMac", "totalTime"),
                project("_id")
                        .andExpression("_id.totalTime / 1000").`as`("dwellTime"),
                group().avg("dwellTime").`as`("avgDwellTime")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map { AverageDailyPresence(value = it["avgDwellTime", 0.0]) }
    }

    fun getTotalDevicesBigData(dailyFilters: FilterDailyRequest): Flux<TotalDevicesDailyBigData> {
        val dailyContext = createContext(dailyFilters)
        dailyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(dailyFilters),
                match(dailyContext.criteria),
                group("clientMac")
                        .addToSet("dateAtZone").`as`("dateAtZone"),
                project("_id").and("dateAtZone").size().`as`("presence"),
                match(dailyFilters.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                group().count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map { TotalDevicesDailyBigData(count = it["total", 0]) }
    }

    fun getDailyDetailedbyUsername(dailyContext: FilterDailyContext): Flux<Document> {
        dailyContext.next()
        val filtersDaily = dailyContext.filterDailyRequest
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                group("groupDate", "username"),
                project("groupDate", "username").andExclude("_id"),
                match(Criteria.where("username").exists(true))
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
    }


    private fun scanApiProjection(filtersDaily: FilterDailyRequest): ProjectionOperation {

        val format = when (filtersDaily.groupBy) {
            FilterGroup.BY_DAY -> "%Y-%m-%d"
            FilterGroup.BY_WEEK -> "%V"
            FilterGroup.BY_MONTH -> "%Y-%m"
            FilterGroup.BY_YEAR -> "%Y"
        }

        return project(ScanApiActivityD::class.java)
                .andExclude("_id")
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

    fun createContext(dailyFilters: FilterDailyRequest): FilterDailyContext {
        return FilterDailyContext(
                filterDailyRequest = dailyFilters,
                chain = listOf(
                        DateFilterDailyBuilder(),
                        LocationFilterDailyBuilder(),
                        BrandFilterDailyBuilder(),
                        StatusFilterDailyBuilder(),
                        UserInfoFilterDailyBuilder()
                )
        )
    }

}

data class DeviceTotalBigData(val group: String, val clientMac: String, val position: Position)
