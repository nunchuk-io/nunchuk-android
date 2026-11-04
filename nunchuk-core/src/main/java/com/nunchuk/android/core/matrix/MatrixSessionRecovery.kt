package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.network.ApiErrorCode
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.persistence.NcEncryptedPreferences
import com.nunchuk.android.core.profile.UserRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.domain.di.MainDispatcher
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isTokenError
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class MatrixConnectionState {
    /** No known problem. A local session alone is not proof the server accepts it. */
    UNKNOWN,
    RECOVERING,

    /** Network / Matrix 5xx / rate limit. Credentials and crypto data are kept; [MatrixSessionRecovery.retry] may help. */
    TEMPORARILY_UNAVAILABLE,

    /** Matrix rejected the credentials and re-authentication did not fix it. */
    INVALID_CREDENTIALS,

    /** The Nunchuk API session is revoked/expired — the normal sign-in flow takes over. */
    API_UNAUTHORIZED,

    /** Account-level rejection (Matrix account expired/deactivated, API account locked). Never retried automatically. */
    ACCOUNT_REJECTED,
}

internal enum class MatrixFailureCategory { TRANSIENT, INVALID_CREDENTIALS, ACCOUNT_REJECTED, UNEXPECTED }

internal fun Throwable.matrixFailureCategory(): MatrixFailureCategory {
    val serverError = (this as? Failure.ServerError)?.error
    return when {
        serverError?.code == MatrixError.M_USER_DEACTIVATED -> MatrixFailureCategory.ACCOUNT_REJECTED
        isTokenError() || isInvalidPassword() -> MatrixFailureCategory.INVALID_CREDENTIALS
        this is Failure.NetworkConnection || this is IOException || shouldBeRetried() ->
            MatrixFailureCategory.TRANSIENT
        this is Failure.ServerError && httpCode >= 500 -> MatrixFailureCategory.TRANSIENT
        this is Failure.ServerError -> MatrixFailureCategory.INVALID_CREDENTIALS
        // A local bug or SDK state error says nothing about the credentials; leave it to a manual retry.
        else -> MatrixFailureCategory.UNEXPECTED
    }
}

/**
 * Recovers a Matrix session whose credentials the homeserver rejected, without a full app logout.
 *
 * - One recovery at a time, and at most [MAX_RECOVERY_ATTEMPTS] per account per process, so a
 *   server that keeps rejecting us cannot make the app mint Matrix devices in a loop.
 * - The Nunchuk API session is checked first; if it is revoked the normal sign-in flow handles it.
 * - Soft logout re-authenticates the same device ([org.matrix.android.sdk.api.session.signout.SignOutService.signInAgain]),
 *   which keeps the crypto store and encrypted history. Hard logout means the server deleted the
 *   device, so a new login is unavoidable; the old session is closed but its data is NOT deleted.
 * - [cancel] (logout / account switch) bumps a generation counter, so a late result is discarded.
 */
