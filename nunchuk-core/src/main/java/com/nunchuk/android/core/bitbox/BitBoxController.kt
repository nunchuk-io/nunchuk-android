package com.nunchuk.android.core.bitbox

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.hardware.HardwareDevice
import com.nunchuk.android.core.hardware.HardwareTransportKind
import com.nunchuk.android.model.BitBoxStep
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.BitBoxErrorCode
import com.nunchuk.android.type.BitBoxMnemonicLength
import com.nunchuk.android.type.BitBoxProduct
import com.nunchuk.android.type.BitBoxStepType
import com.nunchuk.android.type.BitBoxTransport
import com.nunchuk.android.type.BitBoxUserInteraction
import timber.log.Timber
import java.util.ArrayDeque
import java.util.UUID
import java.util.regex.Pattern
import kotlin.concurrent.thread

/**
 * BitBox's view of the shared device model, aliased so the BitBox and Ledger flows share one
 * device picker ([com.nunchuk.android.core.hardware.HardwareDeviceScanBody]).
 */
typealias BitBoxTransportKind = HardwareTransportKind
typealias BitBoxDevice = HardwareDevice

/** Every operation the controller can run; one is active at a time. */
enum class BitBoxRequest {
    INITIALIZE,
    MASTER_FINGERPRINT,
    XPUB,
    IS_WALLET_REGISTERED,
    REGISTER_WALLET,
    SIGN_PSBT,
    SIGN_MESSAGE,
    GET_WALLET_ADDRESS,
    SET_DEVICE_NAME,
    CREATE_NEW_SEED,
    SHOW_MNEMONIC,
    RESTORE_FROM_MNEMONIC,
    CHECK_SD_CARD,
    INSERT_SD_CARD,
    LIST_BACKUPS,
    RESTORE_BACKUP,
    CHANGE_PASSWORD,
    SET_PASSPHRASE_ENABLED,
    CREATE_BACKUP,
    CHECK_BACKUP,
    FACTORY_RESET,
    ENTER_FIRMWARE_UPGRADE,
    UPGRADE_FIRMWARE,
    BOOTLOADER_REBOOT,
}

