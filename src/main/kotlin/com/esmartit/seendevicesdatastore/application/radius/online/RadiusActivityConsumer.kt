package com.esmartit.seendevicesdatastore.application.radius.online

import com.esmartit.seendevicesdatastore.domain.RadiusActivity
import com.esmartit.seendevicesdatastore.domain.RadiusActivityInfo
import com.esmartit.seendevicesdatastore.domain.incomingevents.FreeRadiusEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel
import java.time.Instant

@EnableBinding(RadiusActivityInput::class)
class RadiusActivityConsumer(
    private val repository: RadiusActivityRepository
) {

    @StreamListener(RadiusActivityInput.RADIUS_ACTIVITY_INPUT)
    fun handle(event: FreeRadiusEvent) {
        val radiusActivity = RadiusActivity(info = event.toInfo())
        repository.save(radiusActivity)
    }
}

private fun FreeRadiusEvent.toInfo(): RadiusActivityInfo {
    return RadiusActivityInfo(
        username = username,
        acctSessionId = acctSessionId,
        statusType = statusType,
        acctUniqueSessionId = acctUniqueSessionId,
        calledStationId = calledStationId,
        callingStationId = callingStationId.replace("-", "").toLowerCase(),
        connectInfo = connectInfo,
        eventTimeStamp = Instant.ofEpochSecond(eventTimeStamp),
        serviceType = serviceType,
        nasIpAddress = nasIpAddress,
        acctTerminateCause = acctTerminateCause,
        acctSessionTime = acctSessionTime,
        acctOutputOctets = acctOutputOctets,
        acctInputOctets = acctInputOctets
    )
}

interface RadiusActivityInput {
    @Input(RADIUS_ACTIVITY_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val RADIUS_ACTIVITY_INPUT = "radius-activity-input"
    }
}