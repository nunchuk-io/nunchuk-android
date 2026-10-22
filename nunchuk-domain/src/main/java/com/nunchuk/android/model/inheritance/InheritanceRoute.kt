package com.nunchuk.android.model.inheritance

import com.nunchuk.android.model.WalletTimelock

/**
 * A way to claim an on-chain (miniscript) inheritance: any [requiredSignatures] of [xfps] once
 * [timelock] has passed. Routes are independent — a claim satisfies one of them completely.
 *
 * Named "route" rather than "fallback" on purpose: [InheritancePlanFallbackPolicy] and the
 * BSMS "fallback option" already use that word for unrelated things.
 */
data class InheritanceRoute(
    val type: InheritanceRouteType,
    /** 0 for the primary route; 1, 2, … for Fallback 1, Fallback 2, … */
    val order: Int,
    val requiredSignatures: Int,
    val xfps: List<String>,
    val timelock: WalletTimelock,
) {
    val totalKeys: Int
        get() = xfps.size
}

enum class InheritanceRouteType {
    PRIMARY,
    FALLBACK,
}

/**
 * The inheritance routes of an on-chain wallet as the server reports them.
 *
 * [isCustomized] is false for the guided default: primary inheritance is any 2 of the 3 owner keys
 * plus one separate inheritance key.
 */
data class OnChainInheritancePolicy(
    val isCustomized: Boolean,
    val routes: List<InheritanceRoute>,
) {
    val primary: InheritanceRoute?
        get() = routes.firstOrNull { it.type == InheritanceRouteType.PRIMARY }

    val fallbacks: List<InheritanceRoute>
        get() = routes.filter { it.type == InheritanceRouteType.FALLBACK }.sortedBy { it.order }
}
