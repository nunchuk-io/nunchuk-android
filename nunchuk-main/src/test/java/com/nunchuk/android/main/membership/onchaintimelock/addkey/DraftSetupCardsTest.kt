package com.nunchuk.android.main.membership.onchaintimelock.addkey

import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.DraftInheritanceCardKey
import com.nunchuk.android.model.inheritance.DraftKeySlot
import com.nunchuk.android.model.inheritance.DraftKeySlotHolder
import com.nunchuk.android.model.inheritance.DraftKeySlotStore
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.InheritanceValidationField
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.SignerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The default HB draft from the NUN-10320 integration doc: Owner A and the inheritance key imported. */
class DraftSetupCardsTest {

    private fun key(index: Int, account: Int?, section: InheritanceSection?) =
        DraftInheritanceCardKey(keyIndex = index, accountIndex = account, section = section)

    private val spendingA = DraftInheritanceCard(
        section = InheritanceSection.SPENDING, fallbackIndex = null, isServerKey = false, tags = emptyList(),
        keys = listOf(key(0, 0, InheritanceSection.PRIMARY), key(1, 1, InheritanceSection.SPENDING)),
        accountIndexByXfp = emptyMap(),
    )
    private val spendingB = spendingA.copy(
        keys = listOf(key(2, 0, InheritanceSection.PRIMARY), key(3, 1, InheritanceSection.SPENDING)),
    )
    private val server = spendingA.copy(isServerKey = true, keys = listOf(key(6, null, InheritanceSection.SPENDING)))
    private val primary = DraftInheritanceCard(
        section = InheritanceSection.PRIMARY, fallbackIndex = null, isServerKey = false, tags = listOf("INHERITANCE"),
        keys = listOf(key(7, 0, InheritanceSection.PRIMARY)),
        accountIndexByXfp = mapOf("1111aaaa" to 0, "6b35f210" to 0, "9999ffff" to null),
    )

    private val signers = listOf(
        SignerServer(index = 0, name = "Owner A", xfp = "1111aaaa", derivationPath = "m/48'/0'/0'/2'", type = SignerType.AIRGAP, verifyType = VerifyType.APP_VERIFIED),
        SignerServer(index = 1, name = "Owner A", xfp = "1111aaaa", derivationPath = "m/48'/0'/1'/2'", type = SignerType.AIRGAP, verifyType = VerifyType.APP_VERIFIED),
        SignerServer(index = 7, name = "Jade 2", xfp = "6b35f210", derivationPath = "m/48'/0'/0'/2'", type = SignerType.AIRGAP, tags = listOf("INHERITANCE", "SOMETHING_NEW")),
    )

    private val cards = buildDraftSetupCards(listOf(spendingA, spendingB, server, primary), signers)

    @Test
    fun `cards keep the server order and join signers by key index`() {
        assertEquals(4, cards.size)
        val (a, b, _, p) = cards
        assertTrue(a.isComplete)
        assertEquals(listOf(0, 1), a.slots.map { it.signer?.index })
        assertEquals("Owner A", a.device?.name)
        assertFalse(b.isComplete)
        assertEquals(2, b.nextEmptySlot?.keyIndex)
        assertEquals("Jade 2", p.slots.single().signer?.name)
        assertFalse(p.slots.single().isVerified)
    }

    @Test
    fun `unknown tags never break the screen`() {
        assertEquals(1, cards[3].slots.single().signer?.tags?.size)
    }

    @Test
    fun `a retained unused account that is not imported is not a slot`() {
        val retained = spendingA.copy(keys = listOf(key(0, 0, null), key(1, 1, InheritanceSection.SPENDING)))
        val built = buildDraftSetupCards(listOf(retained), emptyList()).single()

        assertEquals(listOf(1), built.slots.map { it.keyIndex })
    }

    @Test
    fun `a spending card asks for every empty account, a primary slot for its own`() {
        assertEquals(
            DraftKeySlot("", 2, listOf(2, 3), emptyList(), accountIndex = 0),
            cards[1].slotToAdd("", cards[1].slots[0]),
        )
        val primarySlot = cards[3].slotToAdd("", cards[3].slots.single())
        assertEquals(listOf(7), primarySlot.keyIndices)
        assertTrue(primarySlot.isInheritance)
    }

    @Test
    fun `expected account - request path, then slot account, then the card's xfp map`() {
        val primarySlot = cards[3].slotToAdd("", cards[3].slots.single()).copy(accountIndex = null)
        assertEquals(0, primarySlot.expectedAccount("1111AAAA"))
        assertEquals(0, primarySlot.expectedAccount("cafebabe"))
        assertNull(primarySlot.expectedAccount("9999ffff"))
        assertEquals(2, primarySlot.expectedAccount("9999ffff", requestPath = "m/48'/0'/2'/2'"))
        assertEquals(1, primarySlot.copy(accountIndex = 1).expectedAccount("1111aaaa"))
        assertEquals(2, primarySlot.copy(accountIndexByXfp = mapOf("1111aaaa" to 2)).expectedAccount("1111aaaa"))
    }

    @Test
    fun `account index is read from either path notation`() {
        assertEquals(2, bip32AccountIndex("m/48'/0'/2'/2'"))
        assertEquals(1, bip32AccountIndex("m/48h/1h/1h/2h"))
        assertNull(bip32AccountIndex("m/48h"))
    }

