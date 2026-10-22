package com.nunchuk.android.signer.tapsigner.backup.verify

import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.RecoveredKey
import com.nunchuk.android.model.signer.SignerServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TapSignerVerifyAccountTest {

    private fun draft(type: OnChainInheritancePolicyType?, signers: List<SignerServer> = listOf(tapSigner)) = DraftWallet(
        config = WalletConfig(m = 2, n = 4, requiredServerKey = true, allowInheritance = true),
        isMasterSecurityQuestionSet = false,
        signers = signers,
        walletType = null,
        onChainInheritancePolicy = type?.let { OnChainInheritancePolicy(type = it, primary = null) },
    )

    private val tapSigner = SignerServer(xfp = "aabbccdd", derivationPath = "m/48h/0h/1h/2h", index = 3)
    private val derived = SingleSigner(
        name = "TAPSIGNER", xpub = "xpub-account-1", derivationPath = "m/48h/0h/1h/2h", masterFingerprint = "aabbccdd",
    )

    private fun resolve(
        draft: Result<DraftWallet>,
        derive: (Int) -> Result<SingleSigner?> = { Result.success(derived) },
    ) = runBlocking { resolveTapSignerVerifyAccount(draft, "AABBCCDD") { derive(it) } }

    @Test
    fun `legacy draft verifies by fingerprint`() {
        assertEquals(TapSignerVerifyAccount.NotRequired, resolve(Result.success(draft(OnChainInheritancePolicyType.LEGACY))))
        assertEquals(TapSignerVerifyAccount.NotRequired, resolve(Result.success(draft(null))))
    }

    @Test
    fun `configurable draft sends the imported account derived from the master signer`() {
        var askedAccount = -1
        val result = resolve(Result.success(draft(OnChainInheritancePolicyType.CONFIGURABLE))) {
            askedAccount = it
            Result.success(derived)
        }
        assertEquals(1, askedAccount)
        assertEquals(
            TapSignerVerifyAccount.Resolved(RecoveredKey("aabbccdd", "m/48h/0h/1h/2h", "xpub-account-1")),
            result,
        )
    }

    @Test
    fun `uncached xpub asks for a tap instead of sending nothing`() {
        val result = resolve(Result.success(draft(OnChainInheritancePolicyType.CONFIGURABLE))) {
            Result.failure(NCNativeException("-1009:xpub not cached"))
        }
        assertEquals(TapSignerVerifyAccount.NeedsTap("m/48h/0h/1h/2h"), result)
    }

    @Test
    fun `unreadable draft or missing account fails - never a silent fingerprint-only verify`() {
        assertTrue(resolve(Result.failure(RuntimeException("offline"))) is TapSignerVerifyAccount.Failed)
        assertTrue(
            resolve(Result.success(draft(OnChainInheritancePolicyType.CONFIGURABLE, signers = emptyList())))
                is TapSignerVerifyAccount.Failed
        )
        assertTrue(
            resolve(Result.success(draft(OnChainInheritancePolicyType.CONFIGURABLE))) { Result.success(null) }
                is TapSignerVerifyAccount.Failed
        )
    }
}
