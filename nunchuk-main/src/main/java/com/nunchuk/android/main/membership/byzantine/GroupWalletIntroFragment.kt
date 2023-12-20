package com.nunchuk.android.main.membership.byzantine

import androidx.navigation.fragment.findNavController
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.BaseIntroFragment

class GroupWalletIntroFragment : BaseIntroFragment() {
    override val title: String
        get() = getString(R.string.nc_what_is_a_group_wallet)
    override val imageResId: Int
        get() = R.drawable.bg_group_wallet_intro
    override val content: String
        get() = getString(R.string.nc_group_wallet_intro)

    override fun onContinueClicked() {
        runCatching {
            findNavController().navigate(
                GroupWalletIntroFragmentDirections.actionGroupWalletIntroFragmentToSecurityTradeOffFragment()
            )
        }
    }

    override val isCountdown: Boolean = false
}