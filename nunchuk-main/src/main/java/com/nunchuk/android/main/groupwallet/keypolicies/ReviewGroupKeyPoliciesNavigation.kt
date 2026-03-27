package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class ReviewGroupKeyPoliciesRoute(
    val walletId: String = "",
    val dummyTransactionId: String = "",
) {
    companion object {
        fun from(savedStateHandle: SavedStateHandle): ReviewGroupKeyPoliciesRoute {
            return ReviewGroupKeyPoliciesRoute(
                walletId = savedStateHandle.get<String>("walletId").orEmpty(),
                dummyTransactionId = savedStateHandle.get<String>("dummyTransactionId").orEmpty(),
            )
        }
    }
}

fun NavGraphBuilder.reviewGroupKeyPolicies(
    onBackClicked: () -> Unit = {},
    onOpenWalletAuthentication: (walletId: String, dummyTransactionId: String) -> Unit = { _, _ -> },
    onDiscardSuccess: () -> Unit = {},
) {
    composable<ReviewGroupKeyPoliciesRoute> {
        ReviewGroupKeyPoliciesScreen(
            onBackClicked = onBackClicked,
            onOpenWalletAuthentication = onOpenWalletAuthentication,
            onDiscardSuccess = onDiscardSuccess,
        )
    }
}

fun NavController.navigateToReviewGroupKeyPolicies(
    walletId: String,
    dummyTransactionId: String,
) {
    navigate(
        ReviewGroupKeyPoliciesRoute(
            walletId = walletId,
            dummyTransactionId = dummyTransactionId,
        )
    )
}
