package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.DraftInheritanceCardKey
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.InheritanceValidationField
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.wallet.ReplaceWalletStatus
import com.nunchuk.android.type.SignerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The default 2-of-4 wallet of Figma Row 9 (REP-01): Keystone and COLDCARD on spending (Acct 0 / 1),
 * the Platform key, Jade as the separate inheritance key, and Keystone again on Fallback 1 (Acct 2).
 */
class ConfigurableReplaceCardsTest {

    private fun key(index: Int, account: Int?, section: InheritanceSection?) =
        DraftInheritanceCardKey(keyIndex = index, accountIndex = account, section = section)

    private fun card(section: InheritanceSection, vararg keys: DraftInheritanceCardKey, fallbackIndex: Int? = null, server: Boolean = false) =
        DraftInheritanceCard(
            section = section, fallbackIndex = fallbackIndex, isServerKey = server,
            tags = if (section == InheritanceSection.SPENDING) emptyList() else listOf("INHERITANCE"),
            keys = keys.toList(), accountIndexByXfp = emptyMap(),
        )

    private val cards = listOf(
        card(InheritanceSection.SPENDING, key(0, 0, InheritanceSection.SPENDING), key(1, 1, InheritanceSection.PRIMARY)),
        card(InheritanceSection.SPENDING, key(2, 0, InheritanceSection.SPENDING), key(3, 1, InheritanceSection.PRIMARY)),
        card(InheritanceSection.SPENDING, key(4, null, InheritanceSection.SPENDING), server = true),
        card(InheritanceSection.PRIMARY, key(6, 0, InheritanceSection.PRIMARY)),
        card(InheritanceSection.FALLBACK, key(8, 2, InheritanceSection.FALLBACK), fallbackIndex = 0),
    )

    private fun signer(index: Int, name: String, xfp: String, account: Int, vararg tags: String) = SignerServer(
        index = index, name = name, xfp = xfp, derivationPath = "m/48'/0'/$account'/2'",
        type = SignerType.AIRGAP, tags = tags.toList(),
    )

    private val walletSigners = listOf(
        signer(0, "Keystone", "79eb35f4", 0),
        signer(1, "Keystone", "79eb35f4", 1),
        signer(2, "COLDCARD", "a47c3e19", 0),
        signer(3, "COLDCARD", "a47c3e19", 1),
        signer(6, "Jade 2", "6b35f210", 0, "INHERITANCE", "SOMETHING_NEW"),
        signer(8, "Keystone", "79eb35f4", 2, "INHERITANCE"),
    )

    private val primaryTime = WalletTimelock(timelockValue = 2063999100L, timezone = "UTC", based = TimelockBased.TIME_LOCK)
    private val fallbackTime = primaryTime.copy(timelockValue = 2221765500L)
    private val policy = OnChainInheritancePolicy(
        type = OnChainInheritancePolicyType.CONFIGURABLE,
        primary = PrimaryInheritanceCondition(m = 2, n = 4, isCustomized = false, timelock = primaryTime),
        fallbacks = listOf(FallbackInheritanceCondition(index = 0, m = 1, n = 1, timelock = fallbackTime)),
    )

    private fun build(
        replacements: Map<String, List<SignerServer>> = emptyMap(),
        timelock: WalletTimelock? = primaryTime,
        fallbackTimelocks: List<WalletTimelock> = listOf(fallbackTime),
        errors: List<InheritanceValidationError> = emptyList(),
    ) = buildConfigurableReplaceState(
        policy = policy,
        walletSigners = walletSigners,
        walletTimelock = primaryTime,
        status = ReplaceWalletStatus(
            replacements = replacements,
            timelock = timelock,
            inheritanceCards = cards,
            validationErrors = errors,
            fallbackTimelocks = fallbackTimelocks,
        ),
    )

    @Test
    fun `cards keep the server order, one row per device, joined by key index`() {
        val state = build()
        assertEquals(5, state.cards.size)
        val (keystone, coldcard, server, primary, fallback) = state.cards
        assertEquals(listOf("79eb35f4"), keystone.rows.map { it.originalXfp })
        assertEquals(listOf(0, 1), keystone.rows.single().slots.map { it.accountIndex })
        assertEquals("COLDCARD", coldcard.rows.single().original?.name)
        assertTrue(server.isServerKey)
        assertTrue(server.rows.isEmpty())
        assertEquals("Jade 2", primary.rows.single().original?.name)
        assertEquals(listOf(2), fallback.rows.single().slots.map { it.accountIndex })
    }

    @Test
    fun `a key used in several cards is one replacement target over all its accounts`() {
        val target = build().targets.getValue("79eb35f4")
        assertEquals(listOf(0, 1, 2), target.slots.map { it.accountIndex })
        assertEquals(listOf(0, 1, 8), target.slots.map { it.keyIndex })
        assertFalse(target.isReplacing)
        assertEquals(0, target.nextMissingSlot?.keyIndex)
    }

