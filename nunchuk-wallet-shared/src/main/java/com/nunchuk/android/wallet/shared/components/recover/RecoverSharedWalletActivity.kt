package com.nunchuk.android.wallet.shared.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.databinding.ActivityRecoverSharedWalletBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class RecoverSharedWalletActivity : BaseActivity<ActivityRecoverSharedWalletBinding>() {

    private val viewModel: RecoverSharedWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityRecoverSharedWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: RecoverSharedWalletEvent) {
        when (event) {
            is RecoverSharedWalletEvent.RecoverSharedWalletSuccess -> {

            }
        }
    }


    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnRecoverUsingBSMS.setOnClickListener {
            openSelectFileChooser(REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val file = intent?.data?.let {
                getFileFromUri(contentResolver, it, cacheDir)
            }
            file?.let {
                moveToAddRecoverSharedWalletScreen(it.readText())
            }
        }
    }

    private fun moveToAddRecoverSharedWalletScreen(data: String) {
        navigator.openAddRecoverSharedWalletScreen(
            this,data
        )
    }


    companion object {
        private const val REQUEST_CODE = 10000

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, RecoverSharedWalletActivity::class.java))
        }
    }

}