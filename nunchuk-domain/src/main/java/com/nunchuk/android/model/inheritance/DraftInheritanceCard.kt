package com.nunchuk.android.model.inheritance

/**
 * One card on "Let's configure your wallet" for an on-chain draft (`draft_wallet.cards`, NUN-10320).
 * Keep the server's order.
 *
 * @property fallbackIndex FALLBACK cards only — joins [FallbackInheritanceCondition.index].
 * @property isServerKey the Platform key card (`type: SERVER_KEY`), which has a Configure action.
 * @property tags key-import tags, e.g. INHERITANCE.
 * @property accountIndexByXfp primary/fallback cards: which account to import for an XFP the user
 *   picks. An XFP missing from the map uses account 0; a null value is unresolved — do not guess.
 */
data class DraftInheritanceCard(
    val section: InheritanceSection,
    val fallbackIndex: Int?,
    val isServerKey: Boolean,
    val tags: List<String>,
    val keys: List<DraftInheritanceCardKey>,
    val accountIndexByXfp: Map<String, Int?>,
)

/**
 * A backend-assigned slot. Join [keyIndex] to the draft signer's `key_index`; no signer means the
 * key still has to be imported.
 *
 * @property accountIndex account to import; null when it depends on the XFP the user selects.
 * @property section the condition this account serves; null for a retained, unused account.
 */
data class DraftInheritanceCardKey(
    val keyIndex: Int,
    val accountIndex: Int?,
    val section: InheritanceSection?,
)

/**
 * A setup issue to show inline (`validation_errors`). Show [message] once; circle the keys in
 * [keyIndices] when [field] is KEYS, or the condition's timelock when it is TIMELOCK. Never highlight
 * spending cards for an inheritance-condition issue.
 */
data class InheritanceValidationError(
    val code: String,
    val message: String,
    val section: InheritanceSection?,
    val fallbackIndex: Int?,
    val field: InheritanceValidationField?,
    val keyIndices: List<Int>,
)

enum class InheritanceValidationField {
    KEYS,
    TIMELOCK,
}
