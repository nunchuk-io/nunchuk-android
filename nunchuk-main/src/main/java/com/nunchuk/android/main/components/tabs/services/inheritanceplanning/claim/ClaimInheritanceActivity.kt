package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.AddInheritanceKeyRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.addInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.InheritanceOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToPrepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToRecoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.prepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.recoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.navigateToRestoreSeedPhraseHardware
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.restoreSeedPhraseHardware
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClaimInheritanceActivity : BaseComposeActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContentView(
            ComposeView(this).apply {
                setContent {
                    ClaimInheritanceGraph()
                }
            }
        )
    }
}

@Composable
private fun ClaimInheritanceGraph() {
    val navController = rememberNavController()
    NunchukTheme {
        NavHost(
            navController = navController,
            startDestination = AddInheritanceKeyRoute
        ) {
            addInheritanceKey(
                onBackPressed = {
                    navController.popBackStack()
                },
                onAddKeyClick = {
                    navController.navigateToPrepareInheritanceKey()
                },
            )
            prepareInheritanceKey(
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = { option ->
                    when (option) {
                        InheritanceOption.HARDWARE_DEVICE -> {

                        }
                        InheritanceOption.SEED_PHRASE -> {
                            navController.navigateToRecoverInheritanceKey()
                        }
                    }
                },
            )
            recoverInheritanceKey(
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = { isUseDevice ->
                    if (isUseDevice) {
                        navController.navigateToRestoreSeedPhraseHardware()
                    } else {

                    }
                },
            )
            restoreSeedPhraseHardware(
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = {
                    // TODO: Handle continue action
                },
            )
        }
    }
}
