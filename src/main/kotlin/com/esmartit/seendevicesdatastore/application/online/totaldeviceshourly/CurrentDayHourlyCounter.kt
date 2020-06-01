package com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly

import com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly.HourlyDeviceCount
import com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly.HourlyDeviceCountRepository
import com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly.HourlyDeviceCountTailable
import com.esmartit.seendevicesdatastore.application.online.totaldeviceshourly.HourlyDeviceCountTailableRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.function.BiFunction

@Service
class CurrentDayHourlyCounter(
    private val repo: HourlyDeviceCountRepository,
    private val repository: HourlyDeviceCountTailableRepository
) {


    fun getCounters(zoneId: ZoneId): Flux<HourlyDeviceCountTailable> {

        val startOfDay = LocalDate.now().atStartOfDay(zoneId).toInstant()
        val upTillNowFlux = repo.findByTimeGreaterThanEqualOrderByTimeAsc(startOfDay).map { convertToTailable(it) }

        val ticker = Flux.interval(Duration.ofSeconds(5)).onBackpressureDrop()
        val counter = repository.findWithTailableCursorBy()
        val fromNowOnFlux =
            Flux.combineLatest(ticker, counter, BiFunction { _: Long, b: HourlyDeviceCountTailable -> b })

        return Flux.concat(upTillNowFlux, fromNowOnFlux)
    }

    private fun convertToTailable(it: HourlyDeviceCount): HourlyDeviceCountTailable {
        return HourlyDeviceCountTailable(
            id = it.id,
            time = it.time,
            inCount = it.inCount,
            limitCount = it.limitCount,
            outCount = it.outCount
        )
    }
}