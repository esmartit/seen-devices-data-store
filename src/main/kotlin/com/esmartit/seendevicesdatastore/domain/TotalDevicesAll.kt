package com.esmartit.seendevicesdatastore.domain

import org.springframework.data.mongodb.core.mapping.Document

@Document
data class TotalDevicesAll(val id: String = "uniqueCount", val count: Int)
