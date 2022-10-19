package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.CAMERA_PERMISSION_REQUEST_CODE
import com.nunchuk.android.core.util.checkCameraPermission
import com.nunchuk.android.signer.airgap.databinding.ActivityBeforeAddAirSignerBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AirSignerIntroActivity : BaseActivity<ActivityBeforeAddAirSignerBinding>() {

    override fun initializeBinding() = ActivityBeforeAddAirSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (checkCameraPermission()) {
                navigator.openAddAirSignerScreen(this)
            }
        }
    }

    private fun setupViews() {
        binding.btnContinue.setOnClickListener {
            if (checkCameraPermission()) {
                navigator.openAddAirSignerScreen(this)
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AirSignerIntroActivity::class.java))
        }
    }

}