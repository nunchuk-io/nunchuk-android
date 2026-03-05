package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks

import androidx.activity.compose.LocalActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceHowItWorksRoute(val type: Int)

fun NavGraphBuilder.inheritanceHowItWorks(
    onDoneClick: () -> Unit,
) {
    composable<InheritanceHowItWorksRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceHowItWorksRoute>()
        InheritanceHowItWorksScreen(
            type = InheritanceShareSecretType.entries.getOrElse(route.type) {
                InheritanceShareSecretType.DIRECT
            },
            onDoneClick = onDoneClick,
        )
    }
}

fun NavController.navigateToInheritanceHowItWorks(type: Int) {
    navigate(InheritanceHowItWorksRoute(type = type))
}
