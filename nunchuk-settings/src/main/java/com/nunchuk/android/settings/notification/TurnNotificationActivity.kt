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

package com.nunchuk.android.settings.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.settings.databinding.ActivityTurnNotificationBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TurnNotificationActivity : BaseActivity<ActivityTurnNotificationBinding>() {

    private val args: TurnNotificationArgs by lazy {
        TurnNotificationArgs.deserializeFrom(
            intent
        )
    }


    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        openMainScreen()
    }

    private val appSettingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        openMainScreen()
    }

    override fun initializeBinding() = ActivityTurnNotificationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        showMessage()
    }

    private fun openMainScreen() {
        finish()
        navigator.openMainScreen(
            this,
            isClearTask = true
        )
    }

    private fun showMessage() {
        args.messages.forEachIndexed { index, message ->
            NCToastMessage(this@TurnNotificationActivity).showMessage(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                intent.action = APP_NOTIFICATION_SETTINGS
            }
            intent.putExtra(APP_PACKAGE, packageName)
            intent.putExtra(APP_UID, applicationInfo.uid)
            appSettingLauncher.launch(intent)
        }
    }

    companion object {
        private const val DISMISS_TIME = 2000L

        private const val APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
        private const val APP_PACKAGE = "app_package"
        private const val APP_UID = "app_uid"
        fun start(
            activityContext: Context,
            messages: ArrayList<String>
        ) {
            activityContext.startActivity(
                TurnNotificationArgs(
                    messages = messages
                ).buildIntent(
                    activityContext
                )
            )
        }
    }
}