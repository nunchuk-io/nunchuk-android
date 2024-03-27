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

package com.nunchuk.android.settings.developer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.domain.data.DeveloperSetting
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.log.FileLogTree
import com.nunchuk.android.settings.databinding.ActivityDeveloperSettingBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class DeveloperSettingActivity : BaseActivity<ActivityDeveloperSettingBinding>() {

    private val viewModel: DeveloperSettingViewModel by viewModels()

    override fun initializeBinding() = ActivityDeveloperSettingBinding.inflate(layoutInflater)

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

    private fun handleState(state: DeveloperSettingState) {
        binding.switchDebugMode.isChecked = state.developerSetting.debugMode
        binding.switchCollaborativeWallet.isChecked = state.developerSetting.matrixBasedCollaborativeWallet
    }

    private fun handleEvent(event: DeveloperSettingEvent) {
        when (event) {
            is DeveloperSettingEvent.UpdateSuccessEvent -> {
                // currently we do nothing
            }
        }
    }

    private fun setupViews() {
        binding.switchDebugMode.setOnCheckedChangeListener { _, checked ->
            updateSetting(debugMode = checked)
        }
        binding.switchCollaborativeWallet.setOnCheckedChangeListener { _, checked ->
            updateSetting(matrixBasedCollaborativeWallet = checked)

        }
        binding.btnClearLog.setOnClickListener {
            try {
                FileLogTree.getLogFile(this).outputStream()
                    .use { it.write("".toByteArray(Charsets.UTF_8)) }
            } catch (e: Exception) {
            }
        }
        binding.btnShareLog.setOnClickListener {
            IntentSharingController.from(this).shareFile(FileLogTree.getLogFile(this).absolutePath)
        }
    }

    private fun updateSetting(
        debugMode: Boolean? = null,
        matrixBasedCollaborativeWallet: Boolean? = null,
    ) {
        debugMode?.let { viewModel.updateDebugMode(it) }
        matrixBasedCollaborativeWallet?.let { viewModel.updateMatrixBasedCollaborativeWallet(it) }
    }

    private fun setupData() {
        viewModel.getDeveloperSettings()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    DeveloperSettingActivity::class.java
                )
            )
        }
    }
}