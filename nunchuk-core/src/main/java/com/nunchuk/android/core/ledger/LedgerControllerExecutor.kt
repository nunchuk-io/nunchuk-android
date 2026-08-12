package com.nunchuk.android.core.ledger

import com.nunchuk.android.core.domain.utils.LedgerCommandException
import com.nunchuk.android.core.domain.utils.LedgerCommandExecutor
import com.nunchuk.android.model.Wallet
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Adapts the callback-driven [LedgerBleController] into the suspend [LedgerCommandExecutor]
 * the [com.nunchuk.android.core.domain.utils.LedgerTransactionSigner] coordinator expects.
 * Each command starts on the main thread (the transport runs its step loop there) and
 * suspends until the controller reports COMPLETE (via [deliverComplete]) or FAILED (via
 * [deliverFailure]). The host activity wires the controller's command callbacks to these.
 */
class LedgerControllerExecutor(
    private val controller: LedgerBleController,
) : LedgerCommandExecutor {

    private var continuation: CancellableContinuation<String>? = null

    val isAwaiting: Boolean get() = continuation != null

    fun deliverComplete(result: String) {
        val cont = continuation ?: return
        continuation = null
        cont.resume(result)
    }

    fun deliverFailure(statusWord: Int, message: String) {
        val cont = continuation ?: return
        continuation = null
        cont.resumeWithException(LedgerCommandException(statusWord, message))
    }

    private suspend fun await(start: () -> Unit): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            continuation = cont
            cont.invokeOnCancellation { continuation = null }
            start()
        }
    }

    override suspend fun getMasterFingerprint(): String =
        await { controller.getMasterFingerprint() }

    override suspend fun registerWallet(wallet: Wallet): String =
        await { controller.registerWallet(wallet) }

    override suspend fun signPsbt(wallet: Wallet, hmac: String, psbt: String): String =
        await { controller.signPsbt(wallet, hmac, psbt) }

    override suspend fun getWalletAddress(
        wallet: Wallet,
        hmac: String,
        addressIndex: Int,
        change: Boolean,
    ): String = await { controller.getWalletAddress(wallet, hmac, addressIndex, change) }
}
