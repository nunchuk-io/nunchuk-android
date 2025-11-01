package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.model.InheritanceClaimingInit
import kotlinx.serialization.Serializable

@Serializable
data object ClaimMagicPhraseRoute

fun NavGraphBuilder.claimMagicPhrase(
    onBackPressed: () -> Unit = {},
    onContinue: (String, InheritanceClaimingInit) -> Unit,
) {
    composable<ClaimMagicPhraseRoute> {
        ClaimMagicPhraseScreen(
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

