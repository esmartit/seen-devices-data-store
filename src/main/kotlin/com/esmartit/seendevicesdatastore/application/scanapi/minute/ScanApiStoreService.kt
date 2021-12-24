package com.esmartit.seendevicesdatastore.application.scanapi.minute

import com.esmartit.seendevicesdatastore.application.brands.BrandsRepository
import com.esmartit.seendevicesdatastore.application.radius.online.RadiusActivityRepository
import com.esmartit.seendevicesdatastore.application.radius.registered.RegisteredUserRepository
import com.esmartit.seendevicesdatastore.application.sensorsettings.SensorSettingRepository
import com.esmartit.seendevicesdatastore.application.scanapi.daily.ScanApiActivityDailyRepository
import com.esmartit.seendevicesdatastore.domain.UniqueDevice
import com.esmartit.seendevicesdatastore.application.uniquedevices.UniqueDeviceReactiveRepository
import com.esmartit.seendevicesdatastore.domain.Position
import com.esmartit.seendevicesdatastore.domain.RegisteredInfo
import com.esmartit.seendevicesdatastore.domain.ScanApiActivity
import com.esmartit.seendevicesdatastore.domain.incomingevents.SensorActivityEvent
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
    private val clock: Clock,
    private val sensorSettingRepository: SensorSettingRepository,
    private val brandsRepository: BrandsRepository,
    private val scanApiActivityDailyRepository: ScanApiActivityDailyRepository
) {

    fun save(event: SensorActivityEvent): Mono<UniqueDevice> {
        val newScanApiEvent = event.toScanApiActivity()
        return createScanApiActivity(newScanApiEvent)
//            .doOnNext{saveScanActivityDaily(it)}
                .flatMap { saveUniqueDevice(newScanApiEvent) }
                .onErrorResume(DuplicateKeyException::class.java) {
                    Mono.just(UniqueDevice(id = newScanApiEvent.clientMac))
                }
    }

//    private fun saveScanActivityDaily(scanApiActivity: ScanApiActivity) {
//        scanApiActivityDailyRepository.findbyloquesea()
//        si no existe
//          creo variable
//        else
//          update variable
//        val scanApiDaily = scanApiActivity
//        scanApiActivityDailyRepository.save(scanApiDaily)
//    }

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

    private fun SensorActivityEvent.toScanApiActivity(): ScanApiActivity {
        val sensorSetting = sensorSettingRepository.findByApMac(apMac)
        val brand = brandsRepository.findByName(device.manufacturer ?: "")
        return ScanApiActivity(
            id = "${device.clientMac};${device.seenTime.epochSecond}",
            clientMac = device.clientMac,
            seenTime = device.seenTime,
            brand = brand.name,
            manufacturer = device.manufacturer,
            isConnected = !device.ssid.isNullOrBlank(),
            rssi = device.rssi,
            status = sensorSetting?.presence(device.rssi) ?: Position.NO_POSITION,
            spotId = sensorSetting?.tags?.get("spot_id"),
            sensorId = sensorSetting?.tags?.get("sensorname"),
            countryId = sensorSetting?.tags?.get("country"),
            stateId = sensorSetting?.tags?.get("state"),
            cityId = sensorSetting?.tags?.get("city"),
            zipCode = sensorSetting?.tags?.get("zipcode"),
            zone = sensorSetting?.tags?.get("zone"),
            groupName = sensorSetting?.tags?.get("groupname"),
            hotspot = sensorSetting?.tags?.get("hotspot"),
            ssid = device.ssid,
            location = device.location
        )
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
