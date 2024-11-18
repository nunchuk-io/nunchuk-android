/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.settings.network

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.constants.Constants.GLOBAL_SIGNET_EXPLORER
import com.nunchuk.android.core.constants.Constants.SIG_NET_HOST
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.ActivityNetworkSettingBinding
import com.nunchuk.android.type.Chain
import com.nunchuk.android.utils.getTrimmedText
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class NetworkSettingActivity : BaseActivity<ActivityNetworkSettingBinding>() {

    private val viewModel: NetworkSettingViewModel by viewModels()

    private val selectServerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val server =
                    result.data?.getStringExtra(SelectElectrumServerActivity.EXTRA_SERVER).orEmpty()
                val name =
                    result.data?.getStringExtra(SelectElectrumServerActivity.EXTRA_NAME)
                if (result.data?.getBooleanExtra(SelectElectrumServerActivity.EXTRA_SHOW_MESSAGE, false) == true) {
                    NCToastMessage(this).show(getString(R.string.nc_server_added))
                }
                binding.tvMainNetHost.text = name ?: server
                Timber.d("Selected server: $server")
                viewModel.currentAppSettings?.copy(
                    mainnetServers = listOf(server)
                )?.let(viewModel::updateCurrentState)
            }
        }

    override fun initializeBinding() = ActivityNetworkSettingBinding.inflate(layoutInflater).also {
        enableEdgeToEdge()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private fun handleState(state: NetworkSettingState) {
        binding.rbMainNet.isChecked = state.appSetting.chain == Chain.MAIN
        binding.rbTestNet.isChecked = state.appSetting.chain == Chain.TESTNET
        binding.rbSigNet.isChecked = state.appSetting.chain == Chain.SIGNET

        binding.ivMainNetArrow.isVisible = binding.rbMainNet.isChecked

        binding.groupSigNetExplorer.isVisible = state.appSetting.chain == Chain.SIGNET

        binding.edtExploreAddressSigNetHost.setText(state.appSetting.signetExplorerHost)
        binding.edtExploreAddressSigNetHost.isVisible =
            state.appSetting.signetExplorerHost.isNotEmpty() && state.appSetting.chain == Chain.SIGNET
        binding.exploreAddressSwitch.isChecked = state.appSetting.signetExplorerHost.isNotEmpty()

        binding.tvMainNetHost.setupNetworkViewInfo(
            currentChain = Chain.MAIN,
            selectedChain = state.appSetting.chain
        )
        binding.tvTestNetHost.setupNetworkViewInfo(
            currentChain = Chain.TESTNET,
            selectedChain = state.appSetting.chain
        )
        binding.tvSigNetHost.setupNetworkViewInfo(
            currentChain = Chain.SIGNET,
            selectedChain = state.appSetting.chain
        )

        setupViewsWhenAppSettingChanged()
    }

    private fun isAppSettingChanged(): Boolean {
        return viewModel.currentAppSettings?.copy(
            signetExplorerHost = binding.edtExploreAddressSigNetHost.getTrimmedText()
        ) != viewModel.initAppSettings
    }

    private fun TextView.setupNetworkViewInfo(currentChain: Chain, selectedChain: Chain) {
        background = if (currentChain == selectedChain) {
            ContextCompat.getDrawable(this@NetworkSettingActivity, R.drawable.nc_edit_text_bg)
        } else {
            ContextCompat.getDrawable(
                this@NetworkSettingActivity,
                R.drawable.nc_edit_text_bg_disabled
            )
        }
        setTextColor(
            ColorStateList.valueOf(
                if (currentChain == selectedChain) {
                    ContextCompat.getColor(
                        this@NetworkSettingActivity,
                        R.color.nc_text_primary
                    )
                } else {
                    ContextCompat.getColor(
                        this@NetworkSettingActivity,
                        R.color.nc_second_color
                    )
                }
            )
        )
        isEnabled = currentChain == selectedChain
    }

    private fun handleEvent(event: NetworkSettingEvent) {
        when (event) {
            is NetworkSettingEvent.UpdateSettingSuccessEvent -> {
                handleUpdateAppSettingsSuccess()
            }

            is NetworkSettingEvent.ResetTextHostServerEvent -> {
                binding.tvMainNetHost.setText(
                    event.appSetting.mainnetServers[0]
                )
                binding.tvTestNetHost.setText(
                    event.appSetting.testnetServers[0]
                )
                binding.tvSigNetHost.setText(
                    if (event.appSetting.signetServers.isEmpty()) {
                        SIG_NET_HOST
                    } else {
                        event.appSetting.signetServers[0]
                    }
                )
            }

            is NetworkSettingEvent.LoadingEvent -> showLoading()
            NetworkSettingEvent.SignOutSuccessEvent -> {
                hideLoading()
                restartApp()
            }
        }
    }

    private fun restartApp() {
        navigator.restartApp(this)
    }

    private fun handleUpdateAppSettingsSuccess() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_app_restart_required),
            message = getString(R.string.nc_text_app_restart_des),
            btnYes = getString(R.string.nc_text_restart),
            btnNo = getString(R.string.nc_text_discard),
            onYesClick = {
                viewModel.signOut()
            }
        )
    }

    private fun setupViews() {
        // currently we only support connect to electurm server
        binding.electrumServerSwitch.isChecked = true
        binding.electrumServerSwitch.isClickable = false
        binding.electrumServerSwitch.isEnabled = false
        binding.tvTestNetHost.inputType = InputType.TYPE_CLASS_TEXT
        binding.tvSigNetHost.inputType = InputType.TYPE_CLASS_TEXT

        binding.rbMainNet.setOnCheckedChangeListener { view, checked ->
            if (checked) {
                handleCheckboxChangeListener(view.id)
            }
        }

        binding.rbTestNet.setOnCheckedChangeListener { view, checked ->
            if (checked) {
                handleCheckboxChangeListener(view.id)
            }
        }

        binding.rbSigNet.setOnCheckedChangeListener { view, checked ->
            if (checked) {
                handleCheckboxChangeListener(view.id)
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.currentAppSettings?.let {
                viewModel.updateAppSettings(
                    it.copy(
                        signetExplorerHost = binding.edtExploreAddressSigNetHost.getTrimmedText()
                    )
                )
            }
        }

        binding.btnReset.setOnClickListener {
            currentFocus?.clearFocus()
            viewModel.resetToDefaultAppSetting()
        }
        binding.tvTestNetHost.addTextChangedCallback {
            handleNetworkHostTextCallBack(it, Chain.TESTNET)
        }
        binding.tvSigNetHost.addTextChangedCallback {
            handleNetworkHostTextCallBack(it, Chain.SIGNET)
        }
        val openSelectElectrumServer : () -> Unit = {
            selectServerLauncher.launch(
                SelectElectrumServerActivity.buildIntent(
                    activity = this,
                    chain = Chain.MAIN,
                    server = viewModel.currentAppSettings?.mainnetServers?.firstOrNull().orEmpty()
                )
            )
        }
        binding.ivMainNetArrow.setOnDebounceClickListener {
            openSelectElectrumServer()
        }
        binding.tvMainNetHost.setOnDebounceClickListener {
            openSelectElectrumServer()
        }

        binding.exploreAddressSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.edtExploreAddressSigNetHost.isVisible = isChecked
            if (!isChecked) {
                handleSignetHostChangeCallback("")
                return@setOnCheckedChangeListener
            }

            binding.edtExploreAddressSigNetHost.text?.ifEmpty {
                binding.edtExploreAddressSigNetHost.setText(GLOBAL_SIGNET_EXPLORER)
            }
        }

        binding.edtExploreAddressSigNetHost.addTextChangedCallback {
            handleExplorerHostTextChange()
        }

        showGuideText()
    }

    private fun showGuideText() {
        val fullText = getString(R.string.nc_network_setting_guide)
        val clickableText = "this guide"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openExternalLink("https://github.com/nunchuk-io/resources/tree/main/docs/connection-guide")
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color =
                    ContextCompat.getColor(this@NetworkSettingActivity, R.color.nc_text_primary)
            }
        }

        val clickableTextStart = fullText.indexOf(clickableText)
        val clickableTextEnd = clickableTextStart + clickableText.length

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            clickableTextStart,
            clickableTextEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            UnderlineSpan(),
            clickableTextStart,
            clickableTextEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            clickableSpan,
            clickableTextStart,
            clickableTextEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.guideText.text = spannableString
        binding.guideText.movementMethod = LinkMovementMethod.getInstance()
        binding.guideText.highlightColor =
            ContextCompat.getColor(this@NetworkSettingActivity, android.R.color.transparent)
    }

    private fun handleExplorerHostTextChange() {
        setupViewsWhenAppSettingChanged()
    }

    private fun setupViewsWhenAppSettingChanged() {
        val isChangedSetting = isAppSettingChanged()
        binding.btnSave.isVisible = isChangedSetting
        binding.btnSaveDisable.isVisible = !isChangedSetting
    }

    private fun handleSignetHostChangeCallback(host: String) {
        viewModel.currentAppSettings?.copy(
            signetExplorerHost = host
        )?.let { appSetting ->
            viewModel.updateCurrentState(appSetting)
        }
    }

    private fun handleCheckboxChangeListener(chainId: Int) {
        val chain = when (chainId) {
            R.id.rbSigNet -> Chain.SIGNET
            R.id.rbTestNet -> Chain.TESTNET
            R.id.rbMainNet -> Chain.MAIN
            else -> Chain.TESTNET
        }

        viewModel.currentAppSettings?.copy(
            chain = chain,
            testnetServers = listOf(binding.tvTestNetHost.text.toString()),
            signetServers = listOf(binding.tvSigNetHost.text.toString())
        )?.let(viewModel::updateCurrentState)
    }

    private fun handleNetworkHostTextCallBack(text: String, chain: Chain) {
        when (chain) {
            Chain.SIGNET -> viewModel.currentAppSettings?.copy(
                signetServers = listOf(text)
            )

            Chain.TESTNET -> viewModel.currentAppSettings?.copy(
                testnetServers = listOf(text)
            )

            Chain.MAIN -> viewModel.currentAppSettings?.copy(
                mainnetServers = listOf(text)
            )

            else -> viewModel.currentAppSettings
        }?.let(viewModel::updateCurrentState)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    NetworkSettingActivity::class.java
                )
            )
        }
    }
}