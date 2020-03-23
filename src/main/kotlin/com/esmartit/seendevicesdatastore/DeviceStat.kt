package com.esmartit.seendevicesdatastore

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class DeviceStat(@Id val id: String? = null, val seenDevice: DeviceSeenEvent)