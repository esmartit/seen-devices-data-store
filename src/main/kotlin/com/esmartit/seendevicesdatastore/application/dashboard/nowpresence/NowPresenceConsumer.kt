package com.esmartit.seendevicesdatastore.application.dashboard.nowpresence

import com.esmartit.seendevicesdatastore.application.dashboard.nowpresence.NowPresenceInput.Companion.NOW_PRESENCE_INPUT
import com.esmartit.seendevicesdatastore.application.incomingevents.PresenceEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel

@EnableBinding(NowPresenceInput::class)
class NowPresenceConsumer(
    private val reactiveRepo: NowPresenceReactiveRepository
) {

    @StreamListener(NOW_PRESENCE_INPUT)
    fun handle(event: PresenceEvent) {
        reactiveRepo.save(event.toNowPresence()).block()
    }

    private fun PresenceEvent.toNowPresence(): NowPresence {
        return NowPresence(null, time, inCount, limitCount, outCount)
    }
}

interface NowPresenceInput {
    @Input(NOW_PRESENCE_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val NOW_PRESENCE_INPUT = "minute-device-presence-count-input"
    }
}