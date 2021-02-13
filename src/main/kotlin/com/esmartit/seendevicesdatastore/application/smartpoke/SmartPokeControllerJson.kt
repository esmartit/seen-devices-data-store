package com.esmartit.seendevicesdatastore.application.smartpoke

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.FilterRequest
import com.esmartit.seendevicesdatastore.domain.NowPresence
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.CommonService
import com.esmartit.seendevicesdatastore.services.QueryService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/v3/smartpoke")
class SmartPokeControllerV3(
    private val clock: ClockService,
    private val commonService: CommonService,
    private val scanApiService: ScanApiService,
    private val queryService: QueryService
) {

    @GetMapping(path = ["/today-connected"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyConnected(
        filters: FilterRequest
    ): Mono<NowPresence> {
        return queryService.todayDetected(filters.copy(isConnected = true))
            .last(NowPresence(id = UUID.randomUUID().toString()))
    }

    @GetMapping(path = ["/today-connected-count"], produces = [APPLICATION_JSON_VALUE])
    fun getDailyConnectedCount(
        filters: FilterRequest
    ) = queryService.getTotalDevicesToday(filters.copy(isConnected = true)).toMono()

    @GetMapping(path = ["/now-connected"], produces = [APPLICATION_JSON_VALUE])
    fun getNowConnected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = commonService.timeFlux(zoneId, 30L)
        .filter { it.isConnected }
        .groupBy { it.seenTime.truncatedTo(ChronoUnit.MINUTES) }
        .flatMap { scanApiService.groupByTime(it) }
        .sort { o1, o2 -> o1.time.compareTo(o2.time) }
        .collectList()

    @GetMapping(path = ["/now-connected-count"], produces = [APPLICATION_JSON_VALUE])
    fun getNowConnectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ) = commonService.timeFlux(zoneId, 5L)
        .filter { it.isConnected }
        .map { it.clientMac }
        .distinct()
        .count()
        .map { DailyDevices(it, clock.now()) }

    @GetMapping(path = ["/connected-registered"], produces = [APPLICATION_JSON_VALUE])
    fun getConnectedRegistered(
        filters: FilterRequest
    ): List<DeviceConnectedRegistered>? {
        val createContext = queryService.createContext(filters)
        return queryService.getConnectRegister(createContext).map { DeviceConnectedRegistered(it) }
            .collectList().block()
    }

    @GetMapping(path = ["/total-traffic"], produces = [APPLICATION_JSON_VALUE])
    fun getTraffic(
        filters: FilterRequest
    ): TotalDevicesTraffic? {
        val createContext = queryService.createContext(filters)
        return queryService.getTotalDevicesTraffic(createContext)
            .map { TotalDevicesTraffic(it) }
            .blockLast()
    }

    @GetMapping(path = ["/total-time"], produces = [APPLICATION_JSON_VALUE])
    fun getUserTime(
        filters: FilterRequest
    ): TotalUsersTime? {

        val createContext = queryService.createContext(filters)
        return queryService.getTotalUsersTime(createContext)
            .map { TotalUsersTime(it) }
            .blockLast()
    }

    @GetMapping(path = ["/find"], produces = [APPLICATION_JSON_VALUE])
    fun getFindSmartPoke(
        filters: FilterRequest
    ): List<SmartPokeDevice>? {

        return queryService.findSmartPokeRaw(filters)
            .collectList().block()
    }
}
