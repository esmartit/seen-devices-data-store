package com.esmartit.seendevicesdatastore.application.radius.online

import com.esmartit.seendevicesdatastore.application.incomingevents.FreeRadiusEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel
import java.time.Instant
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@EnableBinding(RadiusActivityInput::class)
class RadiusActivityConsumer(
    private val repository: RadiusActivityRepository
) {

    @StreamListener(RadiusActivityInput.RADIUS_ACTIVITY_INPUT)
    fun handle(event: FreeRadiusEvent) {
        println(event)
//        val registeredDevice = repository.findByInfoUsername(event.username)?.copy(info = event.toInfo())
//            ?: RegisteredDevice(info = event.toInfo())
//        repository.save(registeredDevice)
//        println(event.toInfo())
    }
}

private fun FreeRadiusEvent.toInfo(): RadiusActivityInfo {
    return RadiusActivityInfo(
        username = username,
        acctSessionId = acctSessionId,
        statusType = statusType,
        acctUniqueSessionId = acctUniqueSessionId,
        calledStationId = calledStationId,
        callingStationId = callingStationId,
        connectInfo = connectInfo,
        eventTimeStamp = Instant.now(),
        serviceType = serviceType
    )
}

interface RadiusActivityInput {
    @Input(RADIUS_ACTIVITY_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val RADIUS_ACTIVITY_INPUT = "radius-activity-input"
    }
}