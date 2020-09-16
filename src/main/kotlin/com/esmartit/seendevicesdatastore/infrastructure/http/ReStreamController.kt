package com.esmartit.seendevicesdatastore.infrastructure.http

import com.esmartit.seendevicesdatastore.domain.SensorActivity
import com.esmartit.seendevicesdatastore.application.sensoractivity.SensorActivityReactiveRepository
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
    fun getAllSensorActivity(): Flux<SensorActivity> {
        return repository.findAll()
            .window(Duration.ofMillis(800))
            .flatMap { w -> w.doOnNext { sendEvent(it) } }
            .reduce { _, lastEvent -> lastEvent }
            .flux()
    }

    private fun sendEvent(it: SensorActivity) {
        reStreamOutput.output().send(
            MessageBuilder
                .withPayload(it)
                .setHeader(KafkaHeaders.MESSAGE_KEY, it.device.clientMac.toByteArray())
                .build()
        )
    }
}

interface ReStreamOutput {
    @Output(RE_STREAM_OUTPUT)
    fun output(): MessageChannel

    companion object {
        const val RE_STREAM_OUTPUT = "restream-output"
    }
}