/**
 * Drives the BitBox02 transports (BLE + USB) and the native step loop.
 *
 * The Confluence "BitBox02 mobile integration" page defers to the Ledger guide for the
 * general shape, so this is a port of [com.nunchuk.android.core.ledger.LedgerBleController]
 * onto the NunchukNativeSdk `bitbox*` bindings. Contract highlights specific to BitBox:
 *
 * - Stable session_id per device: BLE address / USB deviceName.
 * - BLE bonds and requests MTU 512; initialization starts only once indications **and**
 *   writes are ready. Each 64-byte report in [BitBoxStep.writes] is written one at a time.
 * - USB matches VID/PID 0x03eb:0x2403 and claims interface 0's bulk IN/OUT endpoints.
 * - Three step types Ledger does not have: `RETRY_AFTER` (poll again after a delay),
 *   `AWAITING_USER` (pairing-code confirmation) and `REBOOT` (write, then expect the
 *   device to drop off the bus).
 * - A transport disconnect invalidates the Noise session. There is no resuming across it:
 *   the host must call [initialize] again, which builds a fresh native session, and then
 *   restart whatever operation was interrupted. [resume] is only for an expired request on
 *   a still-live session (the `RETRY_AFTER` path).
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class BitBoxController(
    private val context: Context,
    private val nativeSdk: NunchukNativeSdk,
    private val listener: Listener,
) {
    interface Listener {
        fun onScanResults(devices: List<BitBoxDevice>)
        fun onScanFinished(found: Int)
        fun onConnecting(device: BitBoxDevice)

        /**
         * A non-NONE interaction the user must act on. Already de-duplicated: polling
         * repeats the same interaction many times and only changes are reported.
         */
        fun onInteraction(interaction: BitBoxUserInteraction)

        /**
         * An `AWAITING_USER` step carrying a pairing code. Show it exactly once, next to
         * the code on the device, then answer with [confirmPairing].
         */
        fun onPairingCode(code: String)

        /** Firmware-upgrade progress as a 0..1 fraction, from bootloader steps. */
        fun onProgress(request: BitBoxRequest, progress: Double)

        /**
         * [request] reached COMPLETE. Read the payload with the matching accessor —
         * [resultString], [resultBoolean], [initializeResult] or [listBackupsResult].
         */
        fun onCommandComplete(request: BitBoxRequest)

        /**
         * [request] reached FAILED. [code] drives recovery — see the Confluence §6 error table.
         * [deviceCode] is the firmware's own error number, 0 when the failure didn't come from
         * the device; §6 asks for it alongside the message on the DEVICE_* codes, where the
         * message alone rarely says which of the device's states was wrong.
         */
        fun onCommandFailed(
            request: BitBoxRequest,
            code: BitBoxErrorCode,
            message: String,
            deviceCode: Int,
        )

        /**
         * A `REBOOT` step: the frames have been written and the device is expected to drop
         * off the bus (factory reset, firmware upgrade). The session is dead afterwards.
         */
        fun onReboot(request: BitBoxRequest)

        /** The transport dropped. The Noise session is gone — reconnect and re-initialize. */
        fun onDisconnected()

        fun onError(message: String)

        /** Bluetooth is off — the host should prompt the user to enable it. */
        fun onBluetoothDisabled()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)
    private val usbManager: UsbManager? =
        ContextCompat.getSystemService(context, UsbManager::class.java)

    private var scanCallback: ScanCallback? = null
    private var scanning = false
    private val foundDevices = linkedMapOf<String, BitBoxDevice>()

    private var connection: BitBoxConnection? = null
    private var pendingReadyAction: (() -> Unit)? = null

    private var usbReceiverRegistered = false

    /** The last interaction surfaced, so polling can't repeat it — Confluence §0. */
    private var lastInteraction = BitBoxUserInteraction.NONE

    /** The pairing code already shown for this session; each one is displayed only once. */
    private var shownPairingCode: String? = null

    /** The session id of the connected device, for the native result accessors. */
    val sessionId: String? get() = connection?.id

    val connectedTransport: BitBoxTransportKind? get() = connection?.transport

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val device = intent.usbDeviceExtra() ?: return
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openUsbDevice(device)
                    } else {
                        listener.onError("USB permission denied")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> onUsbAttached(device)
                UsbManager.ACTION_USB_DEVICE_DETACHED -> onUsbDetached(device)
            }
        }
    }

    // region permissions
    fun hasBlePermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )

        else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // endregion

    // region scanning
    fun startScan() {
        registerUsbReceiver()
        foundDevices.clear()
        // USB BitBoxes are already attached (no scan needed); list them up front.
        enumerateUsbDevices()
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            // Keep USB devices visible; ask the host to prompt the user to enable BT.
            if (foundDevices.isNotEmpty()) listener.onScanResults(foundDevices.values.toList())
            listener.onBluetoothDisabled()
            return
        }
        // A BitBox already bonded to this phone may not advertise; seed it too.
        seedBondedDevices(adapter)
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            if (foundDevices.isNotEmpty()) listener.onScanResults(foundDevices.values.toList())
            listener.onError("BLE scanner is unavailable")
            return
        }
        if (scanning) return
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!isBitBoxBleScanResult(result)) return
                val name = result.scanRecord?.deviceName
                    ?: runCatching { result.device.name }.getOrNull()
                foundDevices[result.device.address] = BitBoxDevice(
                    id = result.device.address,
                    name = name?.takeIf { it.isNotBlank() } ?: result.device.address,
                    transport = BitBoxTransportKind.BLE,
                )
                listener.onScanResults(foundDevices.values.toList())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                listener.onError("BLE scan failed: $errorCode")
            }
        }
        scanCallback = callback
        scanning = true
        runCatching {
            scanner.startScan(
                null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                callback,
            )
        }.onFailure {
            scanning = false
            listener.onError("BLE scan failed: ${it.message ?: it.javaClass.simpleName}")
            return
        }
        mainHandler.postDelayed({
            if (scanning) {
                stopScan()
                listener.onScanFinished(foundDevices.size)
            }
        }, BLE_SCAN_TIMEOUT_MS)
    }

    private fun seedBondedDevices(adapter: BluetoothAdapter) {
        val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
        bonded.forEach { device ->
            val name = runCatching { device.name }.getOrNull()
            if (name != null && BITBOX_BLE_NAME_PATTERN.matcher(name).matches()) {
                foundDevices[device.address] =
                    BitBoxDevice(device.address, name, BitBoxTransportKind.BLE)
            }
        }
        if (foundDevices.isNotEmpty()) listener.onScanResults(foundDevices.values.toList())
    }

    /**
     * Lists attached USB BitBoxes only — no Bluetooth permission or scan. Used when the
     * user enters via "Add BitBox via USB", so we don't prompt for BT permission there.
     */
    fun refreshUsb() {
        registerUsbReceiver()
        foundDevices.entries.removeAll { it.value.transport == BitBoxTransportKind.USB }
        enumerateUsbDevices()
        listener.onScanResults(foundDevices.values.toList())
    }

    private fun enumerateUsbDevices() {
        usbManager?.deviceList?.values.orEmpty()
            .filter { it.isBitBox() }
            .forEach { device ->
                foundDevices[device.deviceName] = BitBoxDevice(
                    id = device.deviceName,
                    name = device.productName ?: "BitBox02 (USB)",
                    transport = BitBoxTransportKind.USB,
                )
            }
    }

    fun stopScan() {
        if (!scanning) return
        scanning = false
        val adapter = bluetoothManager?.adapter
        scanCallback?.let { cb -> runCatching { adapter?.bluetoothLeScanner?.stopScan(cb) } }
        scanCallback = null
    }

    private fun isBitBoxBleScanResult(result: ScanResult): Boolean {
        val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull()
        if (name != null && BITBOX_BLE_NAME_PATTERN.matcher(name).matches()) return true
        return result.scanRecord?.serviceUuids.orEmpty().any { it.uuid == BITBOX_BLE_SERVICE_UUID }
    }

    private fun UsbDevice.isBitBox(): Boolean =
        vendorId == BITBOX_USB_VENDOR_ID && productId == BITBOX_USB_PRODUCT_ID
    // endregion

    // region connect (transport dispatch)
    fun connect(device: BitBoxDevice) {
        Timber.tag(TAG).d("connect ${device.name} id=${device.id} transport=${device.transport}")
        stopScan()
        // Tear down a prior connection to a different device (avoid leaking its runtime).
        connection?.takeIf { it.id != device.id }?.let {
            teardown(it)
            connection = null
        }
        when (device.transport) {
            BitBoxTransportKind.BLE -> connectBleById(device.id)
            BitBoxTransportKind.USB -> connectUsbById(device.id)
        }
    }

    /** Runs [action] once the transport is ready, or immediately if it already is. */
    fun whenReady(action: () -> Unit) {
        val conn = connection
        val ready = conn != null && conn.isReady()
        Timber.tag(TAG).d("whenReady ready=$ready")
        if (ready) action() else pendingReadyAction = action
    }
    // endregion

    // region BLE
    private fun connectBleById(address: String) {
        val adapter: BluetoothAdapter = bluetoothManager?.adapter?.takeIf { it.isEnabled } ?: run {
            listener.onBluetoothDisabled()
            return
        }
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: run {
            listener.onError("BitBox device unavailable")
            return
        }
        connectBle(device, connection)
    }

    private fun connectBle(device: BluetoothDevice, existing: BitBoxConnection? = null) {
        val conn = existing?.takeIf { it.transport == BitBoxTransportKind.BLE }
            ?: BitBoxConnection(device.address, BitBoxTransportKind.BLE).also { connection = it }
        conn.bleDevice = device
        listener.onConnecting(
            BitBoxDevice(device.address, device.name ?: device.address, BitBoxTransportKind.BLE)
        )
        conn.gatt?.let { old ->
            runCatching { old.disconnect() }
            runCatching { old.close() }
            conn.clearBleRuntime()
        }
        // Bonding authenticates the app side of the pairing (Confluence §0), so ask for it
        // before the GATT work rather than letting the first encrypted write trigger it.
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            runCatching { device.createBond() }
        }
        conn.gatt = device.connectGatt(context, false, bleCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val bleCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            mainHandler.post { handleConnectionStateChange(gatt, newState) }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mainHandler.post {
                // Leave room for the ATT opcode + handle; the rest is payload.
                        Timber.tag(TAG).d("onMtuChanged mtu=$mtu status=$status")
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            mainHandler.post { handleServicesDiscovered(gatt) }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            mainHandler.post {
                // Indications are live: the transport is only usable once both this and the
                // write characteristic are in place (Confluence §0).
                connection?.bleNotifyReady = true
                completeBleReady(gatt)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            mainHandler.post { handleCharacteristicWrite(status) }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = characteristic.value ?: return
            mainHandler.post { onData(value) }
        }
    }

    private fun handleConnectionStateChange(gatt: BluetoothGatt, newState: Int) {
        val conn = connection ?: return
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                Timber.tag(TAG).d("BLE connected, requesting MTU $BITBOX_BLE_MTU")
                if (!gatt.requestMtu(BITBOX_BLE_MTU)) gatt.discoverServices()
            }

            BluetoothGatt.STATE_DISCONNECTED -> {
                Timber.tag(TAG).d("BLE disconnected")
                conn.clearBleRuntime()
                notifyDisconnected(conn)
            }
        }
    }

    private fun handleServicesDiscovered(gatt: BluetoothGatt) {
        val conn = connection ?: return
        val service = gatt.getService(BITBOX_BLE_SERVICE_UUID) ?: run {
            fail("BitBox BLE service not found")
            return
        }
        val write = service.getCharacteristic(BITBOX_BLE_WRITE_UUID)
        val indicate = service.getCharacteristic(BITBOX_BLE_INDICATE_UUID)
        if (write == null || indicate == null) {
            fail("BitBox BLE characteristics not found")
            return
        }
        conn.writeCharacteristic = write
        gatt.setCharacteristicNotification(indicate, true)
        val cccd = indicate.getDescriptor(CCCD_UUID)
        if (cccd == null) {
            // No CCCD to write: treat indications as live and continue.
            conn.bleNotifyReady = true
            completeBleReady(gatt)
            return
        }
        cccd.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        if (!gatt.writeDescriptor(cccd)) {
            fail("Failed to enable BitBox indications")
        }
    }

    private fun completeBleReady(gatt: BluetoothGatt) {
        val conn = connection ?: return
        if (!conn.canWriteBle()) return
        Timber.tag(TAG).d("BLE ready for ${conn.id}")
        onTransportReady()
    }

    private fun handleCharacteristicWrite(status: Int) {
        val conn = connection ?: return
        if (status == BluetoothGatt.GATT_SUCCESS) {
            conn.bleWriteInFlight = null
            conn.bleWriteRetries = 0
            writeNextBleFrame()
        } else {
            retryBleWrite()
        }
    }

    private fun writeBleFrames(frames: List<ByteArray>) {
        val conn = connection ?: return
        frames.forEach { conn.bleWriteQueue.add(it) }
        if (conn.bleWriteInFlight == null) writeNextBleFrame()
    }

    private fun writeNextBleFrame() {
        val conn = connection ?: return
        val gatt = conn.gatt ?: return
        val characteristic = conn.writeCharacteristic ?: return
        val frame = conn.bleWriteQueue.poll() ?: return
        conn.bleWriteInFlight = frame
        characteristic.value = frame
        characteristic.writeType = conn.bleWriteType
        if (!gatt.writeCharacteristic(characteristic)) retryBleWrite()
    }

    private fun retryBleWrite() {
        val conn = connection ?: return
        val frame = conn.bleWriteInFlight ?: return
        if (conn.bleWriteRetries++ >= BLE_WRITE_RETRY_ATTEMPTS) {
            fail("BitBox BLE write failed")
            return
        }
        mainHandler.postDelayed({
            val gatt = conn.gatt ?: return@postDelayed
            val characteristic = conn.writeCharacteristic ?: return@postDelayed
            characteristic.value = frame
            characteristic.writeType = conn.bleWriteType
            if (!gatt.writeCharacteristic(characteristic)) retryBleWrite()
        }, BLE_WRITE_RETRY_DELAY_MS)
    }
    // endregion

    // region USB
    private fun connectUsbById(deviceName: String) {
        val device = usbManager?.deviceList?.get(deviceName) ?: run {
            listener.onError("BitBox USB device unavailable")
            return
        }
        val conn = connection?.takeIf { it.id == deviceName && it.transport == BitBoxTransportKind.USB }
            ?: BitBoxConnection(deviceName, BitBoxTransportKind.USB).also { connection = it }
        conn.usbDevice = device
        listener.onConnecting(
            BitBoxDevice(deviceName, device.productName ?: deviceName, BitBoxTransportKind.USB)
        )
        if (usbManager.hasPermission(device)) {
            openUsbDevice(device)
        } else {
            requestUsbPermission(device)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName), flags,
        )
        usbManager?.requestPermission(device, intent)
    }

    private fun openUsbDevice(device: UsbDevice) {
        val conn = connection?.takeIf { it.transport == BitBoxTransportKind.USB } ?: return
        // Confluence §0: claim interface 0 and use its bulk IN/OUT endpoints.
        val usbInterface = device.getInterface(BITBOX_USB_INTERFACE).takeIf {
            device.interfaceCount > BITBOX_USB_INTERFACE
        } ?: run {
            fail("BitBox USB interface not found")
            return
        }
        var inEndpoint: UsbEndpoint? = null
        var outEndpoint: UsbEndpoint? = null
        for (i in 0 until usbInterface.endpointCount) {
            val endpoint = usbInterface.getEndpoint(i)
            if (endpoint.direction == UsbConstants.USB_DIR_IN) inEndpoint = endpoint
            if (endpoint.direction == UsbConstants.USB_DIR_OUT) outEndpoint = endpoint
        }
        if (inEndpoint == null || outEndpoint == null) {
            fail("BitBox USB endpoints not found")
            return
        }
        conn.clearUsbRuntime()
        val usbConnection = usbManager?.openDevice(device)
        if (usbConnection == null || !usbConnection.claimInterface(usbInterface, true)) {
            runCatching { usbConnection?.close() }
            fail("Failed to open BitBox USB device")
            return
        }
        conn.usbConnection = usbConnection
        conn.usbInterface = usbInterface
        conn.usbInEndpoint = inEndpoint
        conn.usbOutEndpoint = outEndpoint
        startUsbReader(conn)
        onTransportReady()
    }

    private fun onUsbAttached(device: UsbDevice) {
        if (!device.isBitBox()) return
        foundDevices[device.deviceName] = BitBoxDevice(
            id = device.deviceName,
            name = device.productName ?: "BitBox02 (USB)",
            transport = BitBoxTransportKind.USB,
        )
        listener.onScanResults(foundDevices.values.toList())
    }

    private fun onUsbDetached(device: UsbDevice) {
        if (!device.isBitBox()) return
        foundDevices.remove(device.deviceName)
        listener.onScanResults(foundDevices.values.toList())
        val conn = connection ?: return
        if (conn.transport != BitBoxTransportKind.USB || conn.id != device.deviceName) return
        conn.clearUsbRuntime()
        notifyDisconnected(conn)
    }

    private fun writeUsbFrames(frames: List<ByteArray>) {
        val conn = connection
        val usbConnection = conn?.usbConnection
        val endpoint = conn?.usbOutEndpoint
        if (usbConnection == null || endpoint == null) {
            fail("BitBox USB connection is not open")
            return
        }
        frames.forEach { frame ->
            val wrote = usbConnection.bulkTransfer(endpoint, frame, frame.size, USB_TIMEOUT_MS)
            if (wrote < 0) {
                fail("USB write failed")
                return
            }
        }
    }

    private fun startUsbReader(conn: BitBoxConnection) {
        val usbConnection = conn.usbConnection ?: return
        val endpoint = conn.usbInEndpoint ?: return
        conn.usbReaderRunning = true
        thread(name = "bitbox-usb-reader-${conn.id}") {
            val buffer = ByteArray(endpoint.maxPacketSize.coerceAtLeast(1))
            // Also stop once this handle is replaced, so a stale reader can't keep pulling
            // frames off a newer connection alongside its own reader.
            while (conn.usbReaderRunning && conn.usbConnection === usbConnection) {
                val read = usbConnection.bulkTransfer(endpoint, buffer, buffer.size, USB_TIMEOUT_MS)
                if (read > 0) {
                    val frame = buffer.copyOf(read)
                    mainHandler.post { onData(frame) }
                }
            }
        }
    }

    private fun registerUsbReceiver() {
        if (usbReceiverRegistered) return
        // ATTACHED/DETACHED keep the picker in sync; both are protected system broadcasts, so
        // NOT_EXPORTED still receives them while keeping our own permission action app-private.
        val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        usbReceiverRegistered = true
    }

    private fun unregisterUsbReceiver() {
        if (!usbReceiverRegistered) return
        runCatching { context.unregisterReceiver(usbReceiver) }
        usbReceiverRegistered = false
    }
    // endregion

    // region commands (transport-agnostic)

    /**
     * Starts a fresh native session over the connected transport and initializes it. This is
     * both the first call after connecting and the only valid recovery after a disconnect.
     */
    fun initialize() = startCommand(BitBoxRequest.INITIALIZE) { id, transport ->
        // A new session means a new pairing code may be shown.
        shownPairingCode = null
        nativeSdk.bitboxInitialize(id, transport)
    }

    /** Answers an `AWAITING_USER` step; [accepted] is whether the codes matched. */
    fun confirmPairing(accepted: Boolean) {
        val conn = connection ?: return
        runBitBox { handleStep(nativeSdk.bitboxConfirmPairing(conn.id, accepted)) }
    }

    fun getMasterFingerprint() = startCommand(BitBoxRequest.MASTER_FINGERPRINT) { id, _ ->
        nativeSdk.bitboxGetMasterFingerprint(id)
    }

    fun getExtendedPublicKey(derivationPath: String, checkOnDevice: Boolean = false) =
        startCommand(BitBoxRequest.XPUB) { id, _ ->
            nativeSdk.bitboxGetExtendedPublicKey(id, derivationPath, checkOnDevice)
        }

    fun isWalletRegistered(wallet: Wallet) =
        startCommand(BitBoxRequest.IS_WALLET_REGISTERED) { id, _ ->
            nativeSdk.bitboxIsWalletRegistered(id, wallet)
        }

    fun registerWallet(wallet: Wallet) = startCommand(BitBoxRequest.REGISTER_WALLET) { id, _ ->
        nativeSdk.bitboxRegisterWallet(id, wallet)
    }

    /** The wallet must already be registered — check with [isWalletRegistered] first. */
    fun signPsbt(wallet: Wallet, psbt: String) = startCommand(BitBoxRequest.SIGN_PSBT) { id, _ ->
        nativeSdk.bitboxSignPsbt(id, wallet, psbt)
    }

    fun signMessage(derivationPath: String, message: String) =
        startCommand(BitBoxRequest.SIGN_MESSAGE) { id, _ ->
            nativeSdk.bitboxSignMessage(id, derivationPath, message)
        }

    /**
     * Confluence §5 `WalletAddressOptions`. [checkOnDevice] is passed explicitly rather than left
     * to the binding's default: it is what makes the device *display* the address, which is the
     * entire point of verifying one, and a silently-flipped default would turn verification into
     * comparing a value the device computed without ever showing the user.
     */
    fun getWalletAddress(
        wallet: Wallet,
        addressIndex: Int,
        change: Boolean,
        checkOnDevice: Boolean = true,
    ) = startCommand(BitBoxRequest.GET_WALLET_ADDRESS) { id, _ ->
        nativeSdk.bitboxGetWalletAddress(id, wallet, addressIndex, change, checkOnDevice)
    }

    fun setDeviceName(name: String) = startCommand(BitBoxRequest.SET_DEVICE_NAME) { id, _ ->
        nativeSdk.bitboxSetDeviceName(id, name)
    }

    fun createNewSeed(mnemonicLength: BitBoxMnemonicLength) =
        startCommand(BitBoxRequest.CREATE_NEW_SEED) { id, _ ->
            nativeSdk.bitboxCreateNewSeed(id, mnemonicLength)
        }

    fun showMnemonic() = startCommand(BitBoxRequest.SHOW_MNEMONIC) { id, _ ->
        nativeSdk.bitboxShowMnemonic(id)
    }

    fun restoreFromMnemonic() = startCommand(BitBoxRequest.RESTORE_FROM_MNEMONIC) { id, _ ->
        nativeSdk.bitboxRestoreFromMnemonic(id)
    }

    fun checkSdCard() = startCommand(BitBoxRequest.CHECK_SD_CARD) { id, _ ->
        nativeSdk.bitboxCheckSdCard(id)
    }

    fun insertSdCard() = startCommand(BitBoxRequest.INSERT_SD_CARD) { id, _ ->
        nativeSdk.bitboxInsertSdCard(id)
    }

    fun listBackups() = startCommand(BitBoxRequest.LIST_BACKUPS) { id, _ ->
        nativeSdk.bitboxListBackups(id)
    }

    fun restoreBackup(backupId: String) = startCommand(BitBoxRequest.RESTORE_BACKUP) { id, _ ->
        nativeSdk.bitboxRestoreBackup(id, backupId)
    }

    fun changePassword() = startCommand(BitBoxRequest.CHANGE_PASSWORD) { id, _ ->
        nativeSdk.bitboxChangePassword(id)
    }

    fun setMnemonicPassphraseEnabled(enabled: Boolean) =
        startCommand(BitBoxRequest.SET_PASSPHRASE_ENABLED) { id, _ ->
            nativeSdk.bitboxSetMnemonicPassphraseEnabled(id, enabled)
        }

    fun createBackup() = startCommand(BitBoxRequest.CREATE_BACKUP) { id, _ ->
        nativeSdk.bitboxCreateBackup(id)
    }

    fun checkBackup(silent: Boolean = false) = startCommand(BitBoxRequest.CHECK_BACKUP) { id, _ ->
        nativeSdk.bitboxCheckBackup(id, silent)
    }

    fun factoryReset() = startCommand(BitBoxRequest.FACTORY_RESET) { id, _ ->
        nativeSdk.bitboxFactoryReset(id)
    }

    fun enterFirmwareUpgrade(signedFirmware: ByteArray) =
        startCommand(BitBoxRequest.ENTER_FIRMWARE_UPGRADE) { id, _ ->
            nativeSdk.bitboxEnterFirmwareUpgrade(id, signedFirmware)
        }

    /**
     * Opens a bootloader session and uploads [signedFirmware]. Replaces the normal session,
     * so the device must be re-initialized once it reboots.
     */
    fun upgradeFirmware(product: BitBoxProduct, signedFirmware: ByteArray) =
        startCommand(BitBoxRequest.UPGRADE_FIRMWARE) { id, _ ->
            nativeSdk.bitboxUpgradeFirmware(id, product, signedFirmware)
        }

    fun bootloaderReboot() = startCommand(BitBoxRequest.BOOTLOADER_REBOOT) { id, _ ->
        nativeSdk.bitboxBootloaderReboot(id)
    }
    // endregion

    // region results
    fun resultString(): String = withSession { nativeSdk.bitboxResultString(it) }.orEmpty()

    fun resultBoolean(): Boolean = withSession { nativeSdk.bitboxResultBoolean(it) } ?: false

    fun initializeResult() = withSession { nativeSdk.bitboxInitializeResult(it) }

    fun listBackupsResult() = withSession { nativeSdk.bitboxListBackupsResult(it) }.orEmpty()

    fun deviceInfo() = withSession { nativeSdk.bitboxDeviceInfo(it) }

    private inline fun <T> withSession(block: (String) -> T): T? {
        val id = connection?.id ?: return null
        return runCatching { block(id) }
            .onFailure { Timber.tag(TAG).e(it, "BitBox result read failed") }
            .getOrNull()
    }
    // endregion

    // region step loop
    private fun startCommand(
        request: BitBoxRequest,
        block: (String, BitBoxTransport) -> BitBoxStep,
    ) {
        val conn = connection ?: run {
            Timber.tag(TAG).e("startCommand($request): no connection")
            listener.onError("BitBox not connected")
            return
        }
        // Confluence §0, the single-command rule: an operation has to reach COMPLETE, FAILED or
        // REBOOT before the next one starts. Starting a second command over a live one would
        // interleave two conversations on the same session and desync the step machine.
        //
        // INITIALIZE is exempt: it builds a brand-new native session (forSession replaces
        // whatever was there), so it is also the documented way out of a stuck or lost session.
        // Guarding it would strand the user with no way to retry.
        if (request == BitBoxRequest.INITIALIZE) {
            conn.finishCommand()
        } else if (conn.commandActive) {
            Timber.tag(TAG).w("startCommand($request) ignored: ${conn.activeRequest} still active")
            return
        }
        Timber.tag(TAG).d("startCommand($request) session=${conn.id} transport=${conn.transport}")
        conn.activeRequest = request
        conn.commandActive = true
        // Each operation reports its own interactions from scratch.
        lastInteraction = BitBoxUserInteraction.NONE
        runBitBox { handleStep(block(conn.id, conn.transport.toNative())) }
    }

    private fun onTransportReady() {
        Timber.tag(TAG).d("onTransportReady pendingReadyAction=${pendingReadyAction != null}")
        pendingReadyAction?.let { action ->
            pendingReadyAction = null
            action()
        }
    }

    private fun onData(data: ByteArray) {
        val conn = connection ?: return
        Timber.tag(TAG).d("onData ${data.size} bytes: ${data.toHexPreview()}")
        val bootloader = conn.activeRequest == BitBoxRequest.UPGRADE_FIRMWARE ||
            conn.activeRequest == BitBoxRequest.BOOTLOADER_REBOOT
        runBitBox {
            handleStep(
                if (bootloader) {
                    nativeSdk.bitboxBootloaderOnData(conn.id, data)
                } else {
                    nativeSdk.bitboxOnData(conn.id, data)
                }
            )
        }
    }

    private fun handleStep(step: BitBoxStep) {
        val conn = connection ?: return
        val request = conn.activeRequest
        Timber.tag(TAG).d(
            "handleStep type=${step.stepType} interaction=${step.userInteraction} " +
                "writes=${step.writes.size} request=$request"
        )
        reportInteraction(step.userInteraction)
        reportPairingCode(step.pairingCode)
        step.progressOrNull?.let { progress ->
            if (request != null) listener.onProgress(request, progress)
        }
        when (step.stepType) {
            BitBoxStepType.WRITE -> {
                conn.commandActive = true
                writeFrames(step.writes)
            }

            BitBoxStepType.READ_MORE -> {
                conn.commandActive = true
            }

            // The device asked to be polled again rather than pushing a response.
            BitBoxStepType.RETRY_AFTER -> {
                conn.commandActive = true
                val delay = step.retryAfterMs.toLong().coerceAtLeast(0L)
                mainHandler.postDelayed({
                    val current = connection ?: return@postDelayed
                    runBitBox { handleStep(nativeSdk.bitboxResume(current.id)) }
                }, delay)
            }

            // The device side is settled; now the answer has to come from the user. The code
            // itself was already surfaced by reportPairingCode — possibly several steps ago.
            BitBoxStepType.AWAITING_USER -> {
                conn.commandActive = true
            }

            BitBoxStepType.COMPLETE -> {
                conn.finishCommand()
                if (request != null) listener.onCommandComplete(request)
            }

            BitBoxStepType.FAILED -> {
                val error = step.error
                val code = error?.errorCode ?: BitBoxErrorCode.NONE
                val message = error?.message?.takeIf { it.isNotBlank() } ?: "BitBox error"
                Timber.tag(TAG).e(
                    "FAILED request=$request code=$code deviceCode=${error?.deviceCode ?: 0} message=$message"
                )
                conn.finishCommand()
                // A lost session cannot be resumed; the host has to reconnect and
                // re-initialize before restarting the operation (Confluence §6).
                if (code.isSessionLost()) shownPairingCode = null
                if (request != null) {
                    listener.onCommandFailed(request, code, message, error?.deviceCode ?: 0)
                } else {
                    listener.onError(message)
                }
            }

            // Terminal: write the frames, then the device drops off the bus.
            BitBoxStepType.REBOOT -> {
                writeFrames(step.writes)
                conn.finishCommand()
                if (request != null) listener.onReboot(request)
            }
        }
    }

    /**
     * Confluence §0 ("display each pairing_code only once").
     *
     * The code is **not** exclusive to `AWAITING_USER`: the native session attaches it to the
     * WRITE that asks the device to confirm, and re-attaches it to every READ_MORE and
     * RETRY_AFTER while that request is outstanding. It has to be on screen for all of that —
     * the user is being asked to compare it against the device right then. Waiting for
     * `AWAITING_USER` would show it only after the device had already been confirmed, and over
     * BLE that step never arrives at all (bonding authenticates the app side, so the session
     * goes straight to finishPairing).
     */
    private fun reportPairingCode(code: String?) {
        if (code == null || code == shownPairingCode) return
        shownPairingCode = code
        listener.onPairingCode(code)
    }

    /** Confluence §0: surface a non-NONE interaction only when it changes. */
    private fun reportInteraction(interaction: BitBoxUserInteraction) {
        if (interaction == BitBoxUserInteraction.NONE || interaction == lastInteraction) return
        lastInteraction = interaction
        listener.onInteraction(interaction)
    }

    private fun writeFrames(frames: List<ByteArray>) {
        if (frames.isEmpty()) return
        Timber.tag(TAG).d("writeFrames ${frames.size} frame(s) via ${connection?.transport}")
        when (connection?.transport) {
            BitBoxTransportKind.BLE -> writeBleFrames(frames)
            BitBoxTransportKind.USB -> writeUsbFrames(frames)
            null -> Unit
        }
    }

    private inline fun runBitBox(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "BitBox native error")
            connection?.finishCommand()
            listener.onError(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun fail(message: String) {
        Timber.tag(TAG).e("fail: $message (activeRequest=${connection?.activeRequest})")
        connection?.finishCommand()
        listener.onError(message)
    }

    /**
     * The transport dropped, which invalidates the Noise session. Anything in flight is
     * dead — the host reconnects, calls [initialize] again and restarts the operation.
     */
    private fun notifyDisconnected(conn: BitBoxConnection) {
        conn.finishCommand()
        lastInteraction = BitBoxUserInteraction.NONE
        shownPairingCode = null
        pendingReadyAction = null
        listener.onDisconnected()
    }
    // endregion

    private fun teardown(conn: BitBoxConnection) {
        runCatching { conn.gatt?.disconnect() }
        runCatching { conn.gatt?.close() }
        conn.clearBleRuntime()
        conn.clearUsbRuntime()
        conn.finishCommand()
    }

    fun close() {
        stopScan()
        unregisterUsbReceiver()
        // Drop pending scan-timeout / retry / write-retry callbacks so they can't keep this
        // controller (and the Activity it references) alive after teardown.
        mainHandler.removeCallbacksAndMessages(null)
        pendingReadyAction = null
        connection?.let { teardown(it) }
        connection = null
    }

    private fun Intent.usbDeviceExtra(): UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION") getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

    private fun BitBoxTransportKind.toNative(): BitBoxTransport = when (this) {
        BitBoxTransportKind.BLE -> BitBoxTransport.BLE
        BitBoxTransportKind.USB -> BitBoxTransport.USB_HID
    }

    /** First bytes of a frame as hex, for logging (the framing header is the useful part). */
    private fun ByteArray.toHexPreview(max: Int = 24): String {
        val shown = take(max).joinToString(" ") { "%02X".format(it) }
        return if (size > max) "$shown … (${size}B)" else shown
    }

    companion object {
        private const val TAG = "BitBox"
        private const val BLE_SCAN_TIMEOUT_MS = 12_000L
        private const val BLE_WRITE_RETRY_ATTEMPTS = 8
        private const val BLE_WRITE_RETRY_DELAY_MS = 120L
        private const val USB_TIMEOUT_MS = 5_000

        /** Confluence §0: Android bonds and requests MTU 512. */
        private const val BITBOX_BLE_MTU = 512

        // Confluence §0. Mirrored in decimal by nunchuk-app's res/xml/bitbox_usb_device_filter.xml,
        // which decides whether Nunchuk is offered when one is plugged in — change both together.
        private const val BITBOX_USB_VENDOR_ID = 0x03eb
        private const val BITBOX_USB_PRODUCT_ID = 0x2403
        private const val BITBOX_USB_INTERFACE = 0

        private const val ACTION_USB_PERMISSION = "com.nunchuk.android.core.bitbox.USB_PERMISSION"

        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // BLE is BitBox02 Nova only; the other models are USB-only.
        private val BITBOX_BLE_SERVICE_UUID =
            UUID.fromString("e1511a45-f3db-44c0-82b8-6c880790d1f1")
        private val BITBOX_BLE_WRITE_UUID =
            UUID.fromString("799d485c-d354-4ed0-b577-f8ee79ec275a")
        private val BITBOX_BLE_INDICATE_UUID =
            UUID.fromString("419572a5-9f53-4eb1-8db7-61bcab928867")

        private val BITBOX_BLE_NAME_PATTERN = Pattern.compile("(?i).*(bitbox|bb02).*")
    }
}

/** Error codes that mean the Noise session is gone and a fresh one must be initialized. */
fun BitBoxErrorCode.isSessionLost(): Boolean = this == BitBoxErrorCode.SESSION_LOST ||
    this == BitBoxErrorCode.DEVICE_NOISE_ENCRYPT ||
    this == BitBoxErrorCode.DEVICE_NOISE_DECRYPT

/** The user backed out — on the device or on the pairing screen. Not a fault to report. */
fun BitBoxErrorCode.isUserCancellation(): Boolean =
    this == BitBoxErrorCode.PAIRING_REJECTED || this == BitBoxErrorCode.USER_ABORT

/** Confluence §6's operation errors — the ones reported with the firmware's own error number. */
fun BitBoxErrorCode.isOperationError(): Boolean =
    this == BitBoxErrorCode.DEVICE_INVALID_INPUT ||
        this == BitBoxErrorCode.DEVICE ||
        this == BitBoxErrorCode.DEVICE_INVALID_STATE

/** Per-device runtime state + BLE write queue. */
@Suppress("DEPRECATION")
internal class BitBoxConnection(val id: String, val transport: BitBoxTransportKind) {
    var commandActive = false
    var activeRequest: BitBoxRequest? = null

    // BLE
    var bleDevice: BluetoothDevice? = null
    var gatt: BluetoothGatt? = null
    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var bleWriteType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    val bleWriteQueue = ArrayDeque<ByteArray>()
    var bleWriteInFlight: ByteArray? = null
    var bleWriteRetries = 0
    var bleNotifyReady = false

    // USB
    var usbDevice: UsbDevice? = null
    var usbConnection: UsbDeviceConnection? = null
    var usbInterface: UsbInterface? = null
    var usbInEndpoint: UsbEndpoint? = null
    var usbOutEndpoint: UsbEndpoint? = null
    var usbReaderRunning = false

    fun isReady(): Boolean = when (transport) {
        BitBoxTransportKind.BLE -> canWriteBle()
        BitBoxTransportKind.USB ->
            usbConnection != null && usbInEndpoint != null && usbOutEndpoint != null
    }

    /** Confluence §0: initialize only after indications *and* writes are ready. */
    fun canWriteBle(): Boolean = gatt != null && writeCharacteristic != null && bleNotifyReady

    fun finishCommand() {
        commandActive = false
        activeRequest = null
    }

    fun clearBleRuntime() {
        gatt = null
        writeCharacteristic = null
        bleWriteType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        bleWriteQueue.clear()
        bleWriteInFlight = null
        bleWriteRetries = 0
        bleNotifyReady = false
    }

    fun clearUsbRuntime() {
        usbReaderRunning = false
        val conn = usbConnection
        val iface = usbInterface
        if (conn != null && iface != null) runCatching { conn.releaseInterface(iface) }
        runCatching { conn?.close() }
        usbConnection = null
        usbInterface = null
        usbInEndpoint = null
        usbOutEndpoint = null
    }
}
