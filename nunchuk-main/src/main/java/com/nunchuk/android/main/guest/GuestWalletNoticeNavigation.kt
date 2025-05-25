package com.nunchuk.android.main.guest

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object GuestWalletNotice

fun NavGraphBuilder.guestWalletNoticeScreen(
    onGotIt: () -> Unit = {},
) {
    composable<GuestWalletNotice> {
        GuestWalletNoticeRoute(onGotIt = onGotIt)
    }
} 