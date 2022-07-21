package com.nunchuk.android.signer.satscard

import android.app.Activity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivitySatsCardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SatsCardActivity : BaseActivity<ActivitySatsCardBinding>() {
    override fun initializeBinding(): ActivitySatsCardBinding {
        return ActivitySatsCardBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHost.findNavController().setGraph(R.navigation.satscard_navigation, intent.extras)
    }

    companion object {
        fun navigate(activity: Activity, status: SatsCardStatus) {
            activity.startActivity(SatsCardArgs(status).buildIntent(activity))
        }
    }
}