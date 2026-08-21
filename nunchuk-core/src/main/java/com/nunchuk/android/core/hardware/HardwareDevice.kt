package com.nunchuk.android.core.hardware

/**
 * Which link a hardware signer is reachable over. Both the Ledger and BitBox transports
 * expose the same two, so the shared device picker can filter on it.
 */
enum class HardwareTransportKind {
    BLE,
    USB,
}

/**
 * A hardware signer offered by the device picker, vendor-neutral so
 * [HardwareDeviceScanBody] backs every flow that lists devices.
 *
 * [id] is the stable per-device session id the native layer keys sessions by: the BLE
 * address, or the USB device name.
 */
data class HardwareDevice(
    val id: String,
    val name: String,
    val transport: HardwareTransportKind,
)
