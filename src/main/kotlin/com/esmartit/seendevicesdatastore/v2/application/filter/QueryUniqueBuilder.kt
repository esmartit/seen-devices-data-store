package com.esmartit.seendevicesdatastore.v2.application.filter

import com.esmartit.seendevicesdatastore.domain.FilterUniqueRequest
import org.springframework.data.mongodb.core.query.Criteria
import java.time.LocalDate
import java.util.ArrayDeque

abstract class QueryUniqueBuilder {
    fun build(uniqueContext: FilterUniqueContext) {
        internalBuild(uniqueContext)
        uniqueContext.next()
    }

    protected abstract fun internalBuild(uniqueContext: FilterUniqueContext)
}

data class FilterUniqueContext(
        val criteria: Criteria = Criteria(),
        val filterUniqueRequest: FilterUniqueRequest,
        private val chain: List<QueryUniqueBuilder>
) {
    private var internalChain: ArrayDeque<QueryUniqueBuilder> = ArrayDeque(chain)

    fun next() {
        if (internalChain.isNotEmpty()) {
            internalChain.pop().build(this)
        }
    }
}

class DateFilterUniqueBuilder : QueryUniqueBuilder() {
    override fun internalBuild(uniqueContext: FilterUniqueContext) {
        val startDate = uniqueContext.filterUniqueRequest.startDate?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(uniqueContext.filterUniqueRequest.timezone).toInstant() }
        val endDate = uniqueContext.filterUniqueRequest.endDate?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(uniqueContext.filterUniqueRequest.timezone)
                            .plusDays(1)
                            .minusSeconds(1)
                            .toInstant()
                }
        val criteria = uniqueContext.criteria
        if (startDate != null && endDate != null) {
            criteria.and("seenTime").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            criteria.and("seenTime").gte(startDate)
        } else if (endDate != null) {
            criteria.and("seenTime").lte(endDate)
        }
    }
}

class HourFilterUniqueBuilder : QueryUniqueBuilder() {
    override fun internalBuild(uniqueContext: FilterUniqueContext) {
        val startHour = uniqueContext.filterUniqueRequest.startTime?.takeUnless { it.isBlank() }?.split(":")?.get(0)?.toInt()
        val endHour = uniqueContext.filterUniqueRequest.endTime?.takeUnless { it.isBlank() }?.split(":")?.get(0)?.toInt()
        val criteria = uniqueContext.criteria
        if (startHour != null && endHour != null) {
            criteria.and("hourAtZone").gte(startHour).lte(endHour)
        } else if (startHour != null) {
            criteria.and("hourAtZone").gte(startHour)
        } else if (endHour != null) {
            criteria.and("hourAtZone").lte(endHour)
        }
    }
}


