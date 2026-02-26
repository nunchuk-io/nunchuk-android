package com.nunchuk.android.settings.walletsecurity.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.UnlockPinSourceFlow
import com.nunchuk.android.settings.walletsecurity.UnlockPinRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnlockPinActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sourceFlow = intent.getIntExtra(EXTRA_SOURCE_FLOW, UnlockPinSourceFlow.NONE)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    UnlockPinActivityNavHost(
                        activity = this@UnlockPinActivity,
                        navigator = navigator,
                        route = UnlockPinRoute(isRemovePin = false, sourceFlow = sourceFlow),
                    )
                }
            },
        )
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