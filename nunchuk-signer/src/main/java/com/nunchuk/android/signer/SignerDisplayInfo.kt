package com.nunchuk.android.signer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.KeyFlow.isPrimaryKeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

enum class SignerDisplayCategory {
    CARD,
    ROW,
    ROW_SIMPLE,
}

data class SignerDisplayInfo(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int = 0,
    val keyType: KeyType,
    val category: SignerDisplayCategory,
    val isDisabled: Boolean = false,
)

fun SupportedSigner.toKeyType(): KeyType? = when (type) {
    SignerType.NFC -> KeyType.TAPSIGNER
    SignerType.COLDCARD_NFC -> KeyType.COLDCARD
    SignerType.PORTAL_NFC -> KeyType.PORTAL
    SignerType.SOFTWARE -> KeyType.SOFTWARE
    SignerType.SERVER -> KeyType.PLATFORM_KEY
    SignerType.AIRGAP -> when (tag) {
        SignerTag.JADE -> KeyType.JADE
        SignerTag.SEEDSIGNER -> KeyType.SEEDSIGNER
        SignerTag.KEYSTONE -> KeyType.KEYSTONE
        SignerTag.PASSPORT -> KeyType.FOUNDATION
        else -> null
    }
    SignerType.HARDWARE -> when (tag) {
        SignerTag.LEDGER -> KeyType.LEDGER
        SignerTag.TREZOR -> KeyType.TREZOR
        SignerTag.BITBOX -> KeyType.BITBOX
        else -> null
    }
    else -> null
}

fun KeyType.toSignerTypeAndTag(): Pair<SignerType, SignerTag?> = when (this) {
    KeyType.TAPSIGNER -> SignerType.NFC to null
    KeyType.COLDCARD -> SignerType.COLDCARD_NFC to null
    KeyType.PORTAL -> SignerType.PORTAL_NFC to null
    KeyType.JADE -> SignerType.AIRGAP to SignerTag.JADE
    KeyType.SEEDSIGNER -> SignerType.AIRGAP to SignerTag.SEEDSIGNER
    KeyType.KEYSTONE -> SignerType.AIRGAP to SignerTag.KEYSTONE
    KeyType.FOUNDATION -> SignerType.AIRGAP to SignerTag.PASSPORT
    KeyType.LEDGER -> SignerType.HARDWARE to SignerTag.LEDGER
    KeyType.TREZOR -> SignerType.HARDWARE to SignerTag.TREZOR
    KeyType.BITBOX -> SignerType.HARDWARE to SignerTag.BITBOX
    KeyType.SOFTWARE -> SignerType.SOFTWARE to null
    KeyType.PLATFORM_KEY -> SignerType.SERVER to null
    KeyType.GENERIC_AIRGAP -> SignerType.AIRGAP to null
}

fun SupportedSigner.toDisplayInfo(): SignerDisplayInfo? {
    val keyType = toKeyType() ?: return null
    return when (type) {
        SignerType.NFC -> SignerDisplayInfo(
            iconRes = R.drawable.ic_nfc_card,
            titleRes = R.string.nc_tapsigner,
            keyType = keyType,
            category = SignerDisplayCategory.CARD,
        )

        SignerType.COLDCARD_NFC -> SignerDisplayInfo(
            iconRes = R.drawable.ic_coldcard_small,
            titleRes = R.string.nc_coldcard,
            keyType = keyType,
            category = SignerDisplayCategory.CARD,
        )

        SignerType.AIRGAP -> when (tag) {
            SignerTag.JADE -> SignerDisplayInfo(
                iconRes = R.drawable.ic_air_gapped_jade,
                titleRes = R.string.nc_jade,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            SignerTag.SEEDSIGNER -> SignerDisplayInfo(
                iconRes = R.drawable.ic_air_gapped_seedsigner,
                titleRes = R.string.nc_seedsigner,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            SignerTag.KEYSTONE -> SignerDisplayInfo(
                iconRes = R.drawable.ic_air_gapped_keystone,
                titleRes = R.string.nc_keystone,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            SignerTag.PASSPORT -> SignerDisplayInfo(
                iconRes = R.drawable.ic_air_gapped_passport,
                titleRes = R.string.nc_foundation,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            else -> null
        }

        SignerType.PORTAL_NFC -> SignerDisplayInfo(
            iconRes = R.drawable.ic_portal_nfc,
            titleRes = R.string.nc_portal,
            keyType = keyType,
            category = SignerDisplayCategory.CARD,
        )

        SignerType.HARDWARE -> when (tag) {
            SignerTag.LEDGER -> SignerDisplayInfo(
                iconRes = R.drawable.ic_ledger_hardware,
                titleRes = R.string.nc_ledger,
                descriptionRes = R.string.nc_desktop_only,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            SignerTag.TREZOR -> SignerDisplayInfo(
                iconRes = R.drawable.ic_trezor_hardware,
                titleRes = R.string.nc_trezor,
                descriptionRes = R.string.nc_desktop_only,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            SignerTag.BITBOX -> SignerDisplayInfo(
                iconRes = R.drawable.ic_bitbox_hardware,
                titleRes = R.string.nc_bitbox,
                descriptionRes = R.string.nc_desktop_only,
                keyType = keyType,
                category = SignerDisplayCategory.CARD,
            )

            else -> null
        }

        SignerType.SOFTWARE -> SignerDisplayInfo(
            iconRes = R.drawable.ic_logo_dark_small,
            titleRes = R.string.nc_software,
            descriptionRes = R.string.nc_text_ss_desc,
            keyType = keyType,
            category = SignerDisplayCategory.ROW,
        )

        SignerType.SERVER -> SignerDisplayInfo(
            iconRes = R.drawable.ic_server_key_dark,
            titleRes = R.string.nc_server_key,
            descriptionRes = R.string.nc_platform_key_desc,
            keyType = keyType,
            category = SignerDisplayCategory.ROW,
        )

        else -> null
    }
}

fun SupportedSigner.isDisabledIn(
    allowedSigners: List<SupportedSigner>,
    isDisableAll: Boolean,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    keyFlow: Int = KeyFlow.NONE,
): Boolean = when (type) {
    SignerType.HARDWARE -> onChainAddSignerParam == null
    SignerType.SOFTWARE -> {
        val explicitlyAllowed =
            allowedSigners.isNotEmpty() && allowedSigners.any { it.matches(this) }
        !keyFlow.isPrimaryKeyFlow() && isDisableAll && !explicitlyAllowed
    }
    SignerType.SERVER -> isDisableAll
    else -> isDisableAll || (allowedSigners.isNotEmpty() && !allowedSigners.any { it.matches(this) })
}

private fun SupportedSigner.matches(target: SupportedSigner): Boolean = when (target.type) {
    SignerType.AIRGAP -> type == SignerType.AIRGAP && (tag == target.tag || tag == null)
    else -> type == target.type
}
