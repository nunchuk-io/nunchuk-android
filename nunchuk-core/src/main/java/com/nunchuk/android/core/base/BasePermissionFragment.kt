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

package com.nunchuk.android.core.base

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.widget.NCWarningDialog

abstract class BasePermissionFragment<out Binding : ViewBinding> : BaseFragment<Binding>() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGrantedMap: Map<String, Boolean> ->
        if (isGrantedMap.values.all { it }) {
            onPermissionGranted(true)
        } else if (isGrantedMap.any { shouldShowRequestPermissionRationale(it.key) }) {
            showAlertPermissionNotGranted()
        } else {
            showAlertPermissionDeniedPermanently()
        }
    }

    private val settingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (checkPermissions(requireActivity())) {
                onPermissionGranted(true)
            } else {
                showAlertPermissionDeniedPermanently()
            }
        }

    private fun showAlertPermissionNotGranted() {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied),
            onYesClick = {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            },
        )
    }

    private fun showAlertPermissionDeniedPermanently() {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied_permanently),
            onYesClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                }
                settingLauncher.launch(intent)
            },
        )
    }

    protected fun requestPermissionOrExecuteAction() {
        if (checkPermissions(requireActivity())) {
            onPermissionGranted(false)
        }
    }

    private fun Activity.isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun checkPermissions(activity: Activity): Boolean {
        permissions.all { activity.isPermissionGranted(it) }
        val isGranted = permissions.all { activity.isPermissionGranted(it) }
        if (!isGranted) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
        return isGranted
    }

    abstract fun onPermissionGranted(fromUser: Boolean)

    abstract val permissions: List<String>
}