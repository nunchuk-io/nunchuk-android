package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceShareSecretRoute(
    val sourceFlow: Int = InheritanceSourceFlow.NONE,
    val magicalPhrase: String = "",
    val planFlow: Int = InheritancePlanFlow.NONE,
    val walletId: String = "",
)

fun NavGraphBuilder.inheritanceShareSecret(
    onContinueClick: (InheritanceShareSecretRoute, type: Int) -> Unit,
) {
    composable<InheritanceShareSecretRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceShareSecretRoute>()
        val viewModel = hiltViewModel<InheritanceShareSecretViewModel>()
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(viewModel, lifecycleOwner, route) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceShareSecretEvent.ContinueClick -> {
                            onContinueClick(route, event.type)
                        }
                    }
                }
        }

        InheritanceShareSecretScreen(
            viewModel = viewModel,
            planFlow = route.planFlow,
        )
    }
}

fun NavController.navigateToInheritanceShareSecret(
    sourceFlow: Int = InheritanceSourceFlow.NONE,
    magicalPhrase: String = "",
    planFlow: Int = InheritancePlanFlow.NONE,
    walletId: String = "",
) {
    navigate(
        InheritanceShareSecretRoute(
            sourceFlow = sourceFlow,
            magicalPhrase = magicalPhrase,
            planFlow = planFlow,
            walletId = walletId,
        )
    )
}
