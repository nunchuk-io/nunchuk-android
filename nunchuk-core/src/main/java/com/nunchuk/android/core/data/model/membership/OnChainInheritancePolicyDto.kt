package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.DraftInheritanceCardKey
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.InheritanceValidationField
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition

// On-chain inheritance API shapes from NUN-10320, shared by draft wallets, wallets and inheritance plans.

data class OnChainInheritancePolicyDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("primary") val primary: PrimaryInheritanceConditionDto? = null,
    @SerializedName("fallbacks") val fallbacks: List<FallbackInheritanceConditionDto>? = null,
)

data class PrimaryInheritanceConditionDto(
    @SerializedName("m") val m: Int = 0,
    @SerializedName("n") val n: Int = 0,
    @SerializedName("is_customized") val isCustomized: Boolean = false,
    @SerializedName("timelock") val timelock: TimelockDto? = null,
)

data class FallbackInheritanceConditionDto(
    @SerializedName("index") val index: Int = 0,
    @SerializedName("m") val m: Int = 0,
    @SerializedName("n") val n: Int = 0,
    @SerializedName("timelock") val timelock: TimelockDto? = null,
)

data class DraftInheritanceCardDto(
    @SerializedName("section") val section: String? = null,
    @SerializedName("fallback_index") val fallbackIndex: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("keys") val keys: List<DraftInheritanceCardKeyDto>? = null,
    @SerializedName("account_index_by_xfp") val accountIndexByXfp: Map<String, Int?>? = null,
)

data class DraftInheritanceCardKeyDto(
    @SerializedName("key_index") val keyIndex: Int = 0,
    @SerializedName("account_index") val accountIndex: Int? = null,
    @SerializedName("section") val section: String? = null,
)

data class InheritanceValidationErrorDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("section") val section: String? = null,
    @SerializedName("fallback_index") val fallbackIndex: Int? = null,
    @SerializedName("field") val field: String? = null,
    @SerializedName("key_indices") val keyIndices: List<Int>? = null,
)

/** Null for an unknown policy type, so a newer server value never renders as the wrong flow. */
internal fun OnChainInheritancePolicyDto.toModel(): OnChainInheritancePolicy? {
    val type = runCatching { OnChainInheritancePolicyType.valueOf(type.orEmpty()) }.getOrNull() ?: return null
    return OnChainInheritancePolicy(
        type = type,
        primary = primary?.let {
            PrimaryInheritanceCondition(
                m = it.m,
                n = it.n,
                isCustomized = it.isCustomized,
                timelock = it.timelock?.toWalletTimelock(),
            )
        },
        fallbacks = fallbacks.orEmpty().map {
            FallbackInheritanceCondition(
                index = it.index,
                m = it.m,
                n = it.n,
                timelock = it.timelock?.toWalletTimelock(),
            )
        },
    )
}

internal fun String?.toInheritanceSection(): InheritanceSection? =
    runCatching { InheritanceSection.valueOf(this.orEmpty()) }.getOrNull()

/** Cards with an unknown section are dropped rather than placed in the wrong part of the screen. */
internal fun DraftInheritanceCardDto.toModel(): DraftInheritanceCard? {
    val section = section.toInheritanceSection() ?: return null
    return DraftInheritanceCard(
        section = section,
        fallbackIndex = fallbackIndex,
        isServerKey = type == SERVER_KEY_CARD_TYPE,
        tags = tags.orEmpty(),
        keys = keys.orEmpty().map {
            DraftInheritanceCardKey(
                keyIndex = it.keyIndex,
                accountIndex = it.accountIndex,
                section = it.section.toInheritanceSection(),
            )
        },
        accountIndexByXfp = accountIndexByXfp.orEmpty(),
    )
}

internal fun InheritanceValidationErrorDto.toModel() = InheritanceValidationError(
    code = code.orEmpty(),
    message = message.orEmpty(),
    section = section.toInheritanceSection(),
    fallbackIndex = fallbackIndex,
    field = runCatching { InheritanceValidationField.valueOf(field.orEmpty().uppercase()) }.getOrNull(),
    keyIndices = keyIndices.orEmpty(),
)

private const val SERVER_KEY_CARD_TYPE = "SERVER_KEY"
