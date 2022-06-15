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
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.Aggregation.unwind
import org.springframework.data.mongodb.core.aggregation.AggregationOptions.builder
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Service
class QueryDailyService(
        private val template: ReactiveMongoTemplate,
        private val clockService: ClockService
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

    fun avgDwellTime(filtersDaily: FilterDailyRequest): Flux<AverageDailyPresence> {
        val dailyContext = createContext(filtersDaily)
        dailyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                group("clientMac")
                        .addToSet("dateAtZone").`as`("dateAtZone")
                        .addToSet(Document.parse("{dateAtZone:\"\$dateAtZone\",groupDate:\"\$groupDate\",statusNumeral:\"\$statusNumeral\",totalTime:\"\$totalTime\"}"))
                        .`as`("root"),
                project("_id", "root").and("dateAtZone").size().`as`("presence"),
                match(filtersDaily.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                unwind("root"),
                project("root.dateAtZone", "root.groupDate", "root.statusNumeral", "root.totalTime").and("_id").`as`("clientMac")
                        .andExclude("_id"),
                group("dateAtZone", "clientMac", "totalTime"),
                project("_id")
                        .andExpression("_id.totalTime / 1000").`as`("dwellTime"),
                match(Criteria("dwellTime").gt(60)),
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


    fun getTotalDevicesToday(dailyFilters: FilterDailyRequest): Flux<TotalDevices> {
        val dailyContext = createContext(dailyFilters)
        dailyContext.next()
        val aggregation = newAggregation(
                scanApiProjection(dailyFilters),
                match(dailyContext.criteria),
                group("clientMac"),
                Aggregation.count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map { TotalDevices(it.getInteger("total"), clockService.now()) }
    }

    fun getTodayDevicesGroupedByBrand(dailyFilters: FilterDailyRequest): Flux<List<BrandCount>> {
//        val dailyFilters = FilterDailyRequest(timezone = zoneId, groupBy = FilterGroup.BY_DAY)
        val dailyContext = createContext(dailyFilters).also { it.next() }

        val aggregation = newAggregation(
                scanApiProjection(dailyFilters),
                match(dailyContext.criteria),
                group("clientMac").addToSet("brand").`as`("brand"),
                project("brand").andExclude("_id"),
                unwind("brand"),
                group("brand").count().`as`("count")
        ).withOptions(builder().allowDiskUse(true).build())

        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map { BrandCount(it.getString("_id"), it.getInteger("count")) }
                .collectList()
                .toFlux()
    }

    fun getTodayDevicesGroupedByZone(dailyFilters: FilterDailyRequest): Flux<List<ZoneCount>> {
//        val dailyFilters = FilterDailyRequest(timezone = zoneId, groupBy = FilterGroup.BY_DAY)
        val dailyContext = createContext(dailyFilters).also { it.next() }

        val aggregation = newAggregation(
                scanApiProjection(dailyFilters),
                match(dailyContext.criteria),
                group("spotId", "zone").addToSet("clientMac").`as`("clientMac"),
                project("clientMac")
                        .and("_id.spotId").`as`("spotId")
                        .and("_id.zone").`as`("zone")
                        .andExclude("_id"),
                unwind("clientMac"),
                group("spotId", "zone").count().`as`("count"),
                project("count")
                        .and("_id.spotId").`as`("spotId")
                        .and("_id.zone").`as`("zone")
                        .andExclude("_id"),
                sort(Sort.Direction.DESC, "count")
                ).withOptions(builder().allowDiskUse(true).build())

        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map { ZoneCount(it.getString("spotId"), it.getString("zone"), it.getInteger("count")) }
                .collectList()
                .toFlux()
    }


    fun getTodayDevicesGroupedByCountry(dailyFilters: FilterDailyRequest): Flux<List<CountryCount>> {
//        val dailyFilters = FilterDailyRequest(timezone = zoneId, groupBy = FilterGroup.BY_DAY)
        val dailyContext = createContext(dailyFilters).also { it.next() }

        val aggregation = newAggregation(
                scanApiProjection(dailyFilters),
                match(dailyContext.criteria),
                group("clientMac").addToSet("countryId").`as`("countryId"),
                project("countryId").andExclude("_id"),
                unwind("countryId"),
                group("countryId").count().`as`("count")
        ).withOptions(builder().allowDiskUse(true).build())

        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
                .map { CountryCount(it.getString("_id"), it.getInteger("count")) }
                .collectList()
                .toFlux()
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

    fun getConnectRegister(dailyContext: FilterDailyContext): Flux<Document> {
        dailyContext.next()
        val filtersDaily = dailyContext.filterDailyRequest
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                project("dateAtZone", "clientMac")
                        .and(ConditionalOperators.IfNull.ifNull("username").then("no_username")).`as`("userName")
                        .andExclude("_id"),
                group("dateAtZone", "clientMac", "userName"),
                LookupOperation.newLookup()
                        .from("registeredUser")
                        .localField("_id.userName")
                        .foreignField("info.username")
                        .`as`("userData"),
                unwind("userData", true),
                project("_id")
                        .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$_id.dateAtZone\", timezone: \"${filtersDaily.timezone}\" } }")
                        .`as`("dateAtZone")
                        .and("_id.clientMac").`as`("clientMac")
                        .and("_id.userName").`as`("userName")
                        .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$userData.info.seenTime\", timezone: \"${filtersDaily.timezone}\" } }")
                        .`as`("dateRegistered")
                        .andExclude("_id"),
                group("dateAtZone", "clientMac", "userName", "dateRegistered"),
                project("_id")
                        .and("_id.dateAtZone").`as`("dateAtZone")
                        .and("_id.dateRegistered").`as`("dateRegistered")
                        .and("_id.clientMac").`as`("clientMac")
                        .and("_id.userName").`as`("userName")
                        .and(
                                ConditionalOperators.`when`(
                                        Criteria.where("_id.dateRegistered")
                                                .`is`("\$_id.dateAtZone")
                                )
                                        .then(1)
                                        .otherwise(0)
                        )
                        .`as`("register")
                        .andExclude("_id"),
                group("dateAtZone")
                        .count().`as`("connected")
                        .sum("register").`as`("registered"),
                project("connected", "registered")
                        .and("_id").`as`("date")
                        .andExclude("_id"),
                sort(Sort.Direction.ASC, "date")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
    }

    fun getTotalDevicesTraffic(dailyContext: FilterDailyContext): Flux<Document> {
        dailyContext.next()
        val filtersDaily = dailyContext.filterDailyRequest
        val radiusContext =
                FilterDailyContext(filterDailyRequest = filtersDaily, chain = listOf(RadiusDateFilterDailyBuilder())).apply { next() }
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                project("dateAtZone", "clientMac")
                        .and(ConditionalOperators.IfNull.ifNull("username").then("no_username")).`as`("userName")
                        .andExclude("_id"),
                group("dateAtZone", "clientMac", "userName"),
                LookupOperation.newLookup()
                        .from("radiusActivity")
                        .localField("_id.userName")
                        .foreignField("info.username")
                        .`as`("userData"),
                unwind("userData"),
                project("_id")
                        .and("_id.dateAtZone").`as`("dateAtZone")
                        .and("_id.clientMac").`as`("clientMac")
                        .and("_id.userName").`as`("userName")
                        .and("userData.info.eventTimeStamp").`as`("dateRadiusAct")
                        .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$userData.info.eventTimeStamp\", timezone: \"${filtersDaily.timezone}\" } }")
                        .`as`("dateRadius")
                        .andExpression("{ \$toLong: \"\$userData.info.acctInputOctets\" }").`as`("inputOct")
                        .andExpression("{ \$toLong: \"\$userData.info.acctOutputOctets\" }").`as`("outputOct")
                        .andExclude("_id"),
                match(radiusContext.criteria),
                group("dateRadius", "userName")
                        .max("inputOct").`as`("InputOcts")
                        .max("outputOct").`as`("OutputOcts"),
                project("_id", "InputOcts", "OutputOcts")
                        .and("_id.dateRadius").`as`("dateRadius")
                        .andExclude("_id"),
                group()
                        .sum("InputOcts").`as`("TotalInputOcts")
                        .sum("OutputOcts").`as`("TotalOutputOcts"),
                project("TotalInputOcts", "TotalOutputOcts").andExclude("_id")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
    }

    fun getTotalUsersTime(dailyContext: FilterDailyContext): Flux<Document> {
        dailyContext.next()
        val filtersDaily = dailyContext.filterDailyRequest
        val radiusContext =
                FilterDailyContext(filterDailyRequest = filtersDaily, chain = listOf(RadiusDateFilterDailyBuilder())).apply { next() }
        val aggregation = newAggregation(
                scanApiProjection(filtersDaily),
                match(dailyContext.criteria),
                project("dateAtZone", "clientMac")
                        .and(ConditionalOperators.IfNull.ifNull("username").then("no_username")).`as`("userName")
                        .andExclude("_id"),
                group("dateAtZone", "clientMac", "userName"),
                LookupOperation.newLookup()
                        .from("radiusActivity")
                        .localField("_id.userName")
                        .foreignField("info.username")
                        .`as`("userData"),
                unwind("userData"),
                project("_id")
                        .and("_id.dateAtZone").`as`("dateAtZone")
                        .and("_id.clientMac").`as`("clientMac")
                        .and("_id.userName").`as`("userName")
                        .and("userData.info.eventTimeStamp").`as`("dateRadiusAct")
                        .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$userData.info.eventTimeStamp\", timezone: \"${filtersDaily.timezone}\" } }")
                        .`as`("dateRadius")
                        .andExpression("{ \$toLong: \"\$userData.info.acctSessionTime\" }").`as`("session")
                        .andExpression("{ \$toLong: \"\$userData.info.acctInputOctets\" }").`as`("inputOct")
                        .andExpression("{ \$toLong: \"\$userData.info.acctOutputOctets\" }").`as`("outputOct")
                        .andExclude("_id"),
                match(radiusContext.criteria),
                group("dateRadius", "userName")
                        .max("session").`as`("sessionTime")
                        .max("inputOct").`as`("InputOcts")
                        .max("outputOct").`as`("OutputOcts"),
                project("_id", "sessionTime", "InputOcts", "OutputOcts")
                        .and("_id.dateRadius").`as`("dateRadius")
                        .andExclude("_id"),
                group("dateRadius")
                        .count().`as`("TotalUsers")
                        .avg("sessionTime").`as`("AvgSessionTime")
                        .sum("InputOcts").`as`("TotalInputOcts")
                        .sum("OutputOcts").`as`("TotalOutputOcts"),
                project("TotalUsers", "AvgSessionTime", "TotalInputOcts", "TotalOutputOcts")
                        .and("_id").`as`("date")
                        .andExclude("_id"),
                sort(Sort.Direction.ASC, "date")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivityD::class.java, Document::class.java)
    }

    private fun groupByTime(group: GroupedFlux<String, DeviceTotalBigData>): Mono<NowPresence> {
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