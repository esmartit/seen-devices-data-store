package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.domain.FilterDailyRequest
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.services.DeviceTotalBigData
import com.esmartit.seendevicesdatastore.services.QueryDailyService
import com.esmartit.seendevicesdatastore.v2.application.filter.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@RestController
@RequestMapping("/bigdata/v2")
class BigDataTotalController(
        private val queryDailyService: QueryDailyService
) {
    @GetMapping(path = ["/find-bigdata"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findBigData(
            dailyFilters: FilterDailyRequest
    ): Flux<BigDataTotalPresenceDevice> {

        val dailyContext = FilterDailyContext(
                filterDailyRequest = dailyFilters,
                chain = listOf(
                        DateFilterDailyBuilder(),
                        LocationFilterDailyBuilder(),
                        BrandFilterDailyBuilder(),
                        StatusFilterDailyBuilder(),
                        UserInfoFilterDailyBuilder()
                )
        )

        return queryDailyService.findBigData(dailyContext)
                .window(Duration.ofMillis(150))
                .flatMap { w -> w.groupBy { it.group }.flatMap { g -> groupByDateDevice(g) } }
                .groupBy { it.group }
                .flatMap { g ->
                    g.scan { t: BigDataTotalPresenceDevice, u: BigDataTotalPresenceDevice ->
                        t.copy(
                                inCount = t.inCount + u.inCount,
                                limitCount = t.limitCount + u.limitCount,
                                outCount = t.outCount + u.outCount
                        )
                    }
                }.concatWith(Mono.just(BigDataTotalPresenceDevice()))
    }

    @GetMapping(path = ["/average-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAveragePresence(
            dailyFilters: FilterDailyRequest
    ): Flux<AverageDailyPresence> {

        return queryDailyService.avgDwellTime(dailyFilters)
                .concatWith(Mono.just(AverageDailyPresence(isLast = true)))
    }

    @GetMapping(path = ["/count-unique-devices"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDevicesBigData(
            dailyFilters: FilterDailyRequest
    ): Flux<TotalDevicesDailyBigData> {

        return queryDailyService.getTotalDevicesBigData(dailyFilters)
                .concatWith(Mono.just(TotalDevicesDailyBigData(isLast = true)))
    }

    fun groupByDateDevice(group: GroupedFlux<String, DeviceTotalBigData>): Mono<BigDataTotalPresenceDevice> {
        return group.reduce(
                BigDataTotalPresenceDevice(
                        id = UUID.randomUUID().toString(),
                        group = group.key()!!,
                        isLast = false
                )
        ) { acc, curr ->
            when (curr.position) {
                Position.IN -> acc.copy(inCount = acc.inCount + 1)
                Position.LIMIT -> acc.copy(limitCount = acc.limitCount + 1)
                Position.OUT -> acc.copy(outCount = acc.outCount + 1)
                Position.NO_POSITION -> acc
            }
        }
    }

}

data class BigDataTotalPresenceDevice(
        val id: String = UUID.randomUUID().toString(),
        val group: String = "",
        val inCount: Long = 0,
        val limitCount: Long = 0,
        val outCount: Long = 0,
        val isLast: Boolean = true
)

data class AverageDailyPresence(val value: Double = 0.0, val isLast: Boolean = false)

data class TotalDevicesDailyBigData(val count: Int = 0, val isLast: Boolean = false)
