package com.nunchuk.android.wallet.components.alias

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val walletIdArgument = "walletId"
const val setAliasRoute = "set_alias/{$walletIdArgument}"

fun NavGraphBuilder.setAlias(
    walletId: String,
    onBackPress : () -> Unit = {},
) {
    composable(
        route = setAliasRoute,
        arguments = listOf(
            navArgument(
                name = walletIdArgument
            ) {
                type = NavType.StringType
                defaultValue = walletId
            }
        )
    ) {
        SetAliasRoute(
            onBackPress = onBackPress
        )
    }
}