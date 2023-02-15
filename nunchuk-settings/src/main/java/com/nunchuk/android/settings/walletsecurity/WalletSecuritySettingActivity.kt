package com.nunchuk.android.settings.walletsecurity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.settings.databinding.ActivityWalletSecuritySettingBinding
import com.nunchuk.android.settings.developer.DeveloperSettingActivity
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletSecuritySettingActivity : BaseActivity<ActivityWalletSecuritySettingBinding>() {

    private val viewModel: WalletSecuritySettingViewModel by viewModels()

    override fun initializeBinding() = ActivityWalletSecuritySettingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        showToolbarBackButton()

        setupViews()
        observeEvent()
    }

    private fun showToolbarBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: WalletSecuritySettingState) {
        binding.hideWalletDetailOption.setOptionChecked(state.walletSecuritySetting.hideWalletDetail)
    }

    private fun handleEvent(event: WalletSecuritySettingEvent) {
        when (event) {
            is WalletSecuritySettingEvent.Error -> NCToastMessage(this).showError(message = event.message)
            is WalletSecuritySettingEvent.Loading -> showOrHideLoading(loading = event.loading)
            WalletSecuritySettingEvent.UpdateConfigSuccess -> {

            }
        }
    }

    private fun setupViews() {
        binding.hideWalletDetailOption.setOptionChangeListener {
            viewModel.updateHideWalletDetail(it)
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    WalletSecuritySettingActivity::class.java
                )
            )
        }
    }
}