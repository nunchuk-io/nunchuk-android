package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceFallbackSettingsRoute

internal fun NavGraphBuilder.inheritanceFallbackSettings(
    initialValueProvider: () -> InheritanceFallbackSettingsValue?,
    finalScheduledPayoutTimeMillisProvider: () -> Long?,
    onBackClicked: () -> Unit,
    onContinueClicked: (InheritanceFallbackSettingsValue) -> Unit,
) {
    composable<InheritanceFallbackSettingsRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        InheritanceFallbackSettingsScreen(
            remainTime = remainTime,
            finalScheduledPayoutTimeMillis = finalScheduledPayoutTimeMillisProvider(),
            initialValue = initialValueProvider() ?: InheritanceFallbackSettingsValue(
                selectedOption = InheritanceFallbackOption.INACTIVITY_FALLBACK,
                triggerValue = "5",
                triggerUnit = FallbackTriggerUnit.YEAR,
                fallbackDate = "05/29/2050",
            ),
            onBackClicked = onBackClicked,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceFallbackSettings() { navigate(InheritanceFallbackSettingsRoute) }
