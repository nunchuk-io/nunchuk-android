package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.InheritanceRoute
import com.nunchuk.android.model.inheritance.InheritanceRouteType
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryInheritanceInfoTest {

    private fun route(type: InheritanceRouteType, order: Int, m: Int, vararg xfps: String) = InheritanceRoute(
        type = type,
        order = order,
        requiredSignatures = m,
        xfps = xfps.toList(),
        timelock = WalletTimelock(),
    )

    @Test
    fun `guided default lists only the tagged inheritance key and counts the owner keys`() {
        val policy = OnChainInheritancePolicy(
            isCustomized = false,
            routes = listOf(route(InheritanceRouteType.PRIMARY, 0, 2, "owner1", "inh", "owner2", "owner3")),
        )

        val info = policy.toPrimaryInheritanceInfo(inheritanceTaggedXfps = setOf("inh"))!!

        assertEquals(listOf("inh"), info.inheritanceKeyXfps)
        assertEquals(2, info.requiredSignatures)
        assertEquals(4, info.totalKeys)
        assertEquals(3, info.ownerKeyCount)
    }

    @Test
    fun `guided default never guesses the inheritance key when no route key is tagged`() {
        val policy = OnChainInheritancePolicy(
            isCustomized = false,
            routes = listOf(route(InheritanceRouteType.PRIMARY, 0, 2, "owner1", "inh", "owner2", "owner3")),
        )

        assertNull(policy.toPrimaryInheritanceInfo(inheritanceTaggedXfps = emptySet()))
        assertNull(policy.toPrimaryInheritanceInfo(inheritanceTaggedXfps = setOf("not-in-route")))
    }

    @Test
    fun `customized route lists every key and has no owner keys`() {
        val policy = OnChainInheritancePolicy(
            isCustomized = true,
            routes = listOf(route(InheritanceRouteType.PRIMARY, 0, 2, "a", "b", "c")),
        )

        val info = policy.toPrimaryInheritanceInfo(inheritanceTaggedXfps = setOf("a"))!!

        assertEquals(listOf("a", "b", "c"), info.inheritanceKeyXfps)
        assertEquals(0, info.ownerKeyCount)
    }

    @Test
    fun `customized 1 of 1 is a single key`() {
        val policy = OnChainInheritancePolicy(
            isCustomized = true,
            routes = listOf(route(InheritanceRouteType.PRIMARY, 0, 1, "a")),
        )

        assertTrue(policy.toPrimaryInheritanceInfo(emptySet())!!.isSingleKey)
    }

    @Test
    fun `no primary route means a legacy plan`() {
        val policy = OnChainInheritancePolicy(
            isCustomized = false,
            routes = listOf(route(InheritanceRouteType.FALLBACK, 1, 1, "f")),
        )

        assertNull(policy.toPrimaryInheritanceInfo(emptySet()))
    }

    @Test
    fun `fallbacks are ordered and the primary is found regardless of position`() {
        val policy = OnChainInheritancePolicy(
            isCustomized = true,
            routes = listOf(
                route(InheritanceRouteType.FALLBACK, 2, 1, "f2"),
                route(InheritanceRouteType.PRIMARY, 0, 1, "p"),
                route(InheritanceRouteType.FALLBACK, 1, 1, "f1"),
            ),
        )

        assertEquals(listOf("p"), policy.primary!!.xfps)
        assertEquals(listOf(1, 2), policy.fallbacks.map { it.order })
    }
}
