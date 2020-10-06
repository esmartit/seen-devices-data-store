package com.esmartit.seendevicesdatastore.infrastructure.http

import com.esmartit.seendevicesdatastore.application.scanapi.minute.ScanApiConsumer
import com.esmartit.seendevicesdatastore.application.scanapi.minute.ScanApiStoreService
import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivityReactiveRepository
import com.esmartit.seendevicesdatastore.domain.SensorActivity
import com.esmartit.seendevicesdatastore.domain.incomingevents.DeviceLocation
import com.esmartit.seendevicesdatastore.domain.incomingevents.DeviceSeen
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.http.MediaType
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime

@RestController
@RequestMapping("/sensor-activity")
@EnableBinding(ReStreamOutput::class)
class ReStreamController(
    private val repository: SensorActivityReactiveRepository,
    private val reStreamOutput: ReStreamOutput,
    private val consumer: ScanApiConsumer,
    private val scanApiStoreService: ScanApiStoreService
) {

    @GetMapping(path = ["/re-stream"])
    fun getAllSensorActivity(): Flux<SensorActivity> {
        return repository.findAll()
            .window(Duration.ofMillis(800))
            .flatMap { w -> w.doOnNext { sendEvent(it) } }
            .reduce { _, lastEvent -> lastEvent }
            .flux()
    }

    @GetMapping(path = ["/process"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun process(@RequestParam(name = "limit", defaultValue = "100") limit: Long): Flux<Int> {
        return repository.findByProcessed(false).limitRequest(limit)
            .doOnSubscribe { println(LocalDateTime.now()) }
            .flatMap { activity ->
                scanApiStoreService.save(consumer.handle(activity.toEvent()))
                    .flatMap { repository.save(activity.copy(processed = true)) }
            }.scan(0) { acc, _ -> acc + 1 }
            .doFinally { println(LocalDateTime.now()) }
    }

    private fun sendEvent(it: SensorActivity) {
        reStreamOutput.output().send(
            MessageBuilder
                .withPayload(it)
                .setHeader(KafkaHeaders.MESSAGE_KEY, it.device.macAddress.toByteArray())
                .build()
        )
    }
}

private fun SensorActivity.toEvent(): SensorActivityEvent {
    return SensorActivityEvent(
        apMac = this.accessPoint.macAddress ?: "",
        device = DeviceSeen(
            clientMac = this.device.macAddress,
            seenTime = this.seenTime,
            rssi = this.rssi,
            seenEpoch = this.seenTime.epochSecond.toInt(),
            manufacturer = this.device.manufacturer,
            os = this.device.os,
            ipv4 = this.device.ipv4,
            ipv6 = this.device.ipv6,
            ssid = this.ssid,
            location = DeviceLocation(
                lat = null,
                lng = null,
                unc = this.location.unc,
                x = emptyList(),
                y = emptyList()
            )
        ),
        apFloors = emptyList()
    )
}

interface ReStreamOutput {
    @Output(RE_STREAM_OUTPUT)
    fun output(): MessageChannel

    companion object {
        const val RE_STREAM_OUTPUT = "restream-output"
    }
}