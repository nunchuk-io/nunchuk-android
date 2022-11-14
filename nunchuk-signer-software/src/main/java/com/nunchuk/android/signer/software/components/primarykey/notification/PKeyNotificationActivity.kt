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

package com.nunchuk.android.signer.software.components.primarykey.notification

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.databinding.ActivityPkeyNotificationBinding
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PKeyNotificationActivity : BaseActivity<ActivityPkeyNotificationBinding>() {

    private val args: PKeyNotificationArgs by lazy {
        PKeyNotificationArgs.deserializeFrom(
            intent
        )
    }

    private var isNotificationSettingOpened = false

    override fun initializeBinding() = ActivityPkeyNotificationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        showAlert()
    }

    override fun onResume() {
        super.onResume()
        if (isNotificationSettingOpened) {
            openMainScreen()
        }
    }

    private fun openMainScreen() {
        navigator.openMainScreen(
            this,
            accountManager.getAccount().token,
            accountManager.getAccount().deviceId,
            isClearTask = true
        )
        finish()
    }

    private fun showAlert() {
        args.messages.forEachIndexed { index, message ->
            NCToastMessage(this@PKeyNotificationActivity).showMessage(
                message = message, dismissTime = (index + 1) * DISMISS_TIME
            )
        }
    }

    private fun setupViews() {
        binding.btnNotNow.setOnDebounceClickListener {
            openMainScreen()
        }
        binding.btnTurnOnNotification.setOnDebounceClickListener {
            openNotificationSetting()
        }
    }

    private fun openNotificationSetting() {
        isNotificationSettingOpened = true
        NotificationUtils.openNotificationSettings(this)
    }

    companion object {
        private const val DISMISS_TIME = 2000L
        fun start(
            activityContext: Context,
            messages: ArrayList<String>,
            primaryKeyFlow: Int,
        ) {
            activityContext.startActivity(
                PKeyNotificationArgs(
                    messages = messages,
                    primaryKeyFlow = primaryKeyFlow,
                ).buildIntent(
                    activityContext
                )
            )
        }
    }
}