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
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.main.components.tabs.wallet.WalletsViewModel
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.isByzantineOrFinney
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.signer.SignersViewModel
import org.matrix.android.sdk.api.extensions.orTrue

@Composable
internal fun WalletEmptyStateView(
    activityContext: Activity,
    navigator: NunchukNavigator,
    walletsViewModel: WalletsViewModel,
    signersViewModel: SignersViewModel,
    state: WalletsState
) {
    val personalSteps = walletsViewModel.getPersonalSteps()
    val plans = walletsViewModel.getPlans().orEmpty()
    val walletType = when {
        personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
        personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
        else -> null
    }
    NunchukTheme(false) {
        val contentData: WizardData?
        val keyWalletEntryData: KeyWalletEntryData?
        val conditionInfo = when {
            state.plans?.size.orDefault(0) == 1 && state.plans?.any {
                it in setOf(
                    MembershipPlan.IRON_HAND,
                    MembershipPlan.HONEY_BADGER,
                    MembershipPlan.HONEY_BADGER_PLUS
                )
            }.orTrue() -> {
                ConditionInfo.PersonalPlanUser(
                    resumeWizard = walletsViewModel.getGroupStage() !in setOf(
                        MembershipStage.DONE,
                        MembershipStage.NONE
                    ),
                    resumeWizardMinutes = state.remainingTime,
                    walletType = walletType,
                    hasSigner = signersViewModel.hasSigner(),
                    groupStep = walletsViewModel.getGroupStage(),
                    assistedWalletId = walletsViewModel.getAssistedWalletId(),
                    plan = plans.firstOrNull() ?: MembershipPlan.NONE
                )
            }

            state.plans?.size.orDefault(0) > 1 && state.plans.orEmpty()
                .any { it.isByzantineOrFinney() } -> {
                ConditionInfo.MultipleSubscriptionsUser(signersViewModel.hasSigner())
            }

            state.wallets.isEmpty() -> {
                ConditionInfo.FreeGuestUser(hasSigner = signersViewModel.hasSigner())
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

            is ConditionInfo.FreeGuestUser -> EmptyStateFreeGuestUser(
                navigator,
                activityContext
            )

            else -> EmptyStateNone()
        }
        contentData = emptyState.getWizardData(conditionInfo)
        keyWalletEntryData = emptyState.getKeyWalletEntryData(conditionInfo)

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