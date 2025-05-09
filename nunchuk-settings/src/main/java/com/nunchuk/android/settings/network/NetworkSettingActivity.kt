package com.nunchuk.android.settings.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

@Serializable
sealed class NetworkSettingScreens {
    @Serializable
    data object NetworkSetting : NetworkSettingScreens()

    @Serializable
    data object CustomExplorer : NetworkSettingScreens()
}

@AndroidEntryPoint
class NetworkSettingActivity : BaseComposeActivity(), OnCustomExplorerClickListener {
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
            }
        }
    }

    override fun onCustomExplorerClick() {
        lifecycleScope.launch {
            showScreenEvent.emit(NetworkSettingScreens.CustomExplorer)
        }
    }

    companion object {
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
