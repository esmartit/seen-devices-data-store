package com.esmartit.seendevicesdatastore.services

import com.esmartit.seendevicesdatastore.application.scanapi.daily.ScanApiDailyReactiveRepository
import org.springframework.stereotype.Service

@Service
class ScanApiDailyService(
        private val scanApiDailyReactiveRepository: ScanApiDailyReactiveRepository
)