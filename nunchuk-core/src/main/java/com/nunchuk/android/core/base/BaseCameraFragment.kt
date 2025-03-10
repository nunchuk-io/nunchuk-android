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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.widget.NCWarningDialog

abstract class BaseCameraFragment<out Binding : ViewBinding> : BaseFragment<Binding>() {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onCameraPermissionGranted(true)
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showAlertPermissionNotGranted()
            } else {
                showAlertPermissionDeniedPermanently()
            }
        }

    private val settingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (requireActivity().isPermissionGranted(Manifest.permission.CAMERA)) {
            onCameraPermissionGranted(true)
        } else {
            showAlertPermissionDeniedPermanently()
        }
    }

    private fun showAlertPermissionNotGranted() {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied),
            onYesClick = {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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

    protected fun requestCameraPermissionOrExecuteAction() {
        if (requestPermissionLauncher.checkCameraPermission(requireActivity())) {
            onCameraPermissionGranted(false)
        }
    }

    private fun Activity.isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun ActivityResultLauncher<String>.checkCameraPermission(activity: Activity): Boolean {
        val isGranted = activity.isPermissionGranted(Manifest.permission.CAMERA)
        if (!isGranted) {
            launch(Manifest.permission.CAMERA)
        }
        return isGranted
    }

    abstract fun onCameraPermissionGranted(fromUser: Boolean)

    private fun handleSelectPhoto() {
        selectPhotoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private val selectPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                decodeQRCodeFromUri(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnSelectPhoto()?.setOnClickListener {
            handleSelectPhoto()
        }
        btnTurnFlash()?.setOnClickListener {
            toggleFlash()
        }
    }

    open fun btnSelectPhoto(): ImageView? = null

    open fun btnTurnFlash(): ImageView? = null

    open fun decodeQRCodeFromUri(uri: Uri) {}

    private var isFlashOn = false

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        if (isFlashOn) {
            torchState(true)
            btnTurnFlash()?.setImageResource(R.drawable.nc_ic_flash_off)
        } else {
            torchState(false)
            btnTurnFlash()?.setImageResource(R.drawable.nc_ic_flash_on)
        }
    }

    open fun torchState(isOn: Boolean) {}
}