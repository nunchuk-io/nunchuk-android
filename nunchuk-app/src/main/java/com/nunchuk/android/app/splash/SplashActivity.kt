package com.nunchuk.android.app.splash

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.nunchuk.android.app.util.FileUtil
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class SplashActivity : BaseActivity() {

    @Inject
    lateinit var createSignerUseCase: CreateSignerUseCase

    @Inject
    lateinit var remoteSignerUseCase: GetRemoteSignerUseCase

    private lateinit var binding: ActivitySplashBinding

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            !isPermissionGranted(INTERNET) -> {
                ActivityCompat.requestPermissions(this, arrayOf(INTERNET), REQUEST_PERMISSION_CODE)
            }
            else -> {
                retrieveData()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            requestPermissions()
        }
    }

    private fun retrieveData() {
        FileUtil.createNunchukRootDir()
        createRemoteSigner()
    }

    @SuppressLint("CheckResult")
    private fun retrieveRemoteSigner() {
        remoteSignerUseCase.execute()
                .delay(2, TimeUnit.SECONDS).defaultSchedulers().subscribe({
                    binding.signerInfo.text = "Signer Info ${gson.toJson(it)}"
                }, {
                    binding.signerInfo.text = "Signer retrieve error ${it.message}"
                })
    }

    @SuppressLint("CheckResult")
    private fun createRemoteSigner() {
        createSignerUseCase.execute(
                "TESTER",
                "xpub6Gs9Gp1P7ov2Xy6XmVBawLUwRgifGMK93K6bYuMdi9PfmJ6y6e7ffzD7JKCjWgJn71YGCQMozL1284Ywoaptv8UGRsua635k8yELEKk9nhh",
                "0297da76f2b4ae426f41e617b4f13243716d1417d3acc3f8da7a54f301fc951741",
                "m/48'/0'/0'/7",
                "0b93c52e"
        )
                .defaultSchedulers()
                .doAfterTerminate(::retrieveRemoteSigner)
                .subscribe({
                    binding.signerInfo.text = "Signer Info ${gson.toJson(it)}"
                }, {
                    binding.signerInfo.text = "Signer retrieve error ${it.message}"
                })
    }

    private fun isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1248
        private const val TAG = "SplashActivity"
    }
}

