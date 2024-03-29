package com.esmartit.seendevicesdatastore.v2.application.filter

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.Position
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import java.time.Instant
import java.time.LocalDate
import java.util.ArrayDeque

abstract class QueryBuilder {
    fun build(context: FilterContext) {
        internalBuild(context)
        context.next()
    }

    protected abstract fun internalBuild(context: FilterContext)
}

data class FilterContext(
    val criteria: Criteria = Criteria(),
    val filterRequest: FilterRequest,
    private val chain: List<QueryBuilder>
) {
    private var internalChain: ArrayDeque<QueryBuilder> = ArrayDeque(chain)

    fun next() {
        if (internalChain.isNotEmpty()) {
            internalChain.pop().build(this)
        }
    }
}

class DateFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val startDate = context.filterRequest.startDate?.takeUnless { it.isBlank() }
            ?.let { LocalDate.parse(it).atStartOfDay(context.filterRequest.timezone).toInstant() }
        val endDate = context.filterRequest.endDate?.takeUnless { it.isBlank() }
            ?.let {
                LocalDate.parse(it).atStartOfDay(context.filterRequest.timezone)
                    .plusDays(1)
                    .minusSeconds(1)
                    .toInstant()
            }
        val criteria = context.criteria
        if (startDate != null && endDate != null) {
            criteria.and("seenTime").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            criteria.and("seenTime").gte(startDate)
        } else if (endDate != null) {
            criteria.and("seenTime").lte(endDate)
        }
    }
}

class HourFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val startHour = context.filterRequest.startTime?.takeUnless { it.isBlank() }?.split(":")?.get(0)?.toInt()
        val endHour = context.filterRequest.endTime?.takeUnless { it.isBlank() }?.split(":")?.get(0)?.toInt()
        val criteria = context.criteria
        if (startHour != null && endHour != null) {
            criteria.and("hourAtZone").gte(startHour).lte(endHour)
        } else if (startHour != null) {
            criteria.and("hourAtZone").gte(startHour)
        } else if (endHour != null) {
            criteria.and("hourAtZone").lte(endHour)
        }
    }
}

class LocationFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val criteria = context.criteria
        context.filterRequest.countryId?.takeUnless { it.isBlank() }?.also { criteria.and("countryId").isEqualTo(it) }
        context.filterRequest.stateId?.takeUnless { it.isBlank() }?.also { criteria.and("stateId").isEqualTo(it) }
        context.filterRequest.cityId?.takeUnless { it.isBlank() }?.also { criteria.and("cityId").isEqualTo(it) }
        context.filterRequest.spotId?.takeUnless { it.isBlank() }?.also { criteria.and("spotId").isEqualTo(it) }
        context.filterRequest.sensorId?.takeUnless { it.isBlank() }?.also { criteria.and("sensorId").isEqualTo(it) }
        context.filterRequest.zone?.takeUnless { it.isBlank() }?.also { criteria.and("zone").isEqualTo(it) }
        context.filterRequest.ssid?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("ssid").`in`(it)
        }
        context.filterRequest.zipCodeId?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("zipCode").`in`(it)
        }
    }
}

class BrandFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val criteria = context.criteria
        context.filterRequest.brands?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("brand").`in`(it)
        }
    }
}

class StatusFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val criteria = context.criteria.and("status")
        context.filterRequest.status?.takeUnless { it.isBlank() }?.also { criteria.isEqualTo(it) }
            ?: criteria.ne(Position.NO_POSITION.name)
    }
}

class UserInfoFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val criteria = context.criteria
        val ageStart = context.filterRequest.ageStart?.takeUnless { it.isBlank() }?.toInt()
        val ageEnd = context.filterRequest.ageEnd?.takeUnless { it.isBlank() }?.toInt()
        if (ageStart != null && ageEnd != null) {
            criteria.and("age").gte(ageStart).lte(ageEnd)
        } else if (ageStart != null) {
            criteria.and("age").gte(ageStart)
        } else if (ageEnd != null) {
            criteria.and("age").lte(ageEnd)
        }
        context.filterRequest.memberShip?.also { criteria.and("memberShip").isEqualTo(it) }
        context.filterRequest.isConnected?.also { criteria.and("isConnected").isEqualTo(it) }
        context.filterRequest.gender?.also { criteria.and("gender").isEqualTo(it) }
        context.filterRequest.zipCode?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("userZipCode").`in`(it)
        }
    }
}

class CustomDateFilterBuilder(private val customDate: Instant) : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        context.criteria.and("seenTime").gte(customDate)
    }
}

class RadiusDateFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val startDate = context.filterRequest.startDate?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterRequest.timezone).toInstant() }
        val endDate = context.filterRequest.endDate?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterRequest.timezone)
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

class SmartPokeDateFilterBuilder : QueryBuilder() {
    override fun internalBuild(context: FilterContext) {
        val startDate = context.filterRequest.startDateP?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterRequest.timezone).toInstant() }
        val endDate = context.filterRequest.endDateP?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterRequest.timezone)
                            .plusDays(1)
                            .minusSeconds(1)
                            .toInstant()
                }
        val criteria = context.criteria
        if (startDate != null && endDate != null) {
            criteria.and("presence.seenTime").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            criteria.and("presence.seenTime").gte(startDate)
        } else if (endDate != null) {
            criteria.and("presence.seenTime").lte(endDate)
        }
    }
}
