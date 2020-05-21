package com.esmartit.seendevicesdatastore.service

import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCount
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountRepository
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountTailable
import com.esmartit.seendevicesdatastore.repository.HourlyDeviceCountTailableRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.function.BiFunction

@Service
class CurrentDayHourlyCounter(
    private val repo: HourlyDeviceCountRepository,
    private val repository: HourlyDeviceCountTailableRepository
) {


    fun getCounters(): Flux<HourlyDeviceCountTailable> {

        val startOfDay = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant()
        val upTillNowFlux = repo.findByTimeGreaterThanEqual(startOfDay).map { convertToTailable(it) }

        val ticker = Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop()
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