package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class AddInheritanceKeyRoute(
    val index: Int,
    val totalKeys: Int,
    val isShowKeyAdded: Boolean = false,
)

fun NavGraphBuilder.addInheritanceKey(
    onBackPressed: () -> Unit = {},
    onAddKeyClick: () -> Unit = {},
) {
    composable<AddInheritanceKeyRoute> {
        val args = it.toRoute<AddInheritanceKeyRoute>()
        AddInheritanceKeyScreen(
            isShowKeyAdded = args.isShowKeyAdded,
            isFirstKey = args.index == 1,
            totalKeys = args.totalKeys,
            onBackPressed = onBackPressed,
            onAddKeyClick = onAddKeyClick,
        )
    }
}

fun NavController.navigateToAddInheritanceKey(
    index: Int, totalKeys: Int, isShowKeyAdded: Boolean = false
) {
    navigate(AddInheritanceKeyRoute(index, totalKeys, isShowKeyAdded))
}
