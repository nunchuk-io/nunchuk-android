package com.nunchuk.android.app.splash

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.core.app.ActivityCompat
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.core.util.FileUtil
import com.nunchuk.android.core.util.isPermissionGranted
import com.nunchuk.android.databinding.ActivitySplashBinding

internal class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set Transparent Status Bar
        window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
    }

    private fun requestPermissions() {
        when {
            !isPermissionGranted(READ_EXTERNAL_STORAGE) -> {
                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
            }
            !isPermissionGranted(WRITE_EXTERNAL_STORAGE) -> {
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
            }
            else -> {
                FileUtil.getOrCreateNunchukRootDir()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            requestPermissions()
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1248
    }
}

