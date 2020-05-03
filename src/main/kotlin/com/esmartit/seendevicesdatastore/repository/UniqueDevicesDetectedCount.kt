package com.esmartit.seendevicesdatastore.repository

import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class UniqueDevicesDetectedCount(val count: Long, val time: Instant)
