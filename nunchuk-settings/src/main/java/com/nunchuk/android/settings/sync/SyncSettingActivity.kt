/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.settings.sync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.domain.data.SyncSetting
import com.nunchuk.android.settings.databinding.ActivitySyncSettingBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncSettingActivity : BaseActivity<ActivitySyncSettingBinding>() {

    private val viewModel: SyncSettingViewModel by viewModels()

    override fun initializeBinding() = ActivitySyncSettingBinding.inflate(layoutInflater)

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

    private fun handleState(state: SyncSettingState) {
        binding.switchSyncMode.isChecked = state.syncSetting.enable
    }

    private fun handleEvent(event: SyncSettingEvent) {
        when(event) {
            is SyncSettingEvent.UpdateSyncSettingSuccessEvent -> {
                viewModel.enableAutoBackup(event.enable)
            }
            is SyncSettingEvent.GetSyncSettingSuccessEvent -> {
                viewModel.enableAutoBackup(event.enable)
            }
            SyncSettingEvent.EnableAutoUpdateSuccessEvent -> {
                viewModel.backupData()
            }
        }
    }

    private fun setupViews() {
        binding.switchSyncMode.setOnCheckedChangeListener { _, checked ->
            updateDisplayUnitSetting(
                enable = checked
            )
        }
    }

    private fun updateDisplayUnitSetting(
        enable: Boolean
    ) {
        viewModel.updateSyncSettings(
            syncSetting = SyncSetting(
                enable = enable
            )
        )
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SyncSettingActivity::class.java))
        }
    }
}