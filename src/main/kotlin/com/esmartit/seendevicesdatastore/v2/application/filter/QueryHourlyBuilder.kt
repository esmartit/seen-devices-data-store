package com.esmartit.seendevicesdatastore.v2.application.filter

import com.esmartit.seendevicesdatastore.domain.FilterHourlyRequest
import com.esmartit.seendevicesdatastore.domain.Position
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import java.time.Instant
import java.time.LocalDate
import java.util.ArrayDeque

abstract class QueryHourlyBuilder {
    fun build(hourlyContext: FilterHourlyContext) {
        internalBuild(hourlyContext)
        hourlyContext.next()
    }

    protected abstract fun internalBuild(context: FilterHourlyContext)
}

data class FilterHourlyContext(
        val criteria: Criteria = Criteria(),
        val filterHourlyRequest: FilterHourlyRequest,
        private val chain: List<QueryHourlyBuilder>
) {
    private var internalChain: ArrayDeque<QueryHourlyBuilder> = ArrayDeque(chain)

    fun next() {
        if (internalChain.isNotEmpty()) {
            internalChain.pop().build(this)
        }
    }
}

class DateFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val startDate = context.filterHourlyRequest.startDate?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterHourlyRequest.timezone).toInstant() }
        val endDate = context.filterHourlyRequest.endDate?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterHourlyRequest.timezone)
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

class HourFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val startHour = context.filterHourlyRequest.startTime?.takeUnless { it.isBlank() }?.split(":")?.get(0)?.toInt()
        val endHour = context.filterHourlyRequest.endTime?.takeUnless { it.isBlank() }?.split(":")?.get(0)?.toInt()
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

class LocationFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val criteria = context.criteria
        context.filterHourlyRequest.countryId?.takeUnless { it.isBlank() }?.also { criteria.and("countryId").isEqualTo(it) }
        context.filterHourlyRequest.stateId?.takeUnless { it.isBlank() }?.also { criteria.and("stateId").isEqualTo(it) }
        context.filterHourlyRequest.cityId?.takeUnless { it.isBlank() }?.also { criteria.and("cityId").isEqualTo(it) }
        context.filterHourlyRequest.spotId?.takeUnless { it.isBlank() }?.also { criteria.and("spotId").isEqualTo(it) }
        context.filterHourlyRequest.sensorId?.takeUnless { it.isBlank() }?.also { criteria.and("sensorId").isEqualTo(it) }
        context.filterHourlyRequest.zone?.takeUnless { it.isBlank() }?.also { criteria.and("zone").isEqualTo(it) }
        context.filterHourlyRequest.ssid?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("ssid").`in`(it)
        }
        context.filterHourlyRequest.zipCodeId?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("zipCode").`in`(it)
        }
    }
}

class BrandFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val criteria = context.criteria
        context.filterHourlyRequest.brands?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("brand").`in`(it)
        }
    }
}

class StatusFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val criteria = context.criteria.and("status")
        context.filterHourlyRequest.status?.takeUnless { it.isBlank() }?.also { criteria.isEqualTo(it) }
                ?: criteria.ne(Position.NO_POSITION.name)
    }
}

class UserInfoFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val criteria = context.criteria
        val ageStart = context.filterHourlyRequest.ageStart?.takeUnless { it.isBlank() }?.toInt()
        val ageEnd = context.filterHourlyRequest.ageEnd?.takeUnless { it.isBlank() }?.toInt()
        if (ageStart != null && ageEnd != null) {
            criteria.and("age").gte(ageStart).lte(ageEnd)
        } else if (ageStart != null) {
            criteria.and("age").gte(ageStart)
        } else if (ageEnd != null) {
            criteria.and("age").lte(ageEnd)
        }
        context.filterHourlyRequest.memberShip?.also { criteria.and("memberShip").isEqualTo(it) }
        context.filterHourlyRequest.isConnected?.also { criteria.and("isConnected").isEqualTo(it) }
        context.filterHourlyRequest.gender?.also { criteria.and("gender").isEqualTo(it) }
        context.filterHourlyRequest.zipCode?.takeUnless { it.isBlank() }?.split(",")?.also {
            criteria.and("userZipCode").`in`(it)
        }
    }
}

class CustomDateFilterHourlyBuilder(private val customDate: Instant) : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        context.criteria.and("dateAtZone").gte(customDate)
    }
}

class RadiusDateFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val startDate = context.filterHourlyRequest.startDate?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterHourlyRequest.timezone).toInstant() }
        val endDate = context.filterHourlyRequest.endDate?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterHourlyRequest.timezone)
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

class SmartPokeDateFilterHourlyBuilder : QueryHourlyBuilder() {
    override fun internalBuild(context: FilterHourlyContext) {
        val startDate = context.filterHourlyRequest.startDateP?.takeUnless { it.isBlank() }
                ?.let { LocalDate.parse(it).atStartOfDay(context.filterHourlyRequest.timezone).toInstant() }
        val endDate = context.filterHourlyRequest.endDateP?.takeUnless { it.isBlank() }
                ?.let {
                    LocalDate.parse(it).atStartOfDay(context.filterHourlyRequest.timezone)
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