    @Test
    fun `nothing changed - Continue stays disabled`() {
        val state = build()
        assertFalse(state.hasChanges)
        assertFalse(state.canContinue)
        assertFalse(state.primaryTimelock.isChanged)
    }

    @Test
    fun `missing account keeps the replacement incomplete (REP-02)`() {
        val state = build(
            replacements = mapOf(
                "79eb35f4" to listOf(
                    signer(0, "Passport", "8e41b2c7", 0),
                    signer(1, "Passport", "8e41b2c7", 1),
                )
            )
        )
        val target = state.targets.getValue("79eb35f4")
        assertTrue(target.isReplacing)
        assertFalse(target.isComplete)
        assertEquals(8, target.nextMissingSlot?.keyIndex)
        assertEquals(2, target.nextMissingSlot?.accountIndex)
        assertEquals("Passport", target.replacementDevice?.name)
        assertFalse(state.canContinue)
    }

    @Test
    fun `all accounts in but unverified - Verify, then Continue once verified (REP-03, REP-03b)`() {
        fun replacement(verify: VerifyType) = mapOf(
            "79eb35f4" to listOf(
                signer(0, "Passport", "8e41b2c7", 0).copy(verifyType = verify),
                signer(1, "Passport", "8e41b2c7", 1).copy(verifyType = verify),
                signer(8, "Passport", "8e41b2c7", 2).copy(verifyType = verify),
            )
        )
        val unverified = build(replacements = replacement(VerifyType.NONE))
        assertTrue(unverified.targets.getValue("79eb35f4").isComplete)
        assertFalse(unverified.targets.getValue("79eb35f4").isVerified)
        assertFalse(unverified.canContinue)

        val verified = build(replacements = replacement(VerifyType.APP_VERIFIED))
        assertTrue(verified.targets.getValue("79eb35f4").isVerified)
        assertTrue(verified.canContinue)
    }

    @Test
    fun `primary timelock change only - keys keep their verification (REP-05a)`() {
        val later = primaryTime.copy(timelockValue = primaryTime.timelockValue + 31_536_000L)
        val state = build(timelock = later)
        assertTrue(state.primaryTimelock.isChanged)
        assertEquals(later, state.primaryTimelock.effective)
        assertEquals(primaryTime, state.primaryTimelock.original)
        assertFalse(state.fallbackTimelocks.getValue(0).isChanged)
        assertTrue(state.canContinue)
    }

    @Test
    fun `a validation error blocks Continue and circles only its own section (REP-06)`() {
        val later = primaryTime.copy(timelockValue = fallbackTime.timelockValue + 1)
        val error = InheritanceValidationError(
            code = "TIMELOCK_ORDER_INVALID", message = "Choose a time after inheritance unlocks.",
            section = InheritanceSection.FALLBACK, fallbackIndex = 0, field = InheritanceValidationField.TIMELOCK,
            keyIndices = emptyList(),
        )
        val state = build(timelock = later, errors = listOf(error))
        assertFalse(state.canContinue)
        assertTrue(state.validationErrors.isTimelockHighlighted(InheritanceSection.FALLBACK, 0))
        assertFalse(state.validationErrors.isTimelockHighlighted(InheritanceSection.PRIMARY, null))
    }

    @Test
    fun `keys error circles the named key indices of that section only`() {
        val error = InheritanceValidationError(
            code = "SPENDS_BEFORE_TIMELOCK", message = "…", section = InheritanceSection.PRIMARY,
            fallbackIndex = null, field = InheritanceValidationField.KEYS, keyIndices = listOf(6),
        )
        val errors = listOf(error)
        assertEquals(setOf(6), errors.highlightedKeys(InheritanceSection.PRIMARY, null))
        assertTrue(errors.highlightedKeys(InheritanceSection.SPENDING, null).isEmpty())
        assertTrue(errors.highlightedKeys(InheritanceSection.FALLBACK, 0).isEmpty())
    }

    @Test
    fun `a retained unused account with no key is neither shown nor required`() {
        val withUnused = cards.toMutableList().apply {
            set(3, card(InheritanceSection.PRIMARY, key(6, 0, InheritanceSection.PRIMARY), key(7, 1, null)))
        }
        val state = buildConfigurableReplaceState(
            policy = policy,
            walletSigners = walletSigners,
            walletTimelock = primaryTime,
            status = ReplaceWalletStatus(timelock = primaryTime, inheritanceCards = withUnused),
        )
        assertEquals(listOf(6), state.cards[3].rows.flatMap { it.slots }.map { it.keyIndex })
        assertNull(state.targets["6b35f210"]?.slots?.firstOrNull { it.keyIndex == 7 })
    }

    @Test
    fun `unknown server tags are dropped, not fatal`() {
        val jade = build().cards[3].rows.single().original
        assertEquals("Jade 2", jade?.name)
        assertEquals(1, jade?.tags?.size)
    }
}
