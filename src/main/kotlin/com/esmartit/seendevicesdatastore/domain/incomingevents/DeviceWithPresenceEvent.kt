package com.esmartit.seendevicesdatastore.domain.incomingevents

import com.esmartit.seendevicesdatastore.v1.repository.Position

data class DeviceWithPresenceEvent(val deviceDetectedEvent: SensorActivityEvent, val position: Position)