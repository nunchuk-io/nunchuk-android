package com.nunchuk.android.settings.devices

import com.nunchuk.android.core.profile.UserDeviceResponse

data class UserDeviceState(
    val devices: List<UserDeviceResponse> = emptyList()
)

sealed class UserDeviceEvent {
    data class Loading(val loading: Boolean = false) : UserDeviceEvent()
    data class DeleteDevicesSuccessEvent(val device: UserDeviceResponse): UserDeviceEvent()
    data class CompromisedDevicesSuccessEvent(val device: UserDeviceResponse): UserDeviceEvent()
    object GetDevicesErrorEvent: UserDeviceEvent()
    object SignOutAllSuccessEvent: UserDeviceEvent()
}