package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.filter.BrandFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.DateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import com.esmartit.seendevicesdatastore.v2.application.filter.HourFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.LocationFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.QueryBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.StatusFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.UserInfoFilterBuilder
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.CaseOperator.`when`
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.switchCases
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import java.time.LocalDate
import java.time.ZoneId
import java.util.ArrayDeque

@RestController
@RequestMapping("/test")
class TestController(
    private val template: ReactiveMongoTemplate
) {

    @GetMapping(path = ["/total-devices-all"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun totalCount(filters: FilterRequest): Flux<Document> {
        val filterContext = FilterContext(
            filterRequest = filters,
            chain = ArrayDeque<QueryBuilder>(
                listOf(
                    DateFilterBuilder(),
                    HourFilterBuilder(),
                    LocationFilterBuilder(),
                    BrandFilterBuilder(),
                    StatusFilterBuilder(),
                    UserInfoFilterBuilder()
                )
            )
        )
        filterContext.next(filterContext)
        val aggregation = newAggregation(
            project(ScanApiActivity::class.java)
                .andExclude("_id")
                .andExpression("{\$hour: { date: \"\$seenTime\", timezone: \"${filters.timezone}\" }}")
                .`as`("hourAtZone")
                .andExpression("{\$toDate: { \$dateToString: { format: \"%Y-%m-%d\", date: \"\$seenTime\", timezone: \"${filters.timezone}\" } }   } ")
                .`as`("dateAtZone")
                .and(
                    switchCases(
                        `when`(ComparisonOperators.Eq.valueOf("status").equalToValue("OUT")).then(0),
                        `when`(ComparisonOperators.Eq.valueOf("status").equalToValue("LIMIT")).then(1),
                        `when`(ComparisonOperators.Eq.valueOf("status").equalToValue("IN")).then(2)
                    ).defaultTo(-1)
                )
                .`as`("statusNumeral"),
            match(
                filterContext.criteria
            ),
//            group("dateAtZone", "clientMac")
//                .max("statusNumeral").`as`("statusNumeral"),
            group("dateAtZone", "clientMac")
                .addToSet("brand").`as`("brand")
                .max("statusNumeral").`as`("statusNumeral")
                .count().`as`("seenTimes"),
//            project("_id.dateAtZone", "statusNumeral").andExclude("_id"),
//            group("dateAtZone").push("statusNumeral").`as`("statusNumeral"),
//            project("_id").and { Document.parse("{\"\$reduce\":{\"input\":\"\$statusNumeral\",\"initialValue\":{\"in\":0,\"limit\":0,\"out\":0},\"in\":{\"in\":{\"\$cond\":{\"if\":{\"\$eq\":[\"\$\$this\",2]},\"then\":{\"\$add\":[\"\$\$value.in\",1]},\"else\":\"\$\$value.in\"}},\"limit\":{\"\$cond\":{\"if\":{\"\$eq\":[\"\$\$this\",1]},\"then\":{\"\$add\":[\"\$\$value.limit\",1]},\"else\":\"\$\$value.limit\"}},\"out\":{\"\$cond\":{\"if\":{\"\$eq\":[\"\$\$this\",0]},\"then\":{\"\$add\":[\"\$\$value.out\",1]},\"else\":\"\$\$value.out\"}}}}}") }
//                .`as`("presence"),
            sort(Sort.Direction.ASC, "_id")
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }

    @GetMapping(path = ["/total-devices-today"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun todayCount(
        @RequestParam("timezone", defaultValue = "UTC") timezone: ZoneId
    ): Flux<Long> {

        val startOfDay = LocalDate.now().atStartOfDay(timezone).toInstant()

        val query = Query.query(
            where("status").ne(NO_POSITION)
                .and("seenTime").gte(startOfDay)
        )
        return template.findDistinct(
            query,
            "clientMac",
            ScanApiActivity::class.java,
            ScanApiActivity::class.java
        ).count().toFlux()
    }
}

