package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase

import com.nunchuk.android.model.InheritanceClaimingInit

sealed class ClaimMagicPhraseDialog {
    data object SubscriptionExpired : ClaimMagicPhraseDialog()
    data class InActivated(val message: String) : ClaimMagicPhraseDialog()
    data class PleaseComeLater(val message: String) : ClaimMagicPhraseDialog()
    data class SecurityDepositRequired(val message: String) : ClaimMagicPhraseDialog()
}

data class ClaimMagicPhraseState(
    val magicalPhrase: String = "",
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val initResult: InheritanceClaimingInit? = null,
    val error: String? = null,
    val dialog: ClaimMagicPhraseDialog? = null
) {
    val formattedMagicPhrase: String
        get() = magicalPhrase.trim()
}

