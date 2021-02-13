package com.esmartit.seendevicesdatastore.application.dashboard.totaluniquedevices

import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.TotalDevices
import com.esmartit.seendevicesdatastore.services.ClockService
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/v3/sensor-activity")
class TotalDevicesControllerJson(
    private val template: ReactiveMongoTemplate,
    private val clockService: ClockService
) {

    @GetMapping(path = ["/unique-devices-detected-count"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllSensorActivity(): TotalDevices? {

        val aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("status").ne(Position.NO_POSITION)),
            Aggregation.group("clientMac"),
            Aggregation.count().`as`("total")
        )

        return template.aggregate(aggregation, ScanApiActivity::class.java, Document::class.java)
            .map { TotalDevices(it.getInteger("total"), clockService.now()) }
            .blockLast()
    }
}