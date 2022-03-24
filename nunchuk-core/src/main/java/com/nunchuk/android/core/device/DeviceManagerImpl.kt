package com.nunchuk.android.core.device

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.utils.DeviceManager
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DeviceManagerImpl @Inject constructor(
    private val ncSharePreferences: NCSharePreferences
) : DeviceManager {
    private var deviceId: String = ncSharePreferences.deviceId

    private fun generateDeviceId() = UUID.randomUUID().toString()

    private fun storeDeviceId(deviceId: String) {
        ncSharePreferences.deviceId = deviceId
    }

    override fun getDeviceId(): String {
        if (deviceId.isNullOrEmpty()) {
            deviceId = generateDeviceId()
            storeDeviceId(deviceId)
        }

        return deviceId
    }

}