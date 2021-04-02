package com.nunchuk.android.app.splash

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.core.util.isPermissionGranted
import com.nunchuk.android.core.util.observe
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

internal class SplashActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SplashViewModel by lazy {
        ViewModelProviders.of(this, factory).get(SplashViewModel::class.java)
    }
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subscribeEvents()
    }

    override fun onResume() {
        super.onResume()
        requestPermissions()
    }

    private fun subscribeEvents() {
        val activityContext = this
        viewModel.event.observe(owner = this) {
            finish()
            when (this) {
                NavCreateAccountEvent -> navigator.openIntroScreen(activityContext)
                NavActivateAccountEvent -> navigator.openChangePasswordScreen(activityContext)
                NavSignInEvent -> navigator.openSignInScreen(activityContext)
                NavHomeScreenEvent -> navigator.openMainScreen(activityContext)
                is InitErrorEvent -> showToast(error ?: "Internal error")
            }
        }
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
                viewModel.initFlow()
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

