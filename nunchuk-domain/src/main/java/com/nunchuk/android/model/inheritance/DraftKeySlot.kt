package com.nunchuk.android.model.inheritance

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the key being added to a configurable on-chain draft goes (NUN-10320 `cards`).
 *
 * @property keyIndex the slot the next imported account fills (`add-key`).
 * @property keyIndices every still-empty slot of the card, for a desktop request (`request-add-key`).
 * @property tags the card's import tags, e.g. INHERITANCE.
 * @property accountIndex the slot's `account_index`; null when it depends on the key picked.
 * @property accountIndexByXfp the card's `account_index_by_xfp`, used when [accountIndex] is null.
 */
data class DraftKeySlot(
    val groupId: String,
    val keyIndex: Int,
    val keyIndices: List<Int> = listOf(keyIndex),
    val tags: List<String> = emptyList(),
    val accountIndex: Int? = null,
    val accountIndexByXfp: Map<String, Int?> = emptyMap(),
) {
    val isInheritance: Boolean
        get() = tags.contains(INHERITANCE_TAG)

    /**
     * The account [xfp] has to be imported from: the request's own path wins (`request.derivation_path`),
     * then the slot's account, then the card's XFP map — absent means account 0, a null entry is
     * unresolved (null here) and must not be guessed.
     */
    fun expectedAccount(xfp: String, requestPath: String? = null): Int? {
        requestPath?.let(::bip32AccountIndex)?.let { return it }
        accountIndex?.let { return it }
        val key = accountIndexByXfp.keys.firstOrNull { it.equals(xfp, ignoreCase = true) } ?: return 0
        return accountIndexByXfp[key]
    }

    companion object {
        const val INHERITANCE_TAG = "INHERITANCE"
    }
}

/** Account of a BIP32 path, e.g. 2 for `m/48'/0'/2'/2'` or `m/48h/1h/2h/2h`; null when it has none. */
fun bip32AccountIndex(path: String): Int? =
    path.split('/').getOrNull(3)?.trimEnd('\'', 'h', 'H')?.toIntOrNull()

/** The imported account is not the one the slot needs (e.g. account 0 where the plan uses account 2). */
class DraftKeyAccountMismatchException(val expectedAccount: Int?) :
    IllegalStateException(
        if (expectedAccount == null) "This key can't be used here yet. Choose another key or add a new one."
        else "This key must be added from Account $expectedAccount. Add Account $expectedAccount to continue."
    )

/** A configurable draft's import arrived with no slot (e.g. the slot was cleared meanwhile): refuse it. */
class DraftKeySlotMissingException :
    IllegalStateException("This key could not be placed. Tap Add on the key's card again.")

/** A configurable draft verifies a backup against the recovered account; none was read. */
class RecoveredKeyRequiredException :
    IllegalStateException("The account of this key could not be read from the device. Try again.")

/** Keeps the picked slot across process death: device flows can outlive the app process. */
interface DraftKeySlotStore {
    fun load(): DraftKeySlot?
    fun save(slot: DraftKeySlot?)
}

/**
 * The slot picked on the on-chain setup screen, for the device flows that save the key themselves
 * (COLDCARD, air-gap, software, desktop). They only know a `MembershipStep`, whose fixed index does
 * not exist on a configurable draft, so the repository takes the slot from here instead.
 *
 * Persisted through [DraftKeySlotStore], so an import that resumes after Android killed the app still
 * lands in its slot. Set by the setup screen right before it starts an add; cleared after the import
 * or when that screen starts another add, so a stale slot never reaches another draft.
 */
@Singleton
class DraftKeySlotHolder @Inject constructor(private val store: DraftKeySlotStore) {
    private val slot = AtomicReference<DraftKeySlot?>(null)
    @Volatile private var isLoaded = false

    fun set(value: DraftKeySlot?) {
        slot.set(value)
        isLoaded = true
        store.save(value)
    }

    fun clear() = set(null)

    /** The slot for [groupId]'s draft, or null to use the step's own index. */
    fun slotFor(groupId: String): DraftKeySlot? {
        if (!isLoaded) {
            slot.compareAndSet(null, store.load())
            isLoaded = true
        }
        return slot.get()?.takeIf { it.groupId == groupId }
    }
}
