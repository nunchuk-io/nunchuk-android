package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload

import androidx.activity.compose.LocalActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceBackUpDownloadRoute

fun NavGraphBuilder.inheritanceBackUpDownload(
    onContinueClicked: () -> Unit,
) {
    composable<InheritanceBackUpDownloadRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        InheritanceBackUpDownloadContent(
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceBackUpDownload() { navigate(InheritanceBackUpDownloadRoute) }
