package com.esmartit.seendevicesdatastore.application.incomingevents

import com.esmartit.seendevicesdatastore.repository.Position

data class DeviceWithPresenceEvent(val deviceDetectedEvent: SensorActivityEvent, val position: Position)