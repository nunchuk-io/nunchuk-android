package com.nunchuk.android.arch.args

import android.content.Context
import android.content.Intent
import android.os.Bundle

interface ActivityArgs {
    fun buildIntent(activityContext: Context): Intent
}

interface FragmentArgs {
    fun buildBundle(): Bundle
}