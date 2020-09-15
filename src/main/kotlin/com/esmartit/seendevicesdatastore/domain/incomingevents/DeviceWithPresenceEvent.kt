package com.esmartit.seendevicesdatastore.domain.incomingevents

import com.esmartit.seendevicesdatastore.domain.Position

data class DeviceWithPresenceEvent(val deviceDetectedEvent: SensorActivityEvent, val position: Position)