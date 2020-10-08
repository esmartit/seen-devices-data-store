package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.count
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
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
    private val reactiveMongoTemplate: ReactiveMongoTemplate
) {

    @GetMapping(path = ["/total-devices-all"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun totalCount(): Flux<Document> {

        val aggregation = newAggregation(
            match(where("status").ne(NO_POSITION)),
            group("clientMac"),
            count().`as`("total")
        )

        return reactiveMongoTemplate.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
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
        return reactiveMongoTemplate.findDistinct(
            query,
            "clientMac",
            ScanApiActivity::class.java,
            ScanApiActivity::class.java
        ).count().toFlux()
    }
}

