package com.nunchuk.android.main.membership.byzantine

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.BaseIntroFragment

class SecurityTradeOffFragment : BaseIntroFragment() {
    override val title: String
        get() = "Security trade-off"
    override val imageResId: Int
        get() = R.drawable.bg_security_trade_off
    override val content: String
        get() = getString(R.string.nc_security_trade_off_desc)

    override val styles: Map<SpanIndicator, SpanStyle>
        get() = mapOf(
            SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold)
        )

    override fun onContinueClicked() {
        findNavController().navigate(
            SecurityTradeOffFragmentDirections.actionSecurityTradeOffFragmentToSelectGroupFragment()
        )
    }

    override val isCountdown: Boolean = false
}