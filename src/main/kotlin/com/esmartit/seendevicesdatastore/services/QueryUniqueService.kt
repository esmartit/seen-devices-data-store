package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.dashboard.detected.FilterGroup
import com.esmartit.seendevicesdatastore.domain.FilterUniqueRequest
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.domain.UniqueDevice
import com.esmartit.seendevicesdatastore.v2.application.filter.DateFilterUniqueBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterUniqueContext
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class QueryUniqueService(
        private val template: ReactiveMongoTemplate,
        private val clockService: ClockService
) {
    fun getNewDevicesToday(uniqueFilters: FilterUniqueRequest): Flux<TotalDevices> {
        val uniqueContext = createUniqueContext(uniqueFilters).also { it.next() }
        val aggregation = Aggregation.newAggregation(
                uniqueApiProjection(uniqueFilters),
                match(uniqueContext.criteria),
                group("dateAtZone").count().`as`("total")
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())
        return template.aggregate(aggregation, UniqueDevice::class.java, Document::class.java)
                .map { TotalDevices(it.getInteger("total"), clockService.now()) }
    }

    private fun uniqueApiProjection(filtersUnique: FilterUniqueRequest): ProjectionOperation {

        val format = when (filtersUnique.groupBy) {
            FilterGroup.BY_DAY -> "%Y-%m-%d"
            FilterGroup.BY_WEEK -> "%V"
            FilterGroup.BY_MONTH -> "%Y-%m"
            FilterGroup.BY_YEAR -> "%Y"
        }

        return Aggregation.project(UniqueDevice::class.java)
                .andExclude("_id")
                .andExpression("\"\$_id\"").`as`("clientMac")
                .andExpression("{ \$dateToString: { format: \"%Y-%m-%d\", date: \"\$seenTime\", timezone: \"${filtersUnique.timezone}\" } }")
                .`as`("dateAtZone")
                .andExpression("{ \$dateToString: { format: \"$format\", date: \"\$seenTime\" } }")
                .`as`("groupDate")
    }

    fun createUniqueContext(uniqueFilters: FilterUniqueRequest): FilterUniqueContext {
        return FilterUniqueContext(
                filterUniqueRequest = uniqueFilters,
                chain = listOf(
                        DateFilterUniqueBuilder()
                )
        )
    }


}