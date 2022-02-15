package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.*
import com.esmartit.seendevicesdatastore.services.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit


@RestController
@RequestMapping("/smartpoke/v2")

class SmartPokeTotalController(
        private val clock: ClockService,
        private val commonDailyService: CommonDailyService,
        private val scanApiDailyService: ScanApiDailyService,
        private val queryDailyService: QueryDailyService

) {
    @GetMapping(path = ["/today-connected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getDailyConnectedCount(
            filtersDaily: FilterDailyRequest
    ): Flux<TotalDevices> {
        val isConnected = filtersDaily.copy(isConnected = true)
        return Flux.interval(Duration.ofSeconds(0), Duration.ofSeconds(15)).flatMap { queryDailyService.getTotalDevicesToday(isConnected) }
    }

    @GetMapping(path = ["/connected-registered"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getConnectedRegistered(
            filtersDaily: FilterDailyRequest
    ): Flux<MutableList<DeviceConnectedRegistered>> {

        val createContext = queryDailyService.createContext(filtersDaily)
        return queryDailyService.getConnectRegister(createContext)
                .map { DeviceConnectedRegistered(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(DeviceConnectedRegistered(isLast = true))))
    }

    @GetMapping(path = ["/total-traffic"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getTraffic(
            filtersDaily: FilterDailyRequest
    ): Flux<MutableList<TotalDevicesTraffic>> {

        val createContext = queryDailyService.createContext(filtersDaily)
        return queryDailyService.getTotalDevicesTraffic(createContext)
                .map { TotalDevicesTraffic(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(TotalDevicesTraffic(isLast = true))))
    }

    @GetMapping(path = ["/total-time"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getUserTime(
            filtersDaily: FilterDailyRequest
    ): Flux<MutableList<TotalUsersTime>> {

        val createContext = queryDailyService.createContext(filtersDaily)
        return queryDailyService.getTotalUsersTime(createContext)
                .map { TotalUsersTime(it) }
                .buffer(500)
                .concatWith(Mono.just(listOf(TotalUsersTime(isLast = true))))
    }

    @GetMapping(path = ["/find-offline"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun findTotalSmartPokeRaw(
            filtersDaily: FilterDailyRequest
    ): Flux<SmartPokeDailyDevice> {

        return queryDailyService.findTotalSmartPokeRaw(filtersDaily)
                .concatWith(Mono.just(SmartPokeDailyDevice( isLast = true)))
    }

}
data class SmartPokeDailyDevice (
        val spot: String = "",
        val sensor: String = "",
        val userName: String = "",
        val isLast: Boolean = false
)