    @Test
    fun `key errors circle keys of their own section only`() {
        val errors = listOf(
            InheritanceValidationError("SPENDS_BEFORE_TIMELOCK", "m", InheritanceSection.PRIMARY, null, InheritanceValidationField.KEYS, listOf(7, 8)),
            InheritanceValidationError("X", "m", InheritanceSection.FALLBACK, 0, InheritanceValidationField.KEYS, listOf(9)),
            InheritanceValidationError("TIMELOCK_ORDER_INVALID", "m", InheritanceSection.FALLBACK, 0, InheritanceValidationField.TIMELOCK, emptyList()),
        )

        assertEquals(setOf(7, 8), errors.highlightedKeyIndices(InheritanceSection.PRIMARY))
        assertEquals(emptySet<Int>(), errors.highlightedKeyIndices(InheritanceSection.SPENDING))
        assertFalse(errors.isTimelockHighlighted(InheritanceSection.PRIMARY))
        assertTrue(errors.isTimelockHighlighted(InheritanceSection.FALLBACK))
    }

    @Test
    fun `fallback errors circle only their own fallback`() {
        val errors = listOf(
            InheritanceValidationError("X", "m", InheritanceSection.FALLBACK, 0, InheritanceValidationField.KEYS, listOf(8)),
            InheritanceValidationError("X", "m", InheritanceSection.FALLBACK, 1, InheritanceValidationField.KEYS, listOf(9)),
            InheritanceValidationError("TIMELOCK_ORDER_INVALID", "m", InheritanceSection.FALLBACK, 1, InheritanceValidationField.TIMELOCK, emptyList()),
        )

        assertEquals(setOf(8), errors.highlightedKeyIndices(InheritanceSection.FALLBACK, 0))
        assertEquals(setOf(9), errors.highlightedKeyIndices(InheritanceSection.FALLBACK, 1))
        assertFalse(errors.isTimelockHighlighted(InheritanceSection.FALLBACK, 0))
        assertTrue(errors.isTimelockHighlighted(InheritanceSection.FALLBACK, 1))
    }

    private class MemoryStore : DraftKeySlotStore {
        var saved: DraftKeySlot? = null
        override fun load(): DraftKeySlot? = saved
        override fun save(slot: DraftKeySlot?) {
            saved = slot
        }
    }

    @Test
    fun `the slot holder only answers for its own draft`() {
        val holder = DraftKeySlotHolder(MemoryStore())
        holder.set(DraftKeySlot(groupId = "", keyIndex = 7))

        assertEquals(7, holder.slotFor("")?.keyIndex)
        assertNull(holder.slotFor("group-1"))
        holder.clear()
        assertNull(holder.slotFor(""))
    }

    @Test
    fun `the slot survives the process - a new holder reads it back until cleared`() {
        val store = MemoryStore()
        DraftKeySlotHolder(store).set(DraftKeySlot(groupId = "", keyIndex = 8, tags = listOf("INHERITANCE"), accountIndex = 2))

        val restored = DraftKeySlotHolder(store)
        assertEquals(DraftKeySlot(groupId = "", keyIndex = 8, tags = listOf("INHERITANCE"), accountIndex = 2), restored.slotFor(""))
        restored.clear()
        assertNull(DraftKeySlotHolder(store).slotFor(""))
    }

    // KEY-10: Fallback 1 holds BitBox (xfp 2a9f3d10) at Account 0; Fallback 2 holds it at Account 3.
    private val fallback1 = primary.copy(
        section = InheritanceSection.FALLBACK, fallbackIndex = 0, keys = listOf(key(8, null, InheritanceSection.FALLBACK)),
    )
    private val fallback2 = fallback1.copy(fallbackIndex = 1, keys = listOf(key(9, null, InheritanceSection.FALLBACK)))
    private val conflictCards = buildDraftSetupCards(
        listOf(spendingA, spendingB, primary, fallback1, fallback2),
        signers.take(2) + listOf(
            SignerServer(index = 8, name = "BitBox", xfp = "2A9F3D10", derivationPath = "m/48'/0'/0'/2'", type = SignerType.HARDWARE),
            SignerServer(index = 9, name = "BitBox", xfp = "2A9F3D10", derivationPath = "m/48'/0'/3'/2'", type = SignerType.HARDWARE),
        ),
    )

    @Test
    fun `a spending card taking Account 0 takes the key from the fallback holding Account 0`() {
        val emptySpending = conflictCards[1]
        assertEquals(setOf(0), conflictCards.fallbacksLosingKey(emptySpending, "2a9f3d10", account = 0))
    }

    @Test
    fun `a primary slot at Account 0 takes the key from that fallback too`() {
        val primaryCard = conflictCards[2]
        assertEquals(setOf(0), conflictCards.fallbacksLosingKey(primaryCard, "2a9f3d10", account = 0))
    }

    @Test
    fun `no conflict for another key, a non-zero account, or a fallback import`() {
        val primaryCard = conflictCards[2]
        assertTrue(conflictCards.fallbacksLosingKey(primaryCard, "1111aaaa", account = 0).isEmpty())
        assertTrue(conflictCards.fallbacksLosingKey(primaryCard, "2a9f3d10", account = 2).isEmpty())
        assertTrue(conflictCards.fallbacksLosingKey(conflictCards[4], "2a9f3d10", account = 0).isEmpty())
    }

    @Test
    fun `a spending card whose Account 0 is already in leaves the fallback alone`() {
        val onlyAccount1Empty = conflictCards[1].let { card ->
            card.copy(slots = card.slots.map { if (it.accountIndex == 0) it.copy(signer = conflictCards[0].device) else it })
        }
        assertTrue(conflictCards.fallbacksLosingKey(onlyAccount1Empty, "2a9f3d10", account = 1).isEmpty())
    }
}
