package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceCreateSuccessRoute(
    val sourceFlow: Int = InheritanceSourceFlow.NONE,
    val magicalPhrase: String = "",
    val planFlow: Int = InheritancePlanFlow.NONE,
    val walletId: String = "",
)

fun NavGraphBuilder.inheritanceCreateSuccess(
    onContinueClick: (InheritanceCreateSuccessRoute) -> Unit,
) {
    composable<InheritanceCreateSuccessRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<InheritanceCreateSuccessRoute>()
        InheritanceCreateSuccessScreenContent(
            onContinueClick = { onContinueClick(route) },
        )
    }
}

fun NavController.navigateToInheritanceCreateSuccess(
    sourceFlow: Int = InheritanceSourceFlow.NONE,
    magicalPhrase: String = "",
    planFlow: Int = InheritancePlanFlow.NONE,
    walletId: String = "",
) {
    navigate(
        InheritanceCreateSuccessRoute(
            sourceFlow = sourceFlow,
            magicalPhrase = magicalPhrase,
            planFlow = planFlow,
            walletId = walletId,
        )
    )
}
