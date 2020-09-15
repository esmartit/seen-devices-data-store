package com.esmartit.seendevicesdatastore.v1.services

import com.esmartit.seendevicesdatastore.v1.application.uniquedevices.UniqueDeviceReactiveRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CommonService(private val uniqueDeviceReactiveRepository: UniqueDeviceReactiveRepository) {

    fun allDevicesCount(): Mono<Long> {
        return uniqueDeviceReactiveRepository.count()
    }
}