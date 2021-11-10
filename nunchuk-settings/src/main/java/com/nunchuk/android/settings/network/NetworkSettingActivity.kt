package com.nunchuk.android.settings.network

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.bus.RestartAppEventBus
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.ActivityNetworkSettingBinding
import com.nunchuk.android.type.Chain
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import timber.log.Timber
import javax.inject.Inject

class NetworkSettingActivity : BaseActivity<ActivityNetworkSettingBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: NetworkSettingViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityNetworkSettingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        showToolbarBackButton()

        setupViews()
        setupData()
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

    private fun handleState(state: NetworkSettingState) {
        binding.rbMainNet.isChecked = state.appSetting.chain == Chain.MAIN
        binding.btnResetMainNet.isVisible = state.appSetting.chain == Chain.MAIN
        binding.rbTestNet.isChecked = state.appSetting.chain == Chain.TESTNET
        binding.btnResetTestNet.isVisible = state.appSetting.chain == Chain.TESTNET

        binding.tvMainNetHost.apply {
            background = if (state.appSetting.chain == Chain.MAIN) {
                ContextCompat.getDrawable(this@NetworkSettingActivity, R.drawable.nc_edit_text_bg)
            } else {
                ContextCompat.getDrawable(this@NetworkSettingActivity, R.drawable.nc_edit_text_bg_disabled)
            }
            setTextColor(
                ColorStateList.valueOf(
                    if (state.appSetting.chain == Chain.MAIN) {
                        ContextCompat.getColor(
                            this@NetworkSettingActivity,
                            R.color.nc_primary_color
                        )
                    } else {
                        ContextCompat.getColor(
                            this@NetworkSettingActivity,
                            R.color.nc_grey_dark_color
                        )
                    }
                )
            )
            inputType = if (state.appSetting.chain == Chain.MAIN) InputType.TYPE_CLASS_TEXT else InputType.TYPE_NULL
        }
        binding.tvTestNetHost.apply {
            background = if (state.appSetting.chain == Chain.TESTNET) {
                ContextCompat.getDrawable(this@NetworkSettingActivity, R.drawable.nc_edit_text_bg)
            } else {
                ContextCompat.getDrawable(this@NetworkSettingActivity, R.drawable.nc_edit_text_bg_disabled)
            }
            setTextColor(
                ColorStateList.valueOf(
                    if (state.appSetting.chain == Chain.TESTNET) {
                        ContextCompat.getColor(
                            this@NetworkSettingActivity,
                            R.color.nc_primary_color
                        )
                    } else {
                        ContextCompat.getColor(
                            this@NetworkSettingActivity,
                            R.color.nc_grey_dark_color
                        )
                    }
                )
            )
            inputType = if (state.appSetting.chain == Chain.TESTNET) InputType.TYPE_CLASS_TEXT else InputType.TYPE_NULL

        }

        val isChangedSetting = viewModel.currentAppSettings != viewModel.initAppSettings
        binding.btnSave.isVisible = isChangedSetting
        binding.btnSaveDisable.isVisible = !isChangedSetting
    }

    private fun handleEvent(event: NetworkSettingEvent) {
        when (event) {
            is NetworkSettingEvent.UpdateSettingSuccessEvent -> {
                handleUpdateAppSettingsSuccess()
            }
            is NetworkSettingEvent.ResetTextHostServerEvent -> {
                binding.tvMainNetHost.setText(
                   event.appSetting.mainnetServers?.get(0).orEmpty()
                )
                binding.tvTestNetHost.setText(
                    event.appSetting.testnetServers?.get(0).orEmpty()
                )
            }
        }
    }

    private fun handleUpdateAppSettingsSuccess() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_app_restart_required),
            message = getString(R.string.nc_text_app_restart_des),
            btnYes = getString(R.string.nc_text_restart),
            btnNo = getString(R.string.nc_text_discard),
            onYesClick = {
                finish()
                RestartAppEventBus.instance().publish()
            }
        )
    }

    private fun setupViews() {

        // currently we only support connect to electurm server
        binding.electrumServerSwitch.isChecked = true
        binding.electrumServerSwitch.isClickable = false
        binding.electrumServerSwitch.isEnabled = false

        binding.rbMainNet.setOnCheckedChangeListener { _, checked ->
            viewModel.currentAppSettings?.copy(
                chain = if (checked) Chain.MAIN else Chain.TESTNET,
                mainnetServers = listOf(binding.tvMainNetHost.text.toString()),
                testnetServers = listOf(binding.tvTestNetHost.text.toString()),
            )?.let {
                viewModel.updateCurrentState(it)
            }
        }

        binding.rbTestNet.setOnCheckedChangeListener { _, checked ->
            viewModel.currentAppSettings?.copy(
                chain = if (checked) Chain.TESTNET else Chain.MAIN,
                mainnetServers = listOf(binding.tvMainNetHost.text.toString()),
                testnetServers = listOf(binding.tvTestNetHost.text.toString())
            )?.let {
                viewModel.updateCurrentState(it)
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.currentAppSettings?.let {
                viewModel.updateAppSettings(it)
            }
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetToDefaultAppSetting()
        }
        binding.btnResetMainNet.setOnClickListener {
            viewModel.currentAppSettings?.copy(
                mainnetServers = listOf(viewModel.initAppSettings?.mainnetServers?.get(0).orEmpty())
            )?.let {
                viewModel.updateCurrentState(it)
                viewModel.fireResetTextHostServerEvent(it)
            }
        }
        binding.btnResetTestNet.setOnClickListener {
            viewModel.currentAppSettings?.copy(
                testnetServers = listOf(viewModel.initAppSettings?.testnetServers?.get(0).orEmpty())
            )?.let {
                viewModel.updateCurrentState(it)
                viewModel.fireResetTextHostServerEvent(it)
            }
        }

        binding.tvMainNetHost.addTextChangedCallback {
            viewModel.currentAppSettings?.copy(
                mainnetServers = listOf(it),
            )?.let { newAppSetting ->
                viewModel.updateCurrentState(newAppSetting)
            }
        }
        binding.tvTestNetHost.addTextChangedCallback {
            viewModel.currentAppSettings?.copy(
                testnetServers = listOf(it),
            )?.let { newAppSetting ->
                viewModel.updateCurrentState(newAppSetting)
            }
        }


    }

    private fun setupData() {
        viewModel.getAppSettings()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, NetworkSettingActivity::class.java))
        }
    }
}