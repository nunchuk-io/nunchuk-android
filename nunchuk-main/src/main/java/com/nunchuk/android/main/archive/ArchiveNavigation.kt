package com.nunchuk.android.main.archive

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object Archive

fun NavGraphBuilder.archiveScreen(
    openWalletDetail: (String) -> Unit = {},
) {
    composable<Archive> {
        ArchiveRoute(openWalletDetail = openWalletDetail)
    }
}