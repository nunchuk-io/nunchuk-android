package com.nunchuk.android.core.signer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.R

/**
 * Device-intro copy for a single-account inheritance key (Figma KEY-05c primary, KEY-05b fallback):
 * names the route and the [account] to select. `[B]…[/B]` marks bold for `NcHighlightText`.
 */
@Composable
fun singleAccountRouteDescription(route: OnChainInheritanceRoute, account: Int): String =
    if (route.fallbackNumber > 0) {
        stringResource(R.string.nc_onchain_add_fallback_key_desc, route.fallbackNumber, account)
    } else {
        stringResource(R.string.nc_onchain_add_primary_inheritance_key_desc, account)
    }