@Singleton
class MatrixSessionRecovery @Inject constructor(
    private val matrix: Matrix,
    private val matrixProvider: MatrixProvider,
    private val headerProvider: HeaderProvider,
    private val accountManager: AccountManager,
    private val userRepository: UserRepository,
    private val encryptedPreferences: NcEncryptedPreferences,
    private val sessionHolder: Lazy<SessionHolder>,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val generation = AtomicInteger(0)
    private var attempts = 0
    private var job: Job? = null
    private var lastRequest: Request? = null
    private var stateChatId: String? = null

    private val _state = MutableStateFlow(MatrixConnectionState.UNKNOWN)
    val state: StateFlow<MatrixConnectionState> = _state.asStateFlow()

    private data class Request(val session: Session, val softLogout: Boolean)

    fun onInvalidToken(session: Session, softLogout: Boolean) {
        start(Request(session, softLogout), userInitiated = false)
    }

    @Synchronized
    fun onAccountExpired() {
        record("matrix account expired")
        stateChatId = accountManager.getAccount().chatId
        _state.value = MatrixConnectionState.ACCOUNT_REJECTED
    }

    /** User-visible retry after a temporary failure or exhausted attempts. */
    @Synchronized
    fun retry() {
        lastRequest?.let { start(it, userInitiated = true) }
    }

    @Synchronized
    fun cancel() {
        generation.incrementAndGet()
        job?.cancel()
        job = null
        attempts = 0
        lastRequest = null
        stateChatId = null
        _state.value = MatrixConnectionState.UNKNOWN
    }

    @Synchronized
    private fun start(request: Request, userInitiated: Boolean) {
        if (job?.isActive == true) return
        val chatId = accountManager.getAccount().chatId
        if (chatId.isBlank() || request.session.myUserId != chatId) return
        if (stateChatId != chatId) {
            // Attempts and rejections belong to one account; a newly signed-in account starts clean.
            stateChatId = chatId
            attempts = 0
            _state.value = MatrixConnectionState.UNKNOWN
        }
        if (_state.value == MatrixConnectionState.ACCOUNT_REJECTED ||
            _state.value == MatrixConnectionState.API_UNAUTHORIZED
        ) return
        if (userInitiated) attempts = 0
        if (attempts >= MAX_RECOVERY_ATTEMPTS) {
            _state.value = MatrixConnectionState.INVALID_CREDENTIALS
            return
        }
        attempts++
        lastRequest = request
        val gen = generation.get()
        _state.value = MatrixConnectionState.RECOVERING
        job = scope.launch {
            val outcome = try {
                recover(request, chatId, gen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                when (e.matrixFailureCategory()) {
                    MatrixFailureCategory.TRANSIENT -> MatrixConnectionState.TEMPORARILY_UNAVAILABLE
                    MatrixFailureCategory.INVALID_CREDENTIALS -> MatrixConnectionState.INVALID_CREDENTIALS
                    MatrixFailureCategory.ACCOUNT_REJECTED -> MatrixConnectionState.ACCOUNT_REJECTED
                    MatrixFailureCategory.UNEXPECTED -> MatrixConnectionState.TEMPORARILY_UNAVAILABLE
                }
            }
            if (generation.get() == gen) {
                record("matrix recovery outcome=$outcome softLogout=${request.softLogout}")
                _state.value = outcome
            }
        }
    }

    private suspend fun recover(request: Request, chatId: String, gen: Int): MatrixConnectionState {
        when (checkApiSession(chatId)) {
            ApiCheck.OK -> Unit
            ApiCheck.UNAUTHORIZED -> return MatrixConnectionState.API_UNAUTHORIZED
            ApiCheck.WRONG_ACCOUNT -> return MatrixConnectionState.ACCOUNT_REJECTED
            ApiCheck.FAILED -> return MatrixConnectionState.TEMPORARILY_UNAVAILABLE
        }
        val password = accountManager.getAccount().token
        if (password.isBlank()) return MatrixConnectionState.API_UNAUTHORIZED

        return if (request.softLogout) {
            recoverSoftLogout(request.session, password, gen)
        } else {
            recoverHardLogout(request.session, chatId, password, gen)
        }
    }

    private suspend fun recoverSoftLogout(session: Session, password: String, gen: Int): MatrixConnectionState {
        withTransientRetry { session.signOutService().signInAgain(password) }
        return withContext(mainDispatcher) {
            // Checked on the same thread that installs, so a logout cannot slip in between.
            if (!isCurrent(gen, session.myUserId)) return@withContext _state.value
            encryptedPreferences.setMatrixCredential(session.sessionParams.credentials)
            session.syncService().startSync(true)
            MatrixEvenBus.instance.publish(MatrixEvent.SignedInEvent(session))
            MatrixConnectionState.UNKNOWN
        }
    }

    private suspend fun recoverHardLogout(
        oldSession: Session,
        chatId: String,
        password: String,
        gen: Int,
    ): MatrixConnectionState {
        withContext(mainDispatcher) {
            runCatching {
                oldSession.syncService().stopSync()
                oldSession.close()
            }
        }
        // The cached credentials belong to the device the server deleted; never restore them again.
        encryptedPreferences.clearMatrixCredential(chatId)
        val authenticationService = matrix.authenticationService()
        val newSession = withTransientRetry {
            authenticationService.directAuthentication(
                homeServerConnectionConfig = matrixProvider.getServerConfig(),
                matrixId = chatId,
                password = password,
                initialDeviceName = headerProvider.getDeviceName(),
            )
        }
        authenticationService.reset()
        // On Main: SignedInEvent listeners (MainActivity, RoomsViewModel) touch main-thread-only Matrix live data,
        // the normal login path installs sessions there too, and the account check cannot race a logout.
        val installed = withContext(mainDispatcher) {
            if (!isCurrent(gen, chatId)) return@withContext false
            sessionHolder.get().storeActiveSession(newSession)
            MatrixEvenBus.instance.publish(MatrixEvent.SignedInEvent(newSession))
            true
        }
        if (!installed) {
            // Logged out / switched account while we were authenticating: drop the new device.
            runCatching { newSession.signOutService().signOut(true, true) }
            return _state.value
        }
        return MatrixConnectionState.UNKNOWN
    }

    private enum class ApiCheck { OK, UNAUTHORIZED, WRONG_ACCOUNT, FAILED }

    private suspend fun checkApiSession(chatId: String): ApiCheck = try {
        val profile = withTransientRetry { userRepository.getUserProfile() }
        if (profile.chatId == chatId) ApiCheck.OK else ApiCheck.WRONG_ACCOUNT
    } catch (e: NunchukApiException) {
        // 401 already triggered the app's sign-in flow (Data publishes UnauthorizedEventBus).
        if (e.code == ApiErrorCode.UNAUTHORIZED) ApiCheck.UNAUTHORIZED else ApiCheck.FAILED
    }

    private suspend fun <T> withTransientRetry(block: suspend () -> T): T {
        var delayMs = INITIAL_BACKOFF_MS
        repeat(MAX_TRANSIENT_RETRIES) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (e is NunchukApiException || e.matrixFailureCategory() != MatrixFailureCategory.TRANSIENT) throw e
                record("matrix recovery transient failure: ${e.javaClass.simpleName}")
            }
            delay(delayMs)
            delayMs *= 2
        }
        return block()
    }

    private fun isCurrent(gen: Int, chatId: String) =
        generation.get() == gen && accountManager.getAccount().chatId == chatId

    // Categories only — never tokens, passwords or credentials.
    private fun record(message: String) = Timber.tag(TAG).w(message)

    private companion object {
        const val TAG = "MatrixSessionRecovery"
        const val MAX_RECOVERY_ATTEMPTS = 2
        const val MAX_TRANSIENT_RETRIES = 3
        const val INITIAL_BACKOFF_MS = 2_000L
    }
}
