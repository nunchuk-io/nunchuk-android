package com.nunchuk.android.wallet.confirm

import android.os.Parcelable
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.MasterSigner
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ParcelizeDevice(
    var type: String = "",
    var model: String = "",
    var path: String = "",
    var masterFingerprint: String = "",
    var connected: Boolean = false,
    var needPassPhraseSent: Boolean = false,
    var needPinSet: Boolean = false,
    var initialized: Boolean = true
) : Parcelable

@Parcelize
internal data class ParcelizeMasterSigner(
    var id: String = "",
    var name: String = "",
    var device: ParcelizeDevice = ParcelizeDevice(),
    var lastHealthCheck: Long = 0,
    var software: Boolean = false
) : Parcelable

internal fun List<MasterSigner>.parcelize() = map(MasterSigner::parcelize) as ArrayList<ParcelizeMasterSigner>

internal fun List<ParcelizeMasterSigner>.deparcelize() = map(ParcelizeMasterSigner::deparcelize)

internal fun MasterSigner.parcelize() = ParcelizeMasterSigner(
    id = id,
    name = name,
    device = device.parcelize(),
    lastHealthCheck = lastHealthCheck,
    software = software
)

internal fun ParcelizeMasterSigner.deparcelize() = MasterSigner(
    id = id,
    name = name,
    device = device.deparcelize(),
    lastHealthCheck = lastHealthCheck,
    software = software
)

internal fun Device.parcelize() = ParcelizeDevice(
    type = type,
    model = model,
    path = path,
    masterFingerprint = masterFingerprint,
    connected = connected,
    needPassPhraseSent = needPassPhraseSent,
    needPinSet = needPinSet,
    initialized = initialized
)

internal fun ParcelizeDevice.deparcelize() = Device(
    type = type,
    model = model,
    path = path,
    masterFingerprint = masterFingerprint,
    connected = connected,
    needPassPhraseSent = needPassPhraseSent,
    needPinSet = needPinSet,
    initialized = initialized
)


