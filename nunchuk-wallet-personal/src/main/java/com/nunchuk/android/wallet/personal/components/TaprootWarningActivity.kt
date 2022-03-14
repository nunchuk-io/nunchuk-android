package com.nunchuk.android.wallet.personal.components

import android.content.Context
import android.os.Bundle
import androidx.core.text.HtmlCompat
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityTaprootWarningBinding
import com.nunchuk.android.widget.util.setLightStatusBar

class TaprootWarningActivity : BaseActivity<ActivityTaprootWarningBinding>() {

    private val args: TaprootWarningArgs by lazy { TaprootWarningArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityTaprootWarningBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
    }

    private fun setupViews() {
        binding.withdrawDesc.text = HtmlCompat.fromHtml(
            getString(R.string.nc_wallet_taproot_withdraw_support_desc),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.btnContinue.setOnClickListener {
            finish()
            navigator.openConfigureWalletScreen(
                activityContext = this,
                walletName = args.walletName,
                walletType = args.walletType,
                addressType = args.addressType
            )
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(TaprootWarningArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }
}
