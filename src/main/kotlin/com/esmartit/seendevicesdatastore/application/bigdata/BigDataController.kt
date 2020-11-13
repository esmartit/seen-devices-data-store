package com.esmartit.seendevicesdatastore.application.bigdata

import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.services.DeviceAndPosition
import com.esmartit.seendevicesdatastore.services.DeviceBigData
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.v2.application.filter.BrandFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.DateFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.FilterContext
import com.esmartit.seendevicesdatastore.v2.application.filter.HourFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.LocationFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.StatusFilterBuilder
import com.esmartit.seendevicesdatastore.v2.application.filter.UserInfoFilterBuilder
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.UUID


@RestController
@RequestMapping("/bigdata")
class BigDataController(
        private val queryService: QueryService
) {

    @GetMapping(path = ["/find"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnected(
            filters: FilterRequest
    ): Flux<BigDataPresence> {

        val context = FilterContext(
                filterRequest = filters,
                chain = listOf(
                        DateFilterBuilder(),
                        HourFilterBuilder(),
                        LocationFilterBuilder(),
                        BrandFilterBuilder(),
                        StatusFilterBuilder(),
                        UserInfoFilterBuilder()
                )
        )

        return queryService.find(context)
                .window(Duration.ofMillis(150))
                .flatMap { w -> w.groupBy { it.group }.flatMap { g -> groupByTime(g) } }
                .groupBy { it.group }
                .flatMap { g ->
                    g.scan { t: BigDataPresence, u: BigDataPresence ->
                        t.copy(
                                inCount = t.inCount + u.inCount,
                                limitCount = t.limitCount + u.limitCount,
                                outCount = t.outCount + u.outCount
                        )
                    }
                }.concatWith(Mono.just(BigDataPresence()))
    }

    @GetMapping(path = ["/find-bigdata"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getFindBigData(
            filters: FilterRequest
    ): Flux<BigDataPresenceDevice> {

        val context = FilterContext(
                filterRequest = filters,
                chain = listOf(
                        DateFilterBuilder(),
                        HourFilterBuilder(),
                        LocationFilterBuilder(),
                        BrandFilterBuilder(),
                        StatusFilterBuilder(),
                        UserInfoFilterBuilder()
                )
        )

        return queryService.findBigData(context)
                .window(Duration.ofMillis(150))
                .flatMap { w -> w.groupBy { it.group }.flatMap { g -> groupByTimeDevice(g) } }
                .groupBy { it.group }
                .flatMap { g ->
                    g.scan { t: BigDataPresenceDevice, u: BigDataPresenceDevice ->
                        t.copy(
                                inCount = t.inCount + u.inCount,
                                limitCount = t.limitCount + u.limitCount,
                                outCount = t.outCount + u.outCount
                        )
                    }
                }.concatWith(Mono.just(BigDataPresenceDevice()))
    }

    @GetMapping(path = ["/average-presence"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getAveragePresence(
            filters: FilterRequest
    ): Flux<AveragePresence> {

        return queryService.avgDwellTime(filters)
                .concatWith(Mono.just(AveragePresence(isLast = true)))
    }

    @GetMapping(path = ["/count-unique-devices"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDevicesBigData(
            filters: FilterRequest
    ): Flux<TotalDevicesBigData> {

        return queryService.getTotalDevicesBigData(filters)
            .concatWith(Mono.just(TotalDevicesBigData(isLast = true)))
    }


    fun groupByTime(group: GroupedFlux<String, DeviceAndPosition>): Mono<BigDataPresence> {
        return group.reduce(
                BigDataPresence(
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

    fun groupByTimeDevice(group: GroupedFlux<String, DeviceBigData>): Mono<BigDataPresenceDevice> {
        return group.reduce(
                BigDataPresenceDevice(
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

data class BigDataPresence(
        val id: String = UUID.randomUUID().toString(),
        val group: String = "",
        val inCount: Long = 0,
        val limitCount: Long = 0,
        val outCount: Long = 0,
        val isLast: Boolean = true
)

data class BigDataPresenceDevice(
        val id: String = UUID.randomUUID().toString(),
        val group: String = "",
        val inCount: Long = 0,
        val limitCount: Long = 0,
        val outCount: Long = 0,
        val isLast: Boolean = true
)

data class AveragePresence(val value: Double = 0.0, val isLast: Boolean = false)

data class TotalDevicesBigData(val count: Int = 0, val isLast: Boolean = false)
