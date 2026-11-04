package com.nunchuk.android.settings.devices

import com.nunchuk.android.core.profile.UserDeviceResponse

internal data class UserDeviceUiModel(
    val device: UserDeviceResponse,
    val isCurrentDevice: Boolean,
)

/**
 * Marks the row belonging to the phone running the app and orders the list.
 *
 * The device list mixes Matrix device IDs and Nunchuk API device IDs, so the current device is
 * found by identity only: the active Matrix session's device ID first, then the API device ID
 * stored at login. When neither matches, no row is current — never guess from position or
 * `lastTs`. Every response row is kept; the current one goes first and the rest are sorted by
 * recency.
 */
internal fun buildUserDeviceList(
    devices: List<UserDeviceResponse>,
    matrixDeviceId: String?,
    apiDeviceId: String?,
): List<UserDeviceUiModel> {
    val currentIndex = devices.indexOfDevice(matrixDeviceId).takeIf { it >= 0 }
        ?: devices.indexOfDevice(apiDeviceId)
    val current = devices.getOrNull(currentIndex)?.let { UserDeviceUiModel(it, isCurrentDevice = true) }
    val others = devices
        .filterIndexed { index, _ -> index != currentIndex }
        .sortedByDescending { it.lastTs }
        .map { UserDeviceUiModel(it, isCurrentDevice = false) }
    return listOfNotNull(current) + others
}

private fun List<UserDeviceResponse>.indexOfDevice(id: String?): Int {
    if (id.isNullOrBlank()) return -1
    return indexOfFirst { !it.id.isNullOrBlank() && it.id == id }
}
