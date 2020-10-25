package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDevice
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDeviceReactiveRepository
import com.esmartit.seendevicesdatastore.domain.RegisteredInfo
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.ZoneOffset

@Component
class ScanApiStoreService(
    private val repository: ScanApiReactiveRepository,
    private val radiusActivityRepository: RadiusActivityRepository,
    private val registeredUserRepository: RegisteredUserRepository,
    private val uniqueDeviceRepository: UniqueDeviceReactiveRepository,
    private val clock: Clock
) {

    fun save(newScanApiEvent: ScanApiActivity): Mono<UniqueDevice> {
        return createScanApiActivity(newScanApiEvent)
            .flatMap { saveUniqueDevice(newScanApiEvent) }
            .onErrorResume(DuplicateKeyException::class.java) {
                Mono.just(UniqueDevice(id = newScanApiEvent.clientMac))
            }
    }

    fun createScanApiActivity(event: ScanApiActivity): Mono<ScanApiActivity> {

        val clientMac = event.clientMac
        val clientMacNormalized = clientMac.replace(":", "").toLowerCase()
        val radiusActivity = radiusActivityRepository.findLastByClientMac(clientMacNormalized, PageRequest.of(0, 1))
        val registeredInfo =
            radiusActivity.firstOrNull()?.info?.username?.let { registeredUserRepository.findByInfoUsername(it)?.info }

        val scanApiEvent = event.toScanApiActivity(clock, registeredInfo)
        return repository.save(scanApiEvent)
    }

    private fun saveUniqueDevice(event: ScanApiActivity): Mono<UniqueDevice> {
        return uniqueDeviceRepository.save(UniqueDevice(id = event.clientMac))
    }
}

private fun ScanApiActivity.toScanApiActivity(
    clock: Clock,
    userInfo: RegisteredInfo?
): ScanApiActivity {
    return this.copy(age = userInfo?.dateOfBirth?.let { clock.instant().atZone(ZoneOffset.UTC).year - it.year } ?: 1900,
        gender = userInfo?.gender,
        memberShip = userInfo?.memberShip,
        username = userInfo?.username,
        userZipCode = userInfo?.zipCode)
}
