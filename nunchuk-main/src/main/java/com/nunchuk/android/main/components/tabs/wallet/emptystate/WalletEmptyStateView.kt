package com.nunchuk.android.main.components.tabs.wallet.emptystate

import EmptyStateHomeView
import KeyWalletEntryView
import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.isByzantineOrFinney
import com.nunchuk.android.nav.NunchukNavigator
import org.matrix.android.sdk.api.extensions.orTrue

@Composable
internal fun WalletEmptyStateView(
    activityContext: Activity,
    navigator: NunchukNavigator,
    groupStage: MembershipStage,
    assistedWalletId: String,
    signers: List<SignerModel>?,
    state: WalletsState
) {
    if (state.plans == null || state.personalSteps == null || signers == null) {
        return
    }
    val hasSigner = signers.isNotEmpty()
    val personalSteps = state.personalSteps
    val plans = state.plans
    val walletType = when {
        personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
        personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
        else -> null
    }
    NunchukTheme {
        val conditionInfo = when {
            state.plans.size.orDefault(0) == 1 && state.plans.any {
                it in setOf(
                    MembershipPlan.IRON_HAND,
                    MembershipPlan.HONEY_BADGER,
                    MembershipPlan.HONEY_BADGER_PLUS
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

            state.plans.size.orDefault(0) == 1 && state.plans
                .any { it.isByzantineOrFinney() } -> {
                ConditionInfo.GroupMasterUser(hasSigner)
            }

            state.plans.size.orDefault(0) > 1 -> {
                ConditionInfo.MultipleSubscriptionsUser(hasSigner)
            }

            state.wallets.isEmpty() -> {
                ConditionInfo.FreeGuestUser(hasSigner = hasSigner)
            }

            else -> ConditionInfo.None
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
        val contentData = emptyState.getWizardData(conditionInfo)
        val keyWalletEntryData = emptyState.getKeyWalletEntryData(conditionInfo)

        val onEmptyStateActionButtonClick = { contentData?.buttonAction?.invoke() }
        val onKeyWalletEntryClick = { keyWalletEntryData?.buttonAction?.invoke() }
        Column {
            if (contentData != null) {
                EmptyStateHomeView(contentData, onActionButtonClick = {
                    onEmptyStateActionButtonClick()
                })
            }
            if (keyWalletEntryData != null) {
                Spacer(modifier = Modifier.height(12.dp))
                KeyWalletEntryView(keyWalletEntryData, onClick = {
                    onKeyWalletEntryClick()
                })
            }
        }
    }
}