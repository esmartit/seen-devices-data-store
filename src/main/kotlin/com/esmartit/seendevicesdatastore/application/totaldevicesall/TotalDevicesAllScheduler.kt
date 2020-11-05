package com.esmartit.seendevicesdatastore.application.totaldevicesall

import com.esmartit.seendevicesdatastore.domain.Position.NO_POSITION
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.TotalDevicesAll
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.count
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TotalDevicesAllScheduler(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(TotalDevicesAllScheduler::class.java)

    @Scheduled(fixedDelay = 15000)
    @SchedulerLock(
        name = "processTotalDevicesAll",
        lockAtMostFor = "5m",
        lockAtLeastFor = "15s"
    )
    fun process() {
        logger.info("processTotalDevicesAll...")
        newAggregation(
            match(where("status").ne(NO_POSITION)),
            group("clientMac"),
            count().`as`("total")
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())
            .run {
                template.aggregate(this, ScanApiActivity::class.java, Document::class.java)
                    .map { TotalDevicesAll(count = it.getInteger("total")) }
                    .flatMap { template.save(it) }
                    .log()
                    .blockLast()
            }
    }
}