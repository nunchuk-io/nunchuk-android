package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.seedphrasebackupinfo

import androidx.activity.compose.LocalActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceSeedPhraseBackupInfoRoute(
    /**
     * The key has both recovery methods, so this is step 2 of 2 — reached from the Backup Password
     * screen. It closes the pair rather than this screen alone, and carries the joint-control note.
     */
    val hasBothMethods: Boolean = false,
)

fun NavGraphBuilder.inheritanceSeedPhraseBackupInfo(
    onContinueClicked: (InheritanceSeedPhraseBackupInfoRoute) -> Unit,
) {
    composable<InheritanceSeedPhraseBackupInfoRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceSeedPhraseBackupInfoRoute>()
        InheritanceSeedPhraseBackupInfoContent(
            hasBothMethods = route.hasBothMethods,
            onContinueClicked = { onContinueClicked(route) },
        )
    }
}

fun NavController.navigateToInheritanceSeedPhraseBackupInfo(hasBothMethods: Boolean = false) {
    navigate(InheritanceSeedPhraseBackupInfoRoute(hasBothMethods = hasBothMethods))
}
