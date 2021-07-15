package com.esmartit.seendevicesdatastore.application.scanapi.daily

import com.esmartit.seendevicesdatastore.domain.ScanApiActivityDaily
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ScanApiDailyStoreService(
        private val repository: ScanApiDailyReactiveRepository
) {
    fun save() {

    }

    fun createScanApiActivityDaily(event: ScanApiActivityDaily): Mono<ScanApiActivityDaily> {
        val scanApiDailyEvent = event.toScanApiActivityDaily()
        return repository.save(scanApiDailyEvent)
    }

}

private fun ScanApiActivityDaily.toScanApiActivityDaily(
): ScanApiActivityDaily {
    return this
}