package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.application.smartpoke.SmartPokeDevice
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.v2.application.filter.BrandFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.DateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import com.esmartit.seendevicesdatastore.v2.application.filter.HourFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.LocationFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.StatusFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.UserInfoFilterBuilder
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
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

@RestController
@RequestMapping("/test")
class TestController(
    private val template: ReactiveMongoTemplate,
    private val queryService: QueryService
) {

    @GetMapping(path = ["/total-devices-all"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun totalCount(filters: FilterRequest): Flux<SmartPokeDevice> {
        return queryService.findSmartPokeRaw(filters)
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
