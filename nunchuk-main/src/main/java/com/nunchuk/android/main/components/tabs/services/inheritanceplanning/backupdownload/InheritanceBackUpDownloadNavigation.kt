package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload

import androidx.activity.compose.LocalActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceBackUpDownloadRoute(
    /**
     * The key has both recovery methods, so this is step 1 of 2 and the seed phrase screen follows.
     * Reached from the single "Info" on the review plan's inheritance key card.
     */
    val continueToSeedPhrase: Boolean = false,
)

fun NavGraphBuilder.inheritanceBackUpDownload(
    onContinueClicked: (InheritanceBackUpDownloadRoute) -> Unit,
) {
    composable<InheritanceBackUpDownloadRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceBackUpDownloadRoute>()
        InheritanceBackUpDownloadContent(
            isFirstOfTwo = route.continueToSeedPhrase,
            onContinueClicked = { onContinueClicked(route) },
        )
    }
}

fun NavController.navigateToInheritanceBackUpDownload(continueToSeedPhrase: Boolean = false) {
    navigate(InheritanceBackUpDownloadRoute(continueToSeedPhrase = continueToSeedPhrase))
}
