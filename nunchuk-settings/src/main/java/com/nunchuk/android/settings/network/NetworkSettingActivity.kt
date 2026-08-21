package com.nunchuk.android.settings.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.system.exitProcess

@Serializable
sealed class NetworkSettingScreens {
    @Serializable
    data object NetworkSetting : NetworkSettingScreens()

    @Serializable
    data object CustomExplorer : NetworkSettingScreens()

    @Serializable
    data object ProxySetting : NetworkSettingScreens()
}

@AndroidEntryPoint
class NetworkSettingActivity : BaseComposeActivity(), OnNetworkSettingMoreClickListener {
    private val showScreenEvent = MutableSharedFlow<NetworkSettingScreens>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                showScreenEvent.collect { screen ->
                    when (screen) {
                        is NetworkSettingScreens.NetworkSetting -> {
                            navController.navigate(NetworkSettingScreens.NetworkSetting)
                        }

                        is NetworkSettingScreens.CustomExplorer -> {
                            navController.navigate(NetworkSettingScreens.CustomExplorer)
                        }

                        is NetworkSettingScreens.ProxySetting -> {
                            navController.navigate(NetworkSettingScreens.ProxySetting)
                        }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = NetworkSettingScreens.NetworkSetting
            ) {
                composable<NetworkSettingScreens.NetworkSetting> {
                    AndroidFragment(
                        NetworkSettingFragment::class.java,
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize()
                    )
                }
                composable<NetworkSettingScreens.CustomExplorer> {
                    CustomExplorerScreen()
                }
                composable<NetworkSettingScreens.ProxySetting> {
                    ProxySettingScreen(
                        onSignOutSuccess = {
                            exitApp()
                        },
                    )
                }
            }
        }
    }

    /**
     * The proxy settings only reach the native SDK through
     * [com.nunchuk.android.share.InitNunchukUseCase] in Application.onCreate, so the process has to
     * go away for a change to take effect. Removing the task first stops the system from restoring
     * this activity, and exitProcess(0) is a normal exit - no crash dialog, no Crashlytics report -
     * so the next launcher tap is a clean cold start.
     */
    private fun exitApp() {
        finishAndRemoveTask()
        runCatching {
            Handler(Looper.getMainLooper()).postDelayed({
                exitProcess(0)
            }, EXIT_DELAY_MS)
        }
    }

    override fun onCustomExplorerClick() {
        lifecycleScope.launch {
            showScreenEvent.emit(NetworkSettingScreens.CustomExplorer)
        }
    }

    override fun onProxySettingClick() {
        lifecycleScope.launch {
            showScreenEvent.emit(NetworkSettingScreens.ProxySetting)
        }
    }

    companion object {
        private const val EXIT_DELAY_MS = 300L

        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    NetworkSettingActivity::class.java
                )
            )
        }
    }
}
