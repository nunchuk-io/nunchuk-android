package com.nunchuk.android.settings.walletsecurity.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.UnlockPinSourceFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnlockPinActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(
                    android.R.id.content,
                    UnlockPinFragment().apply {
                        arguments = UnlockPinFragmentArgs(isRemovePin = false,
                            sourceFlow = intent.getIntExtra(EXTRA_SOURCE_FLOW, UnlockPinSourceFlow.NONE)
                        ).toBundle()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SOURCE_FLOW = "source_flow"
        fun navigate(activityContext: Context, @UnlockPinSourceFlow.UnlockPinSourceFlowInfo sourceFlow: Int) {
            val intent = Intent(activityContext, UnlockPinActivity::class.java)
            intent.putExtra(EXTRA_SOURCE_FLOW, sourceFlow)
            activityContext.startActivity(intent)
        }
    }
}