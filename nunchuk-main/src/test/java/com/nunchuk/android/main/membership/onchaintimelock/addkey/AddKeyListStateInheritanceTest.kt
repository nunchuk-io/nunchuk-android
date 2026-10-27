package com.nunchuk.android.main.membership.onchaintimelock.addkey

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.InheritanceValidationField
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddKeyListStateInheritanceTest {

    private fun policy(
        isCustomized: Boolean,
        type: OnChainInheritancePolicyType = OnChainInheritancePolicyType.CONFIGURABLE,
    ) = OnChainInheritancePolicy(
        type = type,
        primary = PrimaryInheritanceCondition(
            m = if (isCustomized) 3 else 2,
            n = 4,
            isCustomized = isCustomized,
            timelock = null,
        ),
    )

    private fun error(section: InheritanceSection?, message: String = "issue") = InheritanceValidationError(
        code = "CODE",
        message = message,
        section = section,
        fallbackIndex = if (section == InheritanceSection.FALLBACK) 0 else null,
        field = InheritanceValidationField.KEYS,
        keyIndices = listOf(0, 1),
    )

    private fun signer(xfp: String) = SignerModel(
        id = xfp, name = xfp, derivationPath = "", fingerPrint = xfp, isMasterSigner = false,
    )

    private fun card(section: InheritanceSection, vararg slots: DraftSetupSlot, fallbackIndex: Int? = null) = DraftSetupCard(
        card = DraftInheritanceCard(
            section = section,
            fallbackIndex = fallbackIndex,
            isServerKey = false,
            tags = emptyList(),
            keys = emptyList(),
            accountIndexByXfp = emptyMap(),
        ),
        slots = slots.toList(),
    )

    private fun slot(index: Int, xfp: String?, verified: Boolean, section: InheritanceSection = InheritanceSection.PRIMARY) =
        DraftSetupSlot(
            keyIndex = index,
            accountIndex = 0,
            section = section,
            signer = xfp?.let(::signer),
            verifyType = if (verified) VerifyType.APP_VERIFIED else VerifyType.NONE,
        )

    /** Default HB: owners A, B (accounts 0 + 1 each) and the separate inheritance key J. */
    private fun defaultState(
        inheritanceVerified: Boolean,
        ownerAVerified: Boolean,
        finalized: Boolean = true,
        errors: List<InheritanceValidationError> = emptyList(),
    ) = AddKeyListState(
        inheritancePolicy = policy(isCustomized = false),
        isDraftFinalized = finalized,
        validationErrors = errors,
        draftCards = listOf(
            card(
                InheritanceSection.SPENDING,
                slot(0, "a", ownerAVerified),
                slot(1, "a", ownerAVerified, InheritanceSection.SPENDING),
            ),
            card(
                InheritanceSection.SPENDING,
                slot(2, "b", false),
                slot(3, "b", false, InheritanceSection.SPENDING),
            ),
            card(InheritanceSection.PRIMARY, slot(7, "j", inheritanceVerified)),
        ),
    )

    @Test
    fun `draft without a policy keeps the existing setup`() {
        val state = AddKeyListState(validationErrors = listOf(error(InheritanceSection.PRIMARY)))

        assertFalse(state.isConfigurableInheritance)
        assertFalse(state.isPrimaryCustomized)
    }

    @Test
    fun `legacy policy is not configurable even when marked customized`() {
        val state = AddKeyListState(
            inheritancePolicy = policy(isCustomized = true, type = OnChainInheritancePolicyType.LEGACY),
        )

        assertFalse(state.isConfigurableInheritance)
        assertFalse(state.isPrimaryCustomized)
    }

    @Test
    fun `customized primary is reported`() {
        assertTrue(AddKeyListState(inheritancePolicy = policy(isCustomized = true)).isPrimaryCustomized)
    }

    @Test
    fun `default needs the inheritance key and one owner key verified`() {
        assertFalse(defaultState(inheritanceVerified = false, ownerAVerified = true).isDraftReadyToContinue)
        assertFalse(defaultState(inheritanceVerified = true, ownerAVerified = false).isDraftReadyToContinue)
        assertTrue(defaultState(inheritanceVerified = true, ownerAVerified = true).isDraftReadyToContinue)
    }

    @Test
    fun `completed setup still waits for backend readiness`() {
        assertFalse(
            defaultState(inheritanceVerified = true, ownerAVerified = true, finalized = false).isDraftReadyToContinue
        )
    }

    @Test
    fun `any validation error blocks continue`() {
        listOf(InheritanceSection.PRIMARY, InheritanceSection.FALLBACK, InheritanceSection.SPENDING, null)
            .forEach { section ->
                val state = defaultState(
                    inheritanceVerified = true,
                    ownerAVerified = true,
                    errors = listOf(error(section)),
                )
                assertFalse("section $section", state.isDraftReadyToContinue)
            }
    }

    @Test
    fun `owner keys list one entry per device, verified when any account is`() {
        val owners = defaultState(inheritanceVerified = false, ownerAVerified = true).ownerKeys

        assertEquals(listOf("a", "b"), owners.map { it.signer?.fingerPrint })
        assertEquals(listOf(true, false), owners.map { it.isVerified })
    }

    @Test
    fun `primary errors sit under the primary card and the rest are never dropped`() {
        val primary = error(InheritanceSection.PRIMARY, "primary")
        val fallback = error(InheritanceSection.FALLBACK, "fallback")
        val unknown = error(null, "unknown")
        val state = AddKeyListState(
            inheritancePolicy = policy(isCustomized = true),
            validationErrors = listOf(fallback, primary, unknown),
        )

        assertEquals(listOf(primary), state.primaryValidationErrors)
        assertEquals(listOf(fallback, unknown), state.otherValidationErrors)
    }

    @Test
    fun `inheritance guide runs once per setup`() {
        val empty = defaultState(inheritanceVerified = false, ownerAVerified = false).let { state ->
            state.copy(draftCards = state.draftCards.map { card ->
                if (card.section == InheritanceSection.PRIMARY) card.copy(slots = card.slots.map { it.copy(signer = null) }) else card
            })
        }
        assertFalse(empty.isInheritanceGuideSeen)
        assertTrue(empty.copy(isInheritanceGuideSeenInSession = true).isInheritanceGuideSeen)
        // A primary key already in means its guide ran.
        assertTrue(defaultState(inheritanceVerified = false, ownerAVerified = false).isInheritanceGuideSeen)
    }

    @Test
    fun `verification stays pending until the owner key is verified too`() {
        assertFalse(defaultState(inheritanceVerified = true, ownerAVerified = false).isRequiredVerificationDone)
        assertTrue(defaultState(inheritanceVerified = true, ownerAVerified = true).isRequiredVerificationDone)
    }

    @Test
    fun `default keeps Verify while any key on Verify backups is unverified`() {
        // Owner B is never verified here: Continue is allowed, but Verify stays on the inheritance key.
        val state = defaultState(inheritanceVerified = true, ownerAVerified = true)
        assertTrue(state.isRequiredVerificationDone)
        assertTrue(state.hasUnverifiedBackup)
        assertTrue(defaultState(inheritanceVerified = false, ownerAVerified = true).hasUnverifiedBackup)
    }

    /** Default HB, fully verified, plus fallbacks given as (index, slots). */
    private fun withFallbacks(vararg fallbacks: Pair<Int, List<DraftSetupSlot>>): AddKeyListState {
        val base = defaultState(inheritanceVerified = true, ownerAVerified = true)
        return base.copy(
            inheritancePolicy = base.inheritancePolicy!!.copy(
                fallbacks = fallbacks.map { (index, slots) ->
                    FallbackInheritanceCondition(index = index, m = 1, n = slots.size, timelock = null)
                }.reversed(),
            ),
            draftCards = base.draftCards + fallbacks.map { (index, slots) ->
                card(InheritanceSection.FALLBACK, *slots.toTypedArray(), fallbackIndex = index)
            },
        )
    }

    private fun fallbackSlot(index: Int, xfp: String?, verified: Boolean) =
        slot(index, xfp, verified, InheritanceSection.FALLBACK)

    @Test
    fun `fallbacks come in order with their own cards`() {
        val state = withFallbacks(
            0 to listOf(fallbackSlot(8, "j", true)),
            1 to listOf(fallbackSlot(9, "c", false), fallbackSlot(10, null, false)),
        )

        assertEquals(listOf(0, 1), state.fallbacks.map { it.index })
        assertEquals(listOf(8), state.fallbackSlots(0).map { it.keyIndex })
        assertEquals(listOf(9, 10), state.fallbackSlots(1).map { it.keyIndex })
    }

    @Test
    fun `a legacy draft has no fallbacks`() {
        val state = withFallbacks(0 to listOf(fallbackSlot(8, "j", true)))
            .let { it.copy(inheritancePolicy = it.inheritancePolicy!!.copy(type = OnChainInheritancePolicyType.LEGACY)) }

        assertTrue(state.fallbacks.isEmpty())
    }

    @Test
    fun `an added fallback key must be verified before continue`() {
        assertFalse(withFallbacks(0 to listOf(fallbackSlot(8, "j", false))).isRequiredVerificationDone)
        assertTrue(withFallbacks(0 to listOf(fallbackSlot(8, "j", true))).isRequiredVerificationDone)
        // An empty slot is left to the backend's `finalized`.
        assertTrue(withFallbacks(0 to listOf(fallbackSlot(8, null, false))).isRequiredVerificationDone)
    }

    @Test
    fun `fallback errors sit under their fallback, others stay at the end`() {
        val own = error(InheritanceSection.FALLBACK, "fallback 1")
        val gone = error(InheritanceSection.FALLBACK, "fallback 2").copy(fallbackIndex = 1)
        val state = withFallbacks(0 to listOf(fallbackSlot(8, "j", true))).copy(validationErrors = listOf(own, gone))

        assertEquals(listOf(own), state.fallbackValidationErrors(0))
        assertEquals(listOf(gone), state.otherValidationErrors)
    }

    @Test
    fun `fallback guide runs once per fallback`() {
        val state = withFallbacks(
            0 to listOf(fallbackSlot(8, null, false)),
            1 to listOf(fallbackSlot(9, "c", false)),
        )
        val first = state.fallbackCards(0).single().card
        val second = state.fallbackCards(1).single().card

        // The primary guide having run does not cover a fallback.
        assertTrue(state.isGuideSeen(state.primaryCards.single().card))
        assertFalse(state.isGuideSeen(first))
        assertTrue(state.copy(fallbackGuidesSeenInSession = setOf(0)).isGuideSeen(first))
        // A key already in the fallback means its guide ran.
        assertTrue(state.isGuideSeen(second))
    }

    @Test
    fun `fallback number is one based and zero for other cards`() {
        val state = withFallbacks(0 to listOf(fallbackSlot(8, null, false)), 2 to listOf(fallbackSlot(9, null, false)))

        assertEquals(1, state.fallbackCards(0).single().card.fallbackNumber)
        assertEquals(3, state.fallbackCards(2).single().card.fallbackNumber)
        assertEquals(0, state.primaryCards.single().card.fallbackNumber)
    }
}
