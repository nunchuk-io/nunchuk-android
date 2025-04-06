package com.nunchuk.android.main.components.tabs.wallet.component

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.main.databinding.ContainerAssistedWalletBinding
import com.nunchuk.android.model.MembershipStage

@Composable
internal fun AssistedWalletIntro(
    modifier: Modifier = Modifier,
    state: WalletsState,
    stage: MembershipStage,
    context: Context,
    onClick: () -> Unit = {},
) {
    val walletName = state.assistedWallets.firstOrNull()
        ?.let { wallet -> state.wallets.find { wallet.localId == it.wallet.id }?.wallet?.name.orEmpty() }
    AndroidViewBinding(
        modifier = modifier
            .clickable(onClick = onClick),
        factory = ContainerAssistedWalletBinding::inflate
    ) {
        if (stage == MembershipStage.NONE) {
            tvIntroTitle.text =
                context.getString(R.string.nc_let_s_get_you_started)
            tvIntroDesc.text =
                context.getString(R.string.nc_assisted_wallet_intro_desc)
            tvIntroAction.text =
                context.getString(R.string.nc_start_wizard)
        } else if (stage == MembershipStage.SETUP_INHERITANCE) {
            tvIntroTitle.text =
                context.getString(
                    R.string.nc_setup_inheritance_for,
                    walletName.orEmpty()
                )
            tvIntroDesc.text =
                context.getString(R.string.nc_estimate_remain_time, 21)
            tvIntroAction.text =
                context.getString(R.string.nc_do_it_now)
        } else if (stage != MembershipStage.DONE) {
            tvIntroTitle.text =
                context.getString(R.string.nc_you_almost_done)
            tvIntroDesc.text =
                context.getString(
                    R.string.nc_estimate_remain_time,
                    state.remainingTime
                )
            tvIntroAction.text =
                context.getString(R.string.nc_continue_setting_your_wallet)
        }
    }
}