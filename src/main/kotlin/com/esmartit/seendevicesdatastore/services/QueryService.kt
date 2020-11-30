package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.bigdata.AveragePresence
import com.esmartit.seendevicesdatastore.application.bigdata.TotalDevicesBigData
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_DAY
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_HOUR
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MINUTE
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MONTH
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_WEEK
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_YEAR
import com.esmartit.seendevicesdatastore.application.smartpoke.SmartPokeDevice
import com.esmartit.seendevicesdatastore.domain.BrandCount
import com.esmartit.seendevicesdatastore.domain.CountryCount
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.domain.TotalDevicesAll
import com.esmartit.seendevicesdatastore.v2.application.filter.*
import org.bson.Document
import org.bson.Document.parse
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.count
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.Aggregation.unwind
import org.springframework.data.mongodb.core.aggregation.AggregationOptions.builder
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq.valueOf
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.IfNull.ifNull
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.CaseOperator.`when`
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.switchCases
import org.springframework.data.mongodb.core.aggregation.LookupOperation
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
        context.next()
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

    fun findBigData(context: FilterContext): Flux<DeviceBigData> {

        return findBigDataRaw(context)
            .map {
                DeviceBigData(
                    group = it["groupDate", ""],
                    clientMac = it["clientMac", ""],
                    position = Position.byValue(it["statusNumeral", 0])
                )
            }
    }

    fun findBigDataRaw(context: FilterContext): Flux<Document> {
        val filters = context.filterRequest
        context.next()
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("clientMac")
                .addToSet("dateAtZone").`as`("dateAtZone")
                .addToSet(parse("{dateAtZone:\"\$dateAtZone\",groupDate:\"\$groupDate\",statusNumeral:\"\$statusNumeral\"}"))
                .`as`("root"),
            project("_id", "root").and("dateAtZone").size().`as`("presence"),
            match(filters.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                ?: Criteria()),
            unwind("root"),
            project("root.dateAtZone", "root.groupDate", "root.statusNumeral").and("_id").`as`("clientMac")
                .andExclude("_id"),
            group("dateAtZone", "groupDate", "clientMac").max("statusNumeral").`as`("statusNumeral"),
            project("_id.groupDate", "_id.clientMac", "statusNumeral").andExclude("_id"),
            sort(Sort.Direction.ASC, "dateAtZone", "groupDate")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun findSmartPokeRaw(filters: FilterRequest): Flux<SmartPokeDevice> {
        val context = createContext(filters)
        context.next()
        val smartpokeContext =
                FilterContext(filterRequest = filters, chain = listOf(SmartPokeDateFilterBuilder())).apply { next() }
        val aggregation = newAggregation(
                scanApiProjection(filters),
                match(context.criteria),
                // group("username"),
                // project("_id"),
                // Lookup with pipeline

                // unwind("presence"),
                // match(smartpokeContext.criteria),
                // group("_id")
                //        .addToSet("presence.dateAtZone").`as`("dateAtZone")
                //        .addToSet(parse("{spotId:\"\$presence.spotId\",sensorId:\"\$presence.sensorId\",dateAtZone:\"\$dateAtZone\"}"))
                //        .`as`("root"),
                group("username")
                       .addToSet("dateAtZone").`as`("dateAtZone")
                       .addToSet(parse("{spotId:\"\$spotId\",sensorId:\"\$sensorId\",dateAtZone:\"\$dateAtZone\"}"))
                       .`as`("root"),
                match(where("_id").ne("")),
                project("_id", "root").and("dateAtZone").size().`as`("presence"),
                match(filters.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                        ?: Criteria()),
                unwind("root"),
                project("root.spotId", "root.sensorId").and("_id").`as`("userName")
                        .andExclude("_id"),
                group("spotId", "sensorId", "userName"),
                project("_id.spotId", "_id.sensorId", "_id.userName").andExclude("_id")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
                .map {
                    SmartPokeDevice(
                            spot = it["spotId", ""],
                            sensor = it["sensorId", ""],
                            userName = it["userName", ""]
                    )
                }
    }

    fun avgDwellTime(filters: FilterRequest): Flux<AveragePresence> {
        val context = createContext(filters)
        context.next()
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("dateAtZone", "clientMac")
                .max("seenTime").`as`("maxDwell")
                .min("seenTime").`as`("minDwell"),
            project("_id")
                .andExpression("(maxDwell - minDwell) / 1000").`as`("dwellTime"),
            group().avg("dwellTime").`as`("avgDwellTime")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { AveragePresence(value = it["avgDwellTime", 0.0]) }
    }

    fun getConnectRegister(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val aggregation = newAggregation(
                scanApiProjection(filters),
                match(context.criteria),
                project("dateAtZone", "clientMac")
                        .and(ifNull("username").then("no_username")).`as`("userName")
                        .andExclude("_id"),
                group("dateAtZone", "clientMac", "userName"),
                LookupOperation.newLookup()
                        .from("registeredUser")
                        .localField("_id.userName")
                        .foreignField("info.username")
                        .`as`("userData"),
                unwind("userData", true),
                project("_id")
                        .and("_id.dateAtZone").`as`("dateAtZone")
                        .and("_id.clientMac").`as`("clientMac")
                        .and("_id.userName").`as`("userName")
                        .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$userData.info.seenTime\", timezone: \"${filters.timezone}\" } }")
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
                                        where("_id.dateRegistered")
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
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun getTotalDevicesBigData(filters: FilterRequest): Flux<TotalDevicesBigData> {
        val context = createContext(filters)
        context.next()
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("clientMac")
                .addToSet("dateAtZone").`as`("dateAtZone"),
            project("_id").and("dateAtZone").size().`as`("presence"),
            match(filters.presence?.takeUnless { it.isBlank() }?.toInt()?.let { Criteria("presence").gte(it) }
                ?: Criteria()),
            group().count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { TotalDevicesBigData(count = it["total", 0]) }
    }

    fun getDetailedReport(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria)
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun getDetailedbyUsername(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("groupDate", "username"),
            project("groupDate", "username").andExclude("_id"),
            match(where("username").exists(true))
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun getRadiusDetailedReport(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val radiusContext =
            FilterContext(filterRequest = filters, chain = listOf(RadiusDateFilterBuilder())).apply { next() }
        val aggregation = newAggregation(
                scanApiProjection(filters),
                match(context.criteria),
                group("dateAtZone", "clientMac", "username"),
                LookupOperation.newLookup()
                    .from("radiusActivity")
                    .localField("_id.username")
                    .foreignField("info.username")
                    .`as`("userData"),
                unwind("userData"),
                project("_id")
                    .and("_id.username").`as`("userName")
                    .andExpression("{ \$dateToString: { format: \"%Y-%m-%d %H:%M:%S\", date: \"\$userData.info.eventTimeStamp\", timezone: \"${filters.timezone}\" } }")
                    .`as`("eventTimeStamp")
                    .andExpression("{ \$toLong: \"\$userData.info.acctSessionTime\" }").`as`("acctSessionTime")
                    .andExpression("{ \$toLong: \"\$userData.info.acctInputOctets\" }").`as`("acctInputOctets")
                    .andExpression("{ \$toLong: \"\$userData.info.acctOutputOctets\" }").`as`("acctOutputOctets")
                    .and("userData.info.statusType").`as`("statusType")
                    .and("userData.info.serviceType").`as`("serviceType")
                    .and("userData.info.acctTerminateCause").`as`("acctTerminateCause")
                    .and("userData.info.calledStationId").`as`("calledStationId")
                    .and("userData.info.callingStationId").`as`("callingStationId")
                    .and("userData.info.eventTimeStamp").`as`("dateRadiusAct")
                    .andExclude("_id"),
                match(radiusContext.criteria),
                group("userName", "eventTimeStamp", "acctSessionTime", "acctInputOctets", "acctOutputOctets",
                    "statusType", "serviceType", "acctTerminateCause", "calledStationId", "callingStationId"),
                project( "_id")
                    .and("_id.userName").`as`("userName")
                    .and("_id.eventTimeStamp").`as`("eventTimeStamp")
                    .and("_id.acctSessionTime").`as`("session")
                    .and("_id.acctInputOctets").`as`("inputOct")
                    .and("_id.acctOutputOctets").`as`("outputOct")
                    .and("_id.statusType").`as`("statusType")
                    .and("_id.serviceType").`as`("serviceType")
                    .and("_id.acctTerminateCause").`as`("acctTerminateCause")
                    .and("_id.calledStationId").`as`("calledStationId")
                    .and("_id.callingStationId").`as`("callingStationId")
                    .andExclude("_id"),
                sort(Sort.Direction.ASC, "eventTimeStamp")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun getTotalDevicesTraffic(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val radiusContext =
            FilterContext(filterRequest = filters, chain = listOf(RadiusDateFilterBuilder())).apply { next() }
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            project("dateAtZone", "clientMac")
                .and(ifNull("username").then("no_username")).`as`("userName")
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
                .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$userData.info.eventTimeStamp\", timezone: \"${filters.timezone}\" } }")
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
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun getTotalUsersTime(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val radiusContext =
            FilterContext(filterRequest = filters, chain = listOf(RadiusDateFilterBuilder())).apply { next() }
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            project("dateAtZone", "clientMac")
                .and(ifNull("username").then("no_username")).`as`("userName")
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
                .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$userData.info.eventTimeStamp\", timezone: \"${filters.timezone}\" } }")
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
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    fun getDetailedbyTimeReport(context: FilterContext): Flux<Document> {
        context.next()
        val filters = context.filterRequest
        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("spotId", "sensorId", "groupDate")
                .count().`as`("total"),
            project("spotId", "sensorId", "groupDate", "total").andExclude("_id"),
            sort(Sort.Direction.ASC, "spotId", "sensorId", "groupDate")
        ).withOptions(builder().allowDiskUse(true).build())
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

    fun getTotalDevicesAll() =
        template.findAll(TotalDevicesAll::class.java)
            .map { TotalDevices(it.count, clockService.now()) }

    fun getTotalDevicesToday(filters: FilterRequest): Flux<TotalDevices> {
        val context = createTodayContext(filters).also { it.next() }
        val aggregation = newAggregation(
            match(context.criteria),
            group("clientMac"),
            count().`as`("total")
        ).withOptions(builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { TotalDevices(it.getInteger("total"), clockService.now()) }
    }

    fun todayDetected(filters: FilterRequest): Flux<NowPresence> {
        val context = createTodayContext(filters)
        return find(context)
            .groupBy { it.group }
            .flatMap { g -> groupByTime(g) }
            .sort { o1, o2 -> o1.time.compareTo(o2.time) }
    }

    fun getTodayDevicesGroupedByBrand(zoneId: ZoneId): Flux<List<BrandCount>> {
        val filters = FilterRequest(timezone = zoneId, groupBy = BY_DAY)
        val context = createTodayContext(filters).also { it.next() }

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

    fun getTodayDevicesGroupedByCountry(zoneId: ZoneId): Flux<List<CountryCount>> {
        val filters = FilterRequest(timezone = zoneId, groupBy = BY_DAY)
        val context = createTodayContext(filters).also { it.next() }

        val aggregation = newAggregation(
            scanApiProjection(filters),
            match(context.criteria),
            group("clientMac").addToSet("countryId").`as`("countryId"),
            project("countryId").andExclude("_id"),
            unwind("countryId"),
            group("countryId").count().`as`("count")
        ).withOptions(builder().allowDiskUse(true).build())

        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { CountryCount(it.getString("_id"), it.getInteger("count")) }
            .collectList()
            .toFlux()
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

data class DeviceBigData(val group: String, val clientMac: String, val position: Position)
