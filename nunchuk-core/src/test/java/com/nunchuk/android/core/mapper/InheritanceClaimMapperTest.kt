package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.membership.BufferPeriodCountdownResponse
import com.nunchuk.android.core.data.model.membership.ClaimValidKeyDto
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingDownloadWalletResponse
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingInitResponse
import com.nunchuk.android.core.data.model.membership.KeyOriginDto
import com.nunchuk.android.core.network.ApiErrorCode
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.model.inheritance.ClaimDownloadWalletResult
import com.nunchuk.android.model.inheritance.ClaimValidKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InheritanceClaimMapperTest {

    @Test
    fun `required key counts are distinct, positive and ascending`() {
        val init = InheritanceClaimingInitResponse(
            walletType = "MINISCRIPT",
            inheritanceKeyCount = 1,
            requiredKeyCounts = listOf(2, 1, 2, 0),
        ).toInheritanceClaimingInit()
        assertEquals(listOf(1, 2), init.requiredKeyCounts)
    }

    @Test
    fun `missing required key counts stay empty so the single count applies`() {
        val init = InheritanceClaimingInitResponse(walletType = "MINISCRIPT", inheritanceKeyCount = 2)
            .toInheritanceClaimingInit()
        assertTrue(init.requiredKeyCounts.isEmpty())
        assertEquals(2, init.inheritanceKeyCount)
    }

    @Test
    fun `no wallet with valid keys and origins asks for more accounts`() {
        val result = InheritanceClaimingDownloadWalletResponse(
            validKeys = listOf(ClaimValidKeyDto("1111aaaa", "m/48'/0'/0'/2'", "xpubA0")),
            keyOrigins = listOf(KeyOriginDto("1111aaaa", "m/48'/0'/2'/2'")),
        ).toClaimDownloadWalletResult(wallet = null)

        assertEquals(
            ClaimDownloadWalletResult.MoreKeysNeeded(
                validKeys = listOf(ClaimValidKey("1111aaaa", "m/48'/0'/0'/2'", "xpubA0")),
                keyOrigins = listOf(KeyOrigin("1111aaaa", "m/48'/0'/2'/2'")),
            ),
            result,
        )
    }

    @Test
    fun `empty lists mean another key is needed`() {
        val result = InheritanceClaimingDownloadWalletResponse(validKeys = emptyList(), keyOrigins = emptyList())
            .toClaimDownloadWalletResult(wallet = null)
        assertEquals(ClaimDownloadWalletResult.MoreKeysNeeded(emptyList(), emptyList()), result)
    }

    @Test
    fun `buffer period wins over more keys`() {
        val result = InheritanceClaimingDownloadWalletResponse(
            bufferPeriodCountdown = BufferPeriodCountdownResponse(remainingCount = 3),
            validKeys = emptyList(),
        ).toClaimDownloadWalletResult(wallet = null)
        assertTrue(result is ClaimDownloadWalletResult.BufferPeriod)
        assertEquals(3, (result as ClaimDownloadWalletResult.BufferPeriod).countdown.remainingCount)
    }

    @Test
    fun `legacy response without wallet still means no plan`() {
        try {
            InheritanceClaimingDownloadWalletResponse().toClaimDownloadWalletResult(wallet = null)
            fail("expected NunchukApiException")
        } catch (e: NunchukApiException) {
            assertEquals(ApiErrorCode.INHERITANCE_PLAN_NOT_FOUND, e.code)
        }
    }
}
