package com.nunchuk.android.wallet.personal.components

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.isPermissionGranted
import com.nunchuk.android.core.util.showAlertDialog
import com.nunchuk.android.core.util.startActivityAppSetting
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.wallet.personal.databinding.ActivityWalletIntermediaryBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import java.io.File

class WalletIntermediaryActivity : BaseActivity<ActivityWalletIntermediaryBinding>() {

    override fun initializeBinding() = ActivityWalletIntermediaryBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun openCreateNewWalletScreen() {
        navigator.openAddWalletScreen(this)
    }

    private fun openRecoverWalletScreen() {
        val recoverWalletBottomSheet = RecoverWalletActionBottomSheet.show(supportFragmentManager)
        recoverWalletBottomSheet.listener = {
            when(it) {
                RecoverWalletOption.QrCode -> handleOptionUsingQRCode()
                RecoverWalletOption.BSMSFile -> openSelectFileChooser()
            }
        }
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(this)
    }

    private fun openSelectFileChooser() {
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.nc_txt_file_bsms_chooser)), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val file = intent?.data?.let {
                getFileFromUri(contentResolver, it, cacheDir)
            }

            file?.absolutePath?.let {
                navigator.openAddRecoverWalletScreen(this, it)
            }
        }
    }

    private fun getFileFromUri(contentResolver: ContentResolver, uri: Uri, directory: File): File {
        val file =
            File.createTempFile("NCsuffix", ".prefixNC", directory)
        file.outputStream().use {
            contentResolver.openInputStream(uri)?.copyTo(it)
        }
        return file
    }

    private fun setupViews() {
        binding.btnCreateNewWallet.setOnClickListener {
            openCreateNewWalletScreen()
        }
        binding.btnRecoverWallet.setOnClickListener {
            openRecoverWalletScreen()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }


    private fun handleOptionUsingQRCode() {
        if (isPermissionGranted(Manifest.permission.CAMERA)) {
            openScanQRCodeScreen()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION_CAMERA)
        }
    }


    // TODO: refactor with registerForActivityResult later
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePermissionGranted()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showAlertPermissionNotGranted(requestCode)
            } else {
                showAlertPermissionDeniedPermanently()
            }
        }
    }

    private fun handlePermissionGranted() {
        openScanQRCodeScreen()
    }


    private fun showAlertPermissionNotGranted(permissionCode: Int) {
        showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                handleOptionUsingQRCode()
            },
            negativeClick = {
            }
        )
    }

    private fun showAlertPermissionDeniedPermanently() {
        showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied_permanently),
            message = getString(R.string.nc_text_des_permission_denied_permanently),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                startActivityAppSetting()
            },
            negativeClick = {
            }
        )
    }


    companion object {
        private const val REQUEST_CODE = 1111
        private const val REQUEST_PERMISSION_CAMERA = 1112
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, WalletIntermediaryActivity::class.java))
        }
    }

}