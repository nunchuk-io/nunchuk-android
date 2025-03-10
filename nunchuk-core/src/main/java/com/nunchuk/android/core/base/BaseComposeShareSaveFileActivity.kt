package com.nunchuk.android.core.base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.widget.NCToastMessage

abstract class BaseComposeShareSaveFileActivity: BaseComposeActivity(),
    BottomSheetOptionListener {

    protected val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveFileToLocal()
        }
    }

    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                saveFileToLocal()
            }
        } else {
            saveFileToLocal()
        }
    }

    protected fun showSaveShareOption() {
        BottomSheetOption.newInstance(
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_SAVE_FILE,
                    stringId = R.string.nc_save_file
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_SHARE_FILE,
                    stringId = R.string.nc_share_file
                )
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_SAVE_FILE -> checkAndRequestPermission()
            SheetOptionType.TYPE_SHARE_FILE -> shareFile()
            else -> {}
        }
    }

    protected fun showSaveFileState(isSuccess: Boolean) {
        hideLoading()
        if (isSuccess) {
            NCToastMessage(this).showMessage(getString(R.string.nc_save_file_success))
        } else {
            NCToastMessage(this).showError(getString(R.string.nc_save_file_failed))
        }
    }

    open fun shareFile() {}

    open fun saveFileToLocal() {}
}