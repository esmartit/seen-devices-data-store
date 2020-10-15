package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_DAY
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_HOUR
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MINUTE
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_MONTH
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_WEEK
import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterDateGroup.BY_YEAR
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import org.bson.Document
import org.bson.Document.parse
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.aggregation.Aggregation.unwind
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq.valueOf
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.CaseOperator.`when`
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Switch.switchCases
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class QueryService(private val template: ReactiveMongoTemplate) {
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

        val format = when (filters.groupBy) {
            BY_MINUTE -> "%Y-%m-%dT%H:%M"
            BY_HOUR -> "%Y-%m-%dT%H:00"
            BY_DAY -> "%Y-%m-%d"
            BY_WEEK -> "%V"
            BY_MONTH -> "%Y-%m"
            BY_YEAR -> "%Y"
        }

        context.next(context)
        val aggregation = newAggregation(
            project(ScanApiActivity::class.java)
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
                ).`as`("statusNumeral"),
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
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
    }
}

data class DeviceAndPosition(val group: String, val clientMac: String, val position: Position)