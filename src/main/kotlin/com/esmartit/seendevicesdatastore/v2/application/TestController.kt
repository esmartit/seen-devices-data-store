package com.esmartit.seendevicesdatastore.v2.application

import com.esmartit.seendevicesdatastore.application.scanapi.daily.DailyScanApiReactiveRepository
import com.esmartit.seendevicesdatastore.services.ClockService
import com.esmartit.seendevicesdatastore.services.ScanApiService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test")
class TestController(
    private val clock: ClockService,
    private val scanApiService: ScanApiService,
    private val dailyScanApiReactiveRepository: DailyScanApiReactiveRepository
) {


}

