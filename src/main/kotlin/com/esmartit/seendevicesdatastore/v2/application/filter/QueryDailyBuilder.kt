package com.esmartit.seendevicesdatastore.v2.application.filter

import com.esmartit.seendevicesdatastore.domain.FilterDailyRequest
import com.esmartit.seendevicesdatastore.domain.Position
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import java.time.Instant
import java.time.LocalDate
import java.util.ArrayDeque

abstract class QueryDailyBuilder {
    fun build(dailyContext: FilterDailyContext) {
        internalBuild(dailyContext)
        dailyContext.next()
    }

    protected abstract fun internalBuild(context: FilterDailyContext)
}

data class FilterDailyContext(
        val criteria: Criteria = Criteria(),
        val filterDailyRequest: FilterDailyRequest,
        private val chain: List<QueryDailyBuilder>
) {
    private var internalChain: ArrayDeque<QueryDailyBuilder> = ArrayDeque(chain)

    fun next() {
        if (internalChain.isNotEmpty()) {
            internalChain.pop().build(this)
        }
    }
}

class DateFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val startDate = context.filterDailyRequest.startDate?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterDailyRequest.timezone).toInstant() }
        val endDate = context.filterDailyRequest.endDate?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterDailyRequest.timezone)
                            .plusDays(1)
                            .minusSeconds(1)
                            .toInstant()
                }
        val criteria = context.criteria
        if (startDate != null && endDate != null) {
            criteria.and("dateAtZone").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            criteria.and("dateAtZone").gte(startDate)
        } else if (endDate != null) {
            criteria.and("dateAtZone").lte(endDate)
        }
    }
}

class LocationFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val criteria = context.criteria
        context.filterDailyRequest.countryId?.takeUnless { it.isBlank() }?.also { criteria.and("countryId").isEqualTo(it) }
        context.filterDailyRequest.stateId?.takeUnless { it.isBlank() }?.also { criteria.and("stateId").isEqualTo(it) }
        context.filterDailyRequest.cityId?.takeUnless { it.isBlank() }?.also { criteria.and("cityId").isEqualTo(it) }
        context.filterDailyRequest.spotId?.takeUnless { it.isBlank() }?.also { criteria.and("spotId").isEqualTo(it) }
        context.filterDailyRequest.sensorId?.takeUnless { it.isBlank() }?.also { criteria.and("sensorId").isEqualTo(it) }
        context.filterDailyRequest.zone?.takeUnless { it.isBlank() }?.also { criteria.and("zone").isEqualTo(it) }
        context.filterDailyRequest.ssid?.takeUnless { it.isBlank() }?.also { criteria.and("ssid").isEqualTo(it) }
        context.filterDailyRequest.zipCodeId?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("zipCode").`in`(it)
        }
    }
}

class BrandFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val criteria = context.criteria
        context.filterDailyRequest.brands?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("brand").`in`(it)
        }
    }
}

class StatusFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val criteria = context.criteria.and("status")
        context.filterDailyRequest.status?.takeUnless { it.isBlank() }?.also { criteria.isEqualTo(it) }
                ?: criteria.ne(Position.NO_POSITION.name)
    }
}

class UserInfoFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val criteria = context.criteria
        val ageStart = context.filterDailyRequest.ageStart?.takeUnless { it.isBlank() }?.toInt()
        val ageEnd = context.filterDailyRequest.ageEnd?.takeUnless { it.isBlank() }?.toInt()
        if (ageStart != null && ageEnd != null) {
            criteria.and("age").gte(ageStart).lte(ageEnd)
        } else if (ageStart != null) {
            criteria.and("age").gte(ageStart)
        } else if (ageEnd != null) {
            criteria.and("age").lte(ageEnd)
        }
        context.filterDailyRequest.memberShip?.also { criteria.and("memberShip").isEqualTo(it) }
        context.filterDailyRequest.gender?.also { criteria.and("gender").isEqualTo(it) }
        context.filterDailyRequest.zipCode?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("userZipCode").`in`(it)
        }
    }
}

class CustomDateFilterDailyBuilder(private val customDate: Instant) : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        context.criteria.and("dateAtZone").gte(customDate)
    }
}

class RadiusDateFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val startDate = context.filterDailyRequest.startDate?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterDailyRequest.timezone).toInstant() }
        val endDate = context.filterDailyRequest.endDate?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterDailyRequest.timezone)
                            .plusDays(1)
                            .minusSeconds(1)
                            .toInstant()
                }
        val criteria = context.criteria
        if (startDate != null && endDate != null) {
            criteria.and("dateRadiusAct").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            criteria.and("dateRadiusAct").gte(startDate)
        } else if (endDate != null) {
            criteria.and("dateRadiusAct").lte(endDate)
        }
    }
}

class SmartPokeDateFilterDailyBuilder : QueryDailyBuilder() {
    override fun internalBuild(context: FilterDailyContext) {
        val startDate = context.filterDailyRequest.startDateP?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterDailyRequest.timezone).toInstant() }
        val endDate = context.filterDailyRequest.endDateP?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterDailyRequest.timezone)
                            .plusDays(1)
                            .minusSeconds(1)
                            .toInstant()
                }
        val criteria = context.criteria
        if (startDate != null && endDate != null) {
            criteria.and("presence.dateAtZone").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            criteria.and("presence.dateAtZone").gte(startDate)
        } else if (endDate != null) {
            criteria.and("presence.dateAtZone").lte(endDate)
        }
    }
}