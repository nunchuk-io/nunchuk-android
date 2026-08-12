package com.nunchuk.android.core.ledger

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
import android.bluetooth.BluetoothProfile
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
import com.nunchuk.android.model.LedgerStep
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.LedgerStepType
import com.nunchuk.android.type.LedgerTransport
import com.nunchuk.android.type.LedgerUserInteraction
import com.nunchuk.android.type.WalletType
import timber.log.Timber
import java.util.ArrayDeque
import java.util.UUID
import java.util.regex.Pattern
import kotlin.concurrent.thread

enum class LedgerTransportKind {
    BLE,
    USB,
}

enum class LedgerRequest {
    MASTER_FINGERPRINT,
    XPUB,
    SIGN_MESSAGE,
    REGISTER_WALLET,
    SIGN_PSBT,
    GET_WALLET_ADDRESS,
}

data class LedgerDevice(
    val id: String,
    val name: String,
    val transport: LedgerTransportKind,
)

/**
 * Drives the Ledger transports (BLE + USB-HID) and the native step loop for the
 * standalone add-key flow. Ported from the reference `ledger-android`, adapted to the
 * NunchukNativeSdk `ledger*` bindings.
 *
 * Contract highlights (Confluence "Implement"):
 * - Stable session_id per device: BLE address / USB deviceName.
 * - BLE write queue: one frame in flight, next only after onCharacteristicWrite.
 * - Commands run only after the transport is fully ready.
 * - APP_SWITCH resumes once ready again (reconnect if the transport dropped). resume() is
 *   only valid while that app switch is pending, so a drop during any other command is
 *   reported as an error rather than resumed.
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class LedgerBleController(
    private val context: Context,
    private val nativeSdk: NunchukNativeSdk,
    private val listener: Listener,
) {
    interface Listener {
        fun onScanResults(devices: List<LedgerDevice>)
        fun onScanFinished(found: Int)
        fun onConnecting(device: LedgerDevice)
        fun onInteraction(interaction: LedgerUserInteraction)
        fun onCommandComplete(request: LedgerRequest, result: String)

        /**
         * A command reached FAILED. [statusWord] is the device status word (0 if unknown);
         * e.g. 0xB008 (SW_INVALID_SIGNATURE_OR_HMAC) signals a stale wallet registration.
         */
        fun onCommandFailed(request: LedgerRequest, statusWord: Int, message: String)
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
    private val foundDevices = linkedMapOf<String, LedgerDevice>()

    private var connection: LedgerConnection? = null
    private var pendingReadyAction: (() -> Unit)? = null

    private var usbReceiverRegistered = false

    /** Pending "the Ledger never re-enumerated" fallback for a USB app switch. */
    private var usbAppSwitchGrace: Runnable? = null

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
        // USB Ledgers are already attached (no scan needed); list them up front.
        enumerateUsbDevices()
        // Already-paired BLE Ledgers may not advertise; seed them too.
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            // Keep USB devices visible; ask the host to prompt the user to enable BT.
            if (foundDevices.isNotEmpty()) listener.onScanResults(foundDevices.values.toList())
            listener.onBluetoothDisabled()
            return
        }
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
                if (!isLedgerBleScanResult(result)) return
                val name = result.scanRecord?.deviceName
                    ?: runCatching { result.device.name }.getOrNull()
                foundDevices[result.device.address] = LedgerDevice(
                    id = result.device.address,
                    name = name?.takeIf { it.isNotBlank() } ?: result.device.address,
                    transport = LedgerTransportKind.BLE,
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
            if (name != null && LEDGER_BLE_NAME_PATTERN.matcher(name).matches()) {
                foundDevices[device.address] =
                    LedgerDevice(device.address, name, LedgerTransportKind.BLE)
            }
        }
        if (foundDevices.isNotEmpty()) listener.onScanResults(foundDevices.values.toList())
    }

    /**
     * Lists attached USB Ledgers only — no Bluetooth permission or scan. Used when the
     * user enters via "Add Ledger via USB", so we don't prompt for BT permission there.
     */
    fun refreshUsb() {
        registerUsbReceiver()
        foundDevices.entries.removeAll { it.value.transport == LedgerTransportKind.USB }
        enumerateUsbDevices()
        listener.onScanResults(foundDevices.values.toList())
    }

    private fun enumerateUsbDevices() {
        val devices = usbManager?.deviceList?.values.orEmpty()
            .filter { it.vendorId == LEDGER_USB_VENDOR_ID }
        devices.forEach { device ->
            foundDevices[device.deviceName] = LedgerDevice(
                id = device.deviceName,
                name = device.productName ?: "Ledger (USB)",
                transport = LedgerTransportKind.USB,
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

    private fun isLedgerBleScanResult(result: ScanResult): Boolean {
        val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull()
        if (name != null && LEDGER_BLE_NAME_PATTERN.matcher(name).matches()) return true
        return result.scanRecord?.serviceUuids.orEmpty().any { uuid ->
            LEDGER_BLE_SPECS.any { spec -> spec.serviceUuid == uuid.uuid }
        }
    }
    // endregion

    // region connect (transport dispatch)
    fun connect(device: LedgerDevice) {
        Timber.tag(TAG).d("connect ${device.name} id=${device.id} transport=${device.transport}")
        stopScan()
        // Tear down a prior connection to a different device (avoid leaking its runtime).
        connection?.takeIf { it.id != device.id }?.let {
            teardown(it)
            connection = null
        }
        when (device.transport) {
            LedgerTransportKind.BLE -> connectBleById(device.id)
            LedgerTransportKind.USB -> connectUsbById(device.id)
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
            listener.onError("Ledger device unavailable")
            return
        }
        connectBle(device, connection)
    }

    private fun connectBle(device: BluetoothDevice, existing: LedgerConnection? = null) {
        val conn = existing?.takeIf { it.transport == LedgerTransportKind.BLE }
            ?: LedgerConnection(device.address, LedgerTransportKind.BLE).also { connection = it }
        conn.bleDevice = device
        listener.onConnecting(LedgerDevice(device.address, device.name ?: device.address, LedgerTransportKind.BLE))
        conn.gatt?.let { old ->
            runCatching { old.disconnect() }
            runCatching { old.close() }
            conn.clearBleRuntime()
        }
        conn.gatt = device.connectGatt(context, false, bleCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val bleCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            mainHandler.post { handleConnectionStateChange(gatt, newState) }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mainHandler.post {
                connection?.bleMtuPayload = (mtu - 3).coerceAtLeast(20)
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            mainHandler.post { handleServicesDiscovered(gatt) }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            mainHandler.post {
                val spec = LEDGER_BLE_SPECS.firstOrNull { gatt.getService(it.serviceUuid) != null }
                if (spec != null && status == BluetoothGatt.GATT_SUCCESS) completeBleReady(gatt)
                else listener.onError("BLE notification setup failed")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            mainHandler.post { handleCharacteristicWrite(status) }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value?.copyOf() ?: ByteArray(0)
            mainHandler.post { onData(data) }
        }
    }

    private fun handleConnectionStateChange(gatt: BluetoothGatt, newState: Int) {
        val conn = connection?.takeIf { it.transport == LedgerTransportKind.BLE } ?: return
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            if (!gatt.requestMtu(LEDGER_BLE_GATT_MTU)) gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (conn.gatt !== gatt && conn.gatt != null) {
                runCatching { gatt.close() }
                return
            }
            runCatching { gatt.close() }
            conn.clearBleRuntime()
            when {
                // Dropped while the device opens the Bitcoin app: reconnect, and
                // onTransportReady() resumes the queued command once we're back.
                conn.resumePending -> connectBle(gatt.device, conn)
                // Any other mid-command drop cannot be resumed: LedgerSession only accepts
                // resume() while an app switch is pending (once the command itself has been
                // written, resume() answers "no command to resume after app switch"). Fail
                // the command instead so the user can reconnect and retry.
                conn.commandActive -> fail("Ledger disconnected. Reconnect and try again.")
            }
        }
    }

    private fun handleServicesDiscovered(gatt: BluetoothGatt) {
        val conn = connection ?: return
        val match = LEDGER_BLE_SPECS.firstNotNullOfOrNull { spec ->
            gatt.getService(spec.serviceUuid)?.let { spec to it }
        }
        if (match == null) {
            fail("Ledger BLE service not found")
            return
        }
        val (spec, service) = match
        val notify = service.getCharacteristic(spec.notifyUuid)
        val writeWithResponse = service.getCharacteristic(spec.writeUuid)
        val write = writeWithResponse ?: service.getCharacteristic(spec.writeCmdUuid)
        if (notify == null || write == null) {
            fail("Ledger BLE characteristics missing")
            return
        }
        conn.writeCharacteristic = write
        conn.bleWriteType = if (writeWithResponse != null) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        gatt.setCharacteristicNotification(notify, true)
        val cccd = notify.getDescriptor(CCCD_UUID)
        if (cccd != null) {
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (gatt.writeDescriptor(cccd)) return
        }
        completeBleReady(gatt)
    }

    private fun completeBleReady(gatt: BluetoothGatt) {
        val conn = connection ?: return
        if (conn.gatt !== gatt || conn.writeCharacteristic == null) return
        conn.bleNotifyReady = true
        onTransportReady()
    }

    private fun handleCharacteristicWrite(status: Int) {
        val conn = connection ?: return
        conn.bleWriteInFlight = null
        if (status == BluetoothGatt.GATT_SUCCESS) {
            conn.bleWriteRetries = 0
            if (conn.bleWriteQueue.isNotEmpty()) conn.bleWriteQueue.removeFirst()
            writeNextBleFrame()
        } else {
            retryBleWrite()
        }
    }

    private fun writeBleFrames(frames: List<ByteArray>) {
        val conn = connection ?: return
        conn.bleWriteQueue.clear()
        conn.bleWriteQueue.addAll(frames)
        conn.bleWriteInFlight = null
        conn.bleWriteRetries = 0
        writeNextBleFrame()
    }

    private fun writeNextBleFrame() {
        val conn = connection ?: return
        if (conn.bleWriteInFlight != null || conn.bleWriteQueue.isEmpty()) return
        val frame = conn.bleWriteQueue.first()
        val gatt = conn.gatt ?: return
        val characteristic = conn.writeCharacteristic ?: return
        characteristic.value = frame
        characteristic.writeType = conn.bleWriteType
        conn.bleWriteInFlight = frame
        if (!gatt.writeCharacteristic(characteristic)) {
            conn.bleWriteInFlight = null
            retryBleWrite()
        }
    }

    private fun retryBleWrite() {
        val conn = connection ?: return
        if (conn.bleWriteQueue.isEmpty()) return
        if (conn.bleWriteRetries >= BLE_WRITE_RETRY_ATTEMPTS) {
            conn.bleWriteQueue.clear()
            conn.finishCommand()
            fail("BLE write failed")
            return
        }
        conn.bleWriteRetries += 1
        mainHandler.postDelayed({ if (connection?.canWriteBle() == true) writeNextBleFrame() }, BLE_WRITE_RETRY_DELAY_MS)
    }
    // endregion

    // region USB
    private fun connectUsbById(deviceName: String) {
        val device = usbManager?.deviceList?.get(deviceName) ?: run {
            listener.onError("Ledger USB device unavailable")
            return
        }
        val conn = connection?.takeIf { it.id == deviceName && it.transport == LedgerTransportKind.USB }
            ?: LedgerConnection(deviceName, LedgerTransportKind.USB).also { connection = it }
        conn.usbDevice = device
        listener.onConnecting(LedgerDevice(deviceName, device.productName ?: deviceName, LedgerTransportKind.USB))
        if (usbManager.hasPermission(device)) {
            openUsbDevice(device)
        } else {
            requestUsbPermission(device)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName), flags,
        )
        usbManager?.requestPermission(device, intent)
    }

    private fun openUsbDevice(device: UsbDevice) {
        val conn = connection?.takeIf { it.transport == LedgerTransportKind.USB } ?: return
        val hidInterface = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_HID }
        if (hidInterface == null) {
            fail("Ledger HID interface not found")
            return
        }
        var inEndpoint: UsbEndpoint? = null
        var outEndpoint: UsbEndpoint? = null
        for (i in 0 until hidInterface.endpointCount) {
            val endpoint = hidInterface.getEndpoint(i)
            if (endpoint.direction == UsbConstants.USB_DIR_IN) inEndpoint = endpoint
            if (endpoint.direction == UsbConstants.USB_DIR_OUT) outEndpoint = endpoint
        }
        if (inEndpoint == null || outEndpoint == null) {
            fail("Ledger HID endpoints not found")
            return
        }
        conn.clearUsbRuntime()
        val usbConnection = usbManager?.openDevice(device)
        if (usbConnection == null || !usbConnection.claimInterface(hidInterface, true)) {
            runCatching { usbConnection?.close() }
            fail("Failed to open Ledger USB device")
            return
        }
        conn.usbConnection = usbConnection
        conn.usbInterface = hidInterface
        conn.usbInEndpoint = inEndpoint
        conn.usbOutEndpoint = outEndpoint
        startUsbReader(conn)
        // The handle is live again, so the "never re-enumerated" fallback must not fire and
        // resume a second time.
        cancelUsbAppSwitchGrace()
        onTransportReady()
    }

    /**
     * The Ledger re-enumerates while it opens the Bitcoin app: the handle we hold dies and the
     * device comes back as a different [UsbDevice]. Rebind to it — keeping [LedgerConnection.id]
     * so the native session (and the command queued behind the app switch) stays the same — and
     * let [onTransportReady] resume once the HID endpoints are open again.
     */
    private fun onUsbAttached(device: UsbDevice) {
        if (device.vendorId != LEDGER_USB_VENDOR_ID) return
        foundDevices[device.deviceName] = LedgerDevice(
            id = device.deviceName,
            name = device.productName ?: "Ledger (USB)",
            transport = LedgerTransportKind.USB,
        )
        listener.onScanResults(foundDevices.values.toList())

        val conn = connection?.takeIf {
            it.transport == LedgerTransportKind.USB && it.resumePending && it.usbConnection == null
        } ?: return
        Timber.tag(TAG).d("USB re-attached as ${device.deviceName}; rebinding session ${conn.id}")
        cancelUsbAppSwitchGrace()
        conn.usbDevice = device
        listener.onConnecting(
            LedgerDevice(conn.id, device.productName ?: conn.id, LedgerTransportKind.USB)
        )
        if (usbManager?.hasPermission(device) == true) {
            openUsbDevice(device)
        } else {
            requestUsbPermission(device)
        }
    }

    private fun onUsbDetached(device: UsbDevice) {
        foundDevices.remove(device.deviceName)
        listener.onScanResults(foundDevices.values.toList())

        val conn = connection?.takeIf { it.transport == LedgerTransportKind.USB } ?: return
        if (conn.usbDevice?.deviceName != device.deviceName) return
        Timber.tag(TAG).d("USB detached ${device.deviceName} resumePending=${conn.resumePending}")
        conn.clearUsbRuntime()
        when {
            // Detached because of the app switch — onUsbAttached() picks it back up.
            conn.resumePending -> Unit
            conn.commandActive -> fail("Ledger disconnected. Reconnect and try again.")
        }
    }

    /**
     * Fallback for a USB app switch on a device that keeps its USB configuration: if nothing
     * detached within the grace period, resume on the handle we already have.
     */
    private fun scheduleUsbAppSwitchResume(conn: LedgerConnection) {
        cancelUsbAppSwitchGrace()
        val grace = Runnable {
            usbAppSwitchGrace = null
            if (connection !== conn || !conn.resumePending || !conn.isReady()) return@Runnable
            Timber.tag(TAG).d("USB app switch: no re-enumeration, resuming on the open handle")
            onTransportReady()
        }
        usbAppSwitchGrace = grace
        mainHandler.postDelayed(grace, USB_APP_SWITCH_GRACE_MS)
    }

    private fun cancelUsbAppSwitchGrace() {
        usbAppSwitchGrace?.let { mainHandler.removeCallbacks(it) }
        usbAppSwitchGrace = null
    }

    private fun writeUsbFrames(frames: List<ByteArray>) {
        val conn = connection
        val usbConnection = conn?.usbConnection
        val endpoint = conn?.usbOutEndpoint
        if (usbConnection == null || endpoint == null) {
            // Writing with no open handle used to return silently, leaving the command hanging
            // with nothing on screen.
            fail("Ledger USB connection is not open")
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

    private fun startUsbReader(conn: LedgerConnection) {
        val usbConnection = conn.usbConnection ?: return
        val endpoint = conn.usbInEndpoint ?: return
        conn.usbReaderRunning = true
        thread(name = "ledger-usb-reader-${conn.id}") {
            val buffer = ByteArray(endpoint.maxPacketSize.coerceAtLeast(1))
            // Also stop once this handle is replaced — after a re-enumeration the old reader
            // would otherwise keep pulling frames off the new connection alongside its reader.
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
        // ATTACHED/DETACHED keep the picker in sync and drive the re-enumeration that follows
        // an app switch; both are protected system broadcasts, so NOT_EXPORTED still receives
        // them while keeping our own permission action app-private.
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

    // region commands + step loop (transport-agnostic)
    fun getMasterFingerprint() = startCommand(LedgerRequest.MASTER_FINGERPRINT) { id, transport ->
        nativeSdk.ledgerGetMasterFingerprint(id, transport)
    }

    fun getExtendedPublicKey(walletType: WalletType, addressType: AddressType, index: Int) =
        startCommand(LedgerRequest.XPUB) { id, transport ->
            nativeSdk.ledgerGetExtendedPublicKey(id, transport, walletType, addressType, index)
        }

    /**
     * Confluence "Sign message" — signs [message] with the key at [derivationPath].
     * Used for the standalone health check: the resulting signature is delivered via
     * [Listener.onCommandComplete] and verified with `HealthCheckSingleSigner`.
     */
    fun signMessage(derivationPath: String, message: String) =
        startCommand(LedgerRequest.SIGN_MESSAGE) { id, transport ->
            nativeSdk.ledgerSignMessage(id, transport, derivationPath, message)
        }

    /**
     * Confluence "Sign transaction" — registers [wallet] on the device. The completion
     * result (via [Listener.onCommandComplete]) is the wallet HMAC to cache and reuse.
     */
    fun registerWallet(wallet: Wallet) =
        startCommand(LedgerRequest.REGISTER_WALLET) { id, transport ->
            nativeSdk.ledgerRegisterWallet(id, transport, wallet)
        }

    /**
     * Confluence "Sign transaction" — signs [psbt] with the registered [wallet]. Requires the
     * wallet [hmac] from a prior registration. The completion result is the signed PSBT.
     */
    fun signPsbt(wallet: Wallet, hmac: String, psbt: String) =
        startCommand(LedgerRequest.SIGN_PSBT) { id, transport ->
            nativeSdk.ledgerSignPsbt(id, transport, wallet, hmac, psbt)
        }

    /**
     * Confluence "Show address on device" — shows the registered [wallet]'s address at
     * [addressIndex] on the device screen. Requires the wallet [hmac] from a prior
     * registration. The completion result is the address the device derived.
     */
    fun getWalletAddress(wallet: Wallet, hmac: String, addressIndex: Int, change: Boolean) =
        startCommand(LedgerRequest.GET_WALLET_ADDRESS) { id, transport ->
            nativeSdk.ledgerGetWalletAddress(id, transport, wallet, hmac, addressIndex, change)
        }

    private fun startCommand(request: LedgerRequest, block: (String, LedgerTransport) -> LedgerStep) {
        val conn = connection ?: run {
            Timber.tag(TAG).e("startCommand($request): no connection")
            listener.onError("Ledger not connected")
            return
        }
        Timber.tag(TAG).d("startCommand($request) session=${conn.id} transport=${conn.transport}")
        conn.activeRequest = request
        conn.commandActive = true
        runLedger { handleStep(block(conn.id, conn.transport.toNative())) }
    }

    private fun onTransportReady() {
        val conn = connection ?: return
        Timber.tag(TAG).d("onTransportReady resumePending=${conn.resumePending} pendingReadyAction=${pendingReadyAction != null}")
        if (conn.resumePending) {
            runLedger { handleStep(nativeSdk.ledgerResume(conn.id)) }
        } else {
            pendingReadyAction?.let { action ->
                pendingReadyAction = null
                action()
            }
        }
    }

    private fun onData(data: ByteArray) {
        val conn = connection ?: return
        Timber.tag(TAG).d("onData ${data.size} bytes: ${data.toHexPreview()}")
        runLedger { handleStep(nativeSdk.ledgerOnData(conn.id, data)) }
    }

    private fun handleStep(step: LedgerStep) {
        Timber.tag(TAG).d("handleStep type=${step.stepType} interaction=${step.userInteraction} writes=${step.writes.size} request=${connection?.activeRequest}")
        listener.onInteraction(step.userInteraction)
        val conn = connection ?: return
        when (step.stepType) {
            LedgerStepType.WRITE -> {
                conn.commandActive = true
                writeFrames(step.writes)
            }
            LedgerStepType.READ_MORE -> {
                conn.commandActive = true
            }
            LedgerStepType.COMPLETE -> {
                val request = conn.activeRequest
                val result = runCatching { nativeSdk.ledgerResultString(conn.id) }.getOrDefault("")
                Timber.tag(TAG).d("COMPLETE request=$request resultLength=${result.length}")
                conn.finishCommand()
                if (request != null) listener.onCommandComplete(request, result)
            }
            LedgerStepType.FAILED -> {
                val request = conn.activeRequest
                val statusWord = step.error?.statusWord ?: 0
                val message = step.error?.message?.takeIf { it.isNotBlank() } ?: "Ledger error"
                Timber.tag(TAG).e(
                    "FAILED request=$request statusWord=0x%04X code=%d message=%s",
                    statusWord, step.error?.code ?: 0, message,
                )
                conn.finishCommand()
                if (request != null) {
                    listener.onCommandFailed(request, statusWord, message)
                } else {
                    listener.onError(message)
                }
            }
            LedgerStepType.APP_SWITCH -> {
                Timber.tag(TAG).d("APP_SWITCH")
                handleAppSwitch()
            }
        }
    }

    private fun handleAppSwitch() {
        val conn = connection ?: return
        conn.commandActive = true
        conn.resumePending = true
        when (conn.transport) {
            // BLE: resume straight away if the link survived, otherwise on reconnect.
            LedgerTransportKind.BLE -> if (conn.isReady()) {
                onTransportReady()
            } else {
                val device = conn.bleDevice ?: conn.gatt?.device
                if (device == null) fail("Ledger BLE device is unavailable") else connectBle(device, conn)
            }

            LedgerTransportKind.USB -> handleUsbAppSwitch(conn)
        }
    }

    /**
     * APP_SWITCH over USB: the handle we hold is about to die because the Ledger re-enumerates
     * while it opens the Bitcoin app, so never resume on it straight away — wait for the
     * detach/attach pair ([onUsbAttached] resumes), with [scheduleUsbAppSwitchResume] covering
     * a device that stays enumerated.
     */
    private fun handleUsbAppSwitch(conn: LedgerConnection) {
        listener.onConnecting(
            LedgerDevice(conn.id, conn.usbDevice?.productName ?: conn.id, LedgerTransportKind.USB)
        )
        scheduleUsbAppSwitchResume(conn)
    }

    private fun writeFrames(frames: List<ByteArray>) {
        Timber.tag(TAG).d("writeFrames ${frames.size} frame(s) via ${connection?.transport}")
        when (connection?.transport) {
            LedgerTransportKind.BLE -> writeBleFrames(frames)
            LedgerTransportKind.USB -> writeUsbFrames(frames)
            null -> Unit
        }
    }

    private inline fun runLedger(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "Ledger native error")
            connection?.finishCommand()
            listener.onError(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun fail(message: String) {
        Timber.tag(TAG).e("fail: $message (activeRequest=${connection?.activeRequest})")
        connection?.finishCommand()
        listener.onError(message)
    }
    // endregion

    private fun teardown(conn: LedgerConnection) {
        runCatching { conn.gatt?.disconnect() }
        runCatching { conn.gatt?.close() }
        conn.clearBleRuntime()
        conn.clearUsbRuntime()
        conn.finishCommand()
    }

    fun close() {
        stopScan()
        cancelUsbAppSwitchGrace()
        unregisterUsbReceiver()
        // Drop pending scan-timeout / write-retry callbacks so they can't keep this
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

    private fun LedgerTransportKind.toNative(): LedgerTransport = when (this) {
        LedgerTransportKind.BLE -> LedgerTransport.BLE
        LedgerTransportKind.USB -> LedgerTransport.USB_HID
    }

    /** First bytes of a frame as hex, for logging (APDU/response headers are the useful part). */
    private fun ByteArray.toHexPreview(max: Int = 24): String {
        val shown = take(max).joinToString(" ") { "%02X".format(it) }
        return if (size > max) "$shown … (${size}B)" else shown
    }

    companion object {
        private const val TAG = "LedgerBle"
        private const val BLE_SCAN_TIMEOUT_MS = 12_000L
        private const val LEDGER_BLE_GATT_MTU = 156
        private const val BLE_WRITE_RETRY_ATTEMPTS = 8
        private const val BLE_WRITE_RETRY_DELAY_MS = 120L
        private const val USB_TIMEOUT_MS = 5_000
        // How long to wait for the Ledger to re-enumerate after a USB app switch before
        // assuming it kept its USB configuration and resuming on the handle we already have.
        private const val USB_APP_SWITCH_GRACE_MS = 2_500L
        // Every Ledger model shares this vendor id, so matching on it alone covers them all.
        // Mirrored in decimal by nunchuk-app's res/xml/ledger_usb_device_filter.xml, which
        // decides whether Nunchuk is offered when one is plugged in — change both together.
        private const val LEDGER_USB_VENDOR_ID = 0x2c97
        private const val ACTION_USB_PERMISSION = "com.nunchuk.android.signer.ledger.USB_PERMISSION"
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val LEDGER_BLE_NAME_PATTERN = Pattern.compile("(?i).*(ledger|nano|stax|flex).*")

        private val LEDGER_BLE_SPECS = listOf("0004", "6004", "3004", "8004", "9004").map { model ->
            BleSpec(
                serviceUuid = UUID.fromString("13d63400-2c97-$model-0000-4c6564676572"),
                notifyUuid = UUID.fromString("13d63400-2c97-$model-0001-4c6564676572"),
                writeUuid = UUID.fromString("13d63400-2c97-$model-0002-4c6564676572"),
                writeCmdUuid = UUID.fromString("13d63400-2c97-$model-0003-4c6564676572"),
            )
        }

        private data class BleSpec(
            val serviceUuid: UUID,
            val notifyUuid: UUID,
            val writeUuid: UUID,
            val writeCmdUuid: UUID,
        )
    }
}

/** Per-device runtime state + BLE write queue (BLE + USB port of the reference LedgerConnection). */
@Suppress("DEPRECATION")
internal class LedgerConnection(val id: String, val transport: LedgerTransportKind) {
    var commandActive = false
    var resumePending = false
    var activeRequest: LedgerRequest? = null

    // BLE
    var bleDevice: BluetoothDevice? = null
    var gatt: BluetoothGatt? = null
    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var bleWriteType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    val bleWriteQueue = ArrayDeque<ByteArray>()
    var bleWriteInFlight: ByteArray? = null
    var bleWriteRetries = 0
    var bleMtuPayload = 20
    var bleNotifyReady = false

    // USB
    var usbDevice: UsbDevice? = null
    var usbConnection: UsbDeviceConnection? = null
    var usbInterface: UsbInterface? = null
    var usbInEndpoint: UsbEndpoint? = null
    var usbOutEndpoint: UsbEndpoint? = null
    var usbReaderRunning = false

    fun isReady(): Boolean = when (transport) {
        LedgerTransportKind.BLE -> canWriteBle()
        LedgerTransportKind.USB -> usbConnection != null && usbInEndpoint != null && usbOutEndpoint != null
    }

    fun canWriteBle(): Boolean = gatt != null && writeCharacteristic != null && bleNotifyReady

    fun finishCommand() {
        commandActive = false
        resumePending = false
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
