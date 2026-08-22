package com.nunchuk.android.core.bitbox

import com.nunchuk.android.model.BitBoxInitializeResult
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.BitBoxErrorCode
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A BitBox command reached `FAILED`. [code] is what decides recovery — the Confluence §6 error
 * table — so it is carried out rather than flattened into a message.
 */
class BitBoxCommandException(
    val code: BitBoxErrorCode,
    message: String,
    /** The firmware's own error number, 0 when the failure didn't come from the device. */
    val deviceCode: Int = 0,
) : Exception(message)

/**
 * The connected BitBox is not the signer being signed with — the user connected the wrong
 * device. Surfaced separately so the UI can ask for the right one instead of showing a protocol
 * error.
 */
class BitBoxWrongDeviceException(
    val expected: String,
    val actual: String,
) : Exception("Connected BitBox ($actual) does not match the selected key ($expected)")

/**
 * Adapts the callback-driven [BitBoxController] into suspend commands, the BitBox counterpart of
 * [com.nunchuk.android.core.ledger.LedgerControllerExecutor]. Signing is a multi-step
 * conversation — Confluence §3 is check-registration, register if needed, sign — and as a chain
 * of `onCommandComplete` branches that is a state machine with one branch per step; as a suspend
 * sequence it is the four lines the doc shows.
 *
 * Each command starts on the main thread (the transport runs its step loop there) and suspends
 * until the controller reports the matching request COMPLETE or FAILED. Unlike Ledger, results
 * are not carried through the continuation: BitBox returns structs, so each method reads its own
 * payload with the matching accessor and returns it typed.
 *
 * The host wires the controller's command callbacks to [deliverComplete] / [deliverFailure], and
 * must also call [deliverFailure] for the transport-level callbacks that carry no request
 * (`onError`, `onDisconnected`) — otherwise a dropped connection leaves the sequence suspended
 * forever.
 */
class BitBoxCommandExecutor(
    private val controller: BitBoxController,
) {
    private var continuation: CancellableContinuation<Unit>? = null

    /** The request the suspended command is waiting on, so a stale callback can't resume it. */
    private var awaitingRequest: BitBoxRequest? = null

    val isAwaiting: Boolean get() = continuation != null

    fun deliverComplete(request: BitBoxRequest) {
        if (request != awaitingRequest) return
        take()?.resume(Unit)
    }

    fun deliverFailure(
        request: BitBoxRequest?,
        code: BitBoxErrorCode,
        message: String,
        deviceCode: Int = 0,
    ) {
        // A null request is a transport-level failure (onError / onDisconnected): it kills
        // whatever is in flight, whichever command that is.
        if (request != null && request != awaitingRequest) return
        take()?.resumeWithException(BitBoxCommandException(code, message, deviceCode))
    }

    private fun take(): CancellableContinuation<Unit>? {
        val cont = continuation
        continuation = null
        awaitingRequest = null
        return cont
    }

    private suspend fun run(
        request: BitBoxRequest,
        start: () -> Unit,
    ) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            continuation = cont
            awaitingRequest = request
            cont.invokeOnCancellation { take() }
            start()
        }
    }

    /**
     * Opens a fresh native session. Every BitBox conversation starts here — the result is what
     * says whether the device is genuine, its firmware usable and the device set up — and it is
     * also the only way back from a lost session.
     */
    suspend fun initialize(): BitBoxInitializeResult? {
        run(BitBoxRequest.INITIALIZE) { controller.initialize() }
        return controller.initializeResult()
    }

    suspend fun getMasterFingerprint(): String {
        run(BitBoxRequest.MASTER_FINGERPRINT) { controller.getMasterFingerprint() }
        return controller.resultString()
    }

    suspend fun isWalletRegistered(wallet: Wallet): Boolean {
        run(BitBoxRequest.IS_WALLET_REGISTERED) { controller.isWalletRegistered(wallet) }
        return controller.resultBoolean()
    }

    suspend fun registerWallet(wallet: Wallet) {
        run(BitBoxRequest.REGISTER_WALLET) { controller.registerWallet(wallet) }
    }

    suspend fun signPsbt(wallet: Wallet, psbt: String): String {
        run(BitBoxRequest.SIGN_PSBT) { controller.signPsbt(wallet, psbt) }
        return controller.resultString()
    }

    /** Confluence §5: the device displays the address and reports what it derived. */
    suspend fun getWalletAddress(wallet: Wallet, addressIndex: Int, change: Boolean): String {
        run(BitBoxRequest.GET_WALLET_ADDRESS) {
            controller.getWalletAddress(wallet, addressIndex, change)
        }
        return controller.resultString()
    }

    suspend fun signMessage(derivationPath: String, message: String): String {
        run(BitBoxRequest.SIGN_MESSAGE) { controller.signMessage(derivationPath, message) }
        return controller.resultString()
    }
}
