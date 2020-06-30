package com.esmartit.seendevicesdatastore.application.radius.registered

import com.esmartit.seendevicesdatastore.application.incomingevents.RegisteredEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.SubscribableChannel
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@EnableBinding(RegisteredDeviceInput::class)
class RegisteredDeviceConsumer(
    private val repository: RegisteredDeviceRepository
) {

    @StreamListener(RegisteredDeviceInput.REGISTERED_DEVICE_INPUT)
    fun handle(event: RegisteredEvent) {
        println(event)
//        val registeredDevice = repository.findByInfoUsername(event.username)?.copy(info = event.toInfo())
//            ?: RegisteredDevice(info = event.toInfo())
//        repository.save(registeredDevice)
    }
}

private fun RegisteredEvent.toInfo(): RegisteredInfo {
    return RegisteredInfo(
        clientMac = clientMac,
        username = username,
        hotspotName = hotspotName,
        spotId = spotId,
        memberShip = memberShip.takeIf { it == "1" }?.let { true } ?: false,
        zipCode = zipCode,
        gender = gender.takeIf { it == "0" }?.let { Gender.MALE } ?: Gender.FEMALE,
        seenTime = Instant.ofEpochMilli(seenTimeEpoch),
        dateOfBirth = LocalDate.from(dateFormatter.parse(dateOfBirth))
    )
}

interface RegisteredDeviceInput {
    @Input(REGISTERED_DEVICE_INPUT)
    fun input(): SubscribableChannel

    companion object {
        const val REGISTERED_DEVICE_INPUT = "registered-device-input"
    }
}