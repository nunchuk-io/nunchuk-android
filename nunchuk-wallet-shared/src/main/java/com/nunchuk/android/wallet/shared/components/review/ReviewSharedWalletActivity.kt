package com.nunchuk.android.wallet.shared.components.review

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.databinding.ActivityReviewSharedWalletBinding
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.util.setLightStatusBar

class ReviewSharedWalletActivity : BaseActivity<ActivityReviewSharedWalletBinding>() {

    private val args: ReviewSharedWalletArgs by lazy { ReviewSharedWalletArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityReviewSharedWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.walletName.text = args.walletName
        binding.configuration.bindWalletConfiguration(
            totalSigns = args.totalSigns,
            requireSigns = args.requireSigns
        )

        binding.walletType.text = args.walletType.toReadableString(this)
        binding.addressType.text = args.addressType.toReadableString(this)

        binding.btnContinue.setOnClickListener {
            openAssignSignerScreen()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun openAssignSignerScreen() {
        navigator.openAssignSignerSharedWalletScreen(
            this,
            walletName = args.walletName,
            walletType = args.walletType,
            addressType = args.addressType,
            totalSigns = args.totalSigns,
            requireSigns = args.requireSigns
        )
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            totalSigns: Int,
            requireSigns: Int
        ) {
            activityContext.startActivity(
                ReviewSharedWalletArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    totalSigns = totalSigns,
                    requireSigns = requireSigns
                ).buildIntent(activityContext)
            )
        }
    }

}