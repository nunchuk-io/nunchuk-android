package com.nunchuk.android.main.components.tabs.wallet.emptystate

import EmptyStateHomeView
import KeyWalletEntryView
import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.main.components.tabs.wallet.component.ArchivedWalletsRow
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.isByzantineOrFinney
import com.nunchuk.android.nav.NunchukNavigator
import org.matrix.android.sdk.api.extensions.orTrue

@Composable
internal fun WalletEmptyStateView(
    modifier: Modifier,
    activityContext: Activity,
    navigator: NunchukNavigator,
    groupStage: MembershipStage,
    assistedWalletId: String,
    hasSigner: Boolean,
    state: WalletsState,
    openArchivedWalletsScreen: () -> Unit,
) {
    val personalSteps = state.personalSteps
    val plans = state.plans.orEmpty()
    val walletType = when {
        personalSteps == null -> null
        personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
        personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
        else -> null
    }
    val conditionInfo = when {
        plans.size.orDefault(0) == 1 && plans.any {
            it in setOf(
                MembershipPlan.IRON_HAND,
                MembershipPlan.HONEY_BADGER,
                MembershipPlan.HONEY_BADGER_PLUS,
                MembershipPlan.HONEY_BADGER_PREMIER
            )
        }.orTrue() -> {
            ConditionInfo.PersonalPlanUser(
                resumeWizard = groupStage !in setOf(
                    MembershipStage.DONE,
                    MembershipStage.NONE
                ),
                resumeWizardMinutes = state.remainingTime,
                walletType = walletType,
                hasSigner = hasSigner,
                groupStep = groupStage,
                assistedWalletId = assistedWalletId,
                plan = plans.firstOrNull() ?: MembershipPlan.NONE
            )
        }

        plans.size.orDefault(0) == 1 && plans
            .any { it.isByzantineOrFinney() } -> {
            ConditionInfo.GroupMasterUser(hasSigner)
        }

        plans.size.orDefault(0) > 1 -> {
            ConditionInfo.MultipleSubscriptionsUser(hasSigner)
        }

        state.wallets.isEmpty() -> {
            ConditionInfo.FreeGuestUser(hasSigner = hasSigner)
        }

        else -> {
            ConditionInfo.FreeGuestUser(hasSigner = hasSigner)
        }
    }
    val emptyState = when (conditionInfo) {
        is ConditionInfo.PersonalPlanUser -> EmptyStatePersonalPlanUser(
            navigator,
            activityContext
        )

        is ConditionInfo.MultipleSubscriptionsUser -> EmptyStateMultipleSubscriptionsUser(
            navigator,
            activityContext
        )

        is ConditionInfo.GroupMasterUser -> EmptyStateGroupMasterUser(
            navigator,
            activityContext
        )

        is ConditionInfo.FreeGuestUser -> EmptyStateFreeGuestUser(
            navigator,
            activityContext
        )

        else -> EmptyStateNone()
    }
    val contentData = emptyState.getWizardData(conditionInfo, NunchukTheme.isDark)
    val keyWalletEntryDataList = emptyState.getKeyWalletEntryData(conditionInfo)

    val onEmptyStateActionButtonClick = { contentData?.buttonAction?.invoke() }
    Column(
        modifier = modifier,
    ) {
        if (contentData != null) {
            EmptyStateHomeView(contentData, onActionButtonClick = {
                onEmptyStateActionButtonClick()
            })
        }
        keyWalletEntryDataList.forEach { it ->
            Spacer(modifier = Modifier.height(12.dp))
            KeyWalletEntryView(it, onClick = {
                it.buttonAction.invoke()
            })
        }

        if (state.totalArchivedWallet > 0) {
            ArchivedWalletsRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                count = state.totalArchivedWallet,
                onClick = openArchivedWalletsScreen
            )
        }
    }
}