package com.nunchuk.android.wallet.upload

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

class UploadConfigurationArgs : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, UploadConfigurationActivity::class.java).apply {

    }

    companion object {

        fun deserializeFrom(intent: Intent): UploadConfigurationArgs = UploadConfigurationArgs(

        )
    }
}