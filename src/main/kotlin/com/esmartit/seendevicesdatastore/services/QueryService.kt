package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_DAY
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_HOUR
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MINUTE
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MONTH
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_WEEK
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_YEAR
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.Position
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
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq.valueOf
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.CaseOperator.`when`
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.switchCases
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.ArrayDeque

@Component
class QueryService(private val template: ReactiveMongoTemplate) {

    fun find(filters: FilterRequest): Flux<DeviceAndPosition> {
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

        val format = when (filters.groupBy) {
            BY_MINUTE -> "%Y-%m-%dT%H:%M"
            BY_HOUR -> "%Y-%m-%dT%H"
            BY_DAY -> "%Y-%m-%d"
            BY_WEEK -> "%V"
            BY_MONTH -> "%Y-%m"
            BY_YEAR -> "%Y"
        }

        filterContext.next(filterContext)
        val aggregation = newAggregation(
            project(ScanApiActivity::class.java)
                .andExclude("_id")
                .andExpression("{\$hour: { date: \"\$seenTime\", timezone: \"${filters.timezone}\" }}")
                .`as`("hourAtZone")
                .andExpression("{ \$dateToString: { format: \"$format\", date: \"\$seenTime\", timezone: \"${filters.timezone}\" } }")
                .`as`("dateAtZone")
                .and(
                    switchCases(
                        `when`(valueOf("status").equalToValue("OUT")).then(1),
                        `when`(valueOf("status").equalToValue("LIMIT")).then(2),
                        `when`(valueOf("status").equalToValue("IN")).then(3)
                    ).defaultTo(-1)
                ).`as`("statusNumeral"),
            match(filterContext.criteria),
            group("dateAtZone", "clientMac").max("statusNumeral").`as`("statusNumeral"),
            sort(Sort.Direction.ASC, "_id"),
            project("_id.dateAtZone", "_id.clientMac", "statusNumeral").andExclude("_id")
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map {
                DeviceAndPosition(
                    it["dateAtZone", ""],
                    it["clientMac", ""],
                    Position.byValue(it["statusNumeral", 0])
                )
            }
    }
}

data class DeviceAndPosition(val group: String, val clientMac: String, val position: Position)