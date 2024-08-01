package com.nunchuk.android.signer.portal.intro

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow

const val portalIntroRoute = "portal_intro_route"

fun NavGraphBuilder.portalIntro(
    snackState: SnackbarHostState = SnackbarHostState(),
    args: PortalDeviceArgs,
    onScanPortalClicked: () -> Unit = {},
) {
    composable(portalIntroRoute) {
        if (args.type == PortalDeviceFlow.RESCAN) {
            PortalRescanScreen(
                args = args,
                snackState = snackState,
                onScanPortalClicked = onScanPortalClicked,

            )
        } else {
            PortalIntroScreen(
                onScanPortalClicked = onScanPortalClicked,
                snackState = snackState,
                flow = args.type,
            )
        }
    }
}
