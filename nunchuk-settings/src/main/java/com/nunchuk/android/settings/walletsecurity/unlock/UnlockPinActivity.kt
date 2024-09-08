package com.nunchuk.android.settings.walletsecurity.unlock

import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnlockPinActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, UnlockPinFragment())
            }
        }
    }
}