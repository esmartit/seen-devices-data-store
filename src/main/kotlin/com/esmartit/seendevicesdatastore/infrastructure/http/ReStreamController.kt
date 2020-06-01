package com.esmartit.seendevicesdatastore.infrastructure.http

import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivityReactiveRepository
import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivity
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/sensor-activity")
@EnableBinding(ReStreamOutput::class)
class ReStreamController(
    private val repository: SensorActivityReactiveRepository,
    private val reStreamOutput: ReStreamOutput
) {

    @GetMapping(path = ["/re-stream"])
    fun getAllSensorActivity(): Flux<DeviceSeenEventReStream> {
        return repository.findAll()
            .map { it.toDeviceSeenEvent() }
            .window(Duration.ofMillis(800))
            .flatMap { w -> w.doOnNext { sendEvent(it) } }
            .reduce { _, lastEvent -> lastEvent }
            .flux()
    }

    private fun sendEvent(it: DeviceSeenEventReStream) {
        reStreamOutput.output().send(
            MessageBuilder
                .withPayload(it)
                .setHeader(KafkaHeaders.MESSAGE_KEY, it.device.clientMac.toByteArray())
                .build()
        )
    }
}

private fun SensorActivity.toDeviceSeenEvent(): DeviceSeenEventReStream {
    return DeviceSeenEventReStream(
        accessPoint.macAddress ?: "",
        accessPoint.groupName ?: "",
        accessPoint.hotSpot ?: "",
        accessPoint.sensorName ?: "",
        accessPoint.spotId ?: "",
        DeviceSeenReStream(
            device.macAddress,
            device.ipv4,
            device.ipv6,
            DeviceLocationReStream(location.position[0], location.position[1], location.unc),
            device.manufacturer,
            device.os,
            rssi,
            seenTime.epochSecond.toInt(),
            seenTime.toString(),
            accessPoint.sensorName
        ),
        accessPoint.floors
    )
}

interface ReStreamOutput {
    @Output(RE_STREAM_OUTPUT)
    fun output(): MessageChannel

    companion object {
        const val RE_STREAM_OUTPUT = "restream-output"
    }
}