package com.esmartit.seendevicesdatastore.v1.application.dashboard.detected

import com.esmartit.seendevicesdatastore.domain.DailyDevices
import com.esmartit.seendevicesdatastore.domain.NowPresence
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.ZoneId

@RestController
@RequestMapping("/sensor-activity/v2")
class DetectedControllerV2 {

    @GetMapping(path = ["/now-detected"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetected(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<List<NowPresence>> {

        TODO()
    }

    @GetMapping(path = ["/now-detected-count"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getNowDetectedCount(
        @RequestParam(name = "timezone", defaultValue = "UTC") zoneId: ZoneId
    ): Flux<DailyDevices> {

        TODO()
    }
}
