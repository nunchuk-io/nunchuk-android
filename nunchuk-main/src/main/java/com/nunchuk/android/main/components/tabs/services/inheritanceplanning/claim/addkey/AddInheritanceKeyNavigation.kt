package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimData
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.ClaimMagicPhraseRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToPrepareInheritanceKey
import kotlinx.serialization.Serializable

@Serializable
data class AddInheritanceKeyRoute(
    val index: Int,
    val totalKeys: Int,
)

fun NavGraphBuilder.addInheritanceKey(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onAddKeyClick: () -> Unit = {},
) {
    composable<AddInheritanceKeyRoute> {
        val args = it.toRoute<AddInheritanceKeyRoute>()
        AddInheritanceKeyScreen(
            snackState = snackState,
            index = args.index,
            totalKeys = args.totalKeys,
            onBackPressed = onBackPressed,
            onAddKeyClick = onAddKeyClick,
        )
    }
}

/**
 * Opens the add-key screen for key [index] of [totalKeys]. Earlier claim screens are cleared back to
 * Magic Phrase unless [keepBackStack] is set, which keeps the current screen (CLM-KCa/KCb) one back.
 */
fun NavController.navigateToAddInheritanceKey(
    index: Int, totalKeys: Int, keepBackStack: Boolean = false,
) {
    navigate(
        AddInheritanceKeyRoute(index, totalKeys),
        navOptions {
            if (!keepBackStack) {
                popUpTo<ClaimMagicPhraseRoute> {
                    inclusive = false
                }
            }
        }
    )
}

/** Starts collecting keys: the "requires N keys" intro for several keys, else straight to the first key. */
fun NavController.navigateToFirstInheritanceKey(requiredKeyCount: Int, keepBackStack: Boolean = false) {
    if (requiredKeyCount > 1) {
        navigateToAddInheritanceKey(index = 1, totalKeys = requiredKeyCount, keepBackStack = keepBackStack)
    } else {
        navigateToPrepareInheritanceKey()
    }
}

/** Asks for the key after those already added; accepted keys stay in [claimData]. */
fun NavController.navigateToNextInheritanceKey(claimData: ClaimData) {
    navigateToAddInheritanceKey(index = claimData.addedKeyCount + 1, totalKeys = claimData.requiredKeyCount)
}
