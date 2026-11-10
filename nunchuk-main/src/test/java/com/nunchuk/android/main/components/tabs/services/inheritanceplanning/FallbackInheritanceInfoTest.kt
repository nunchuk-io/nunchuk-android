package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import com.nunchuk.android.model.InheritanceKey
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackInheritanceInfoTest {

    private fun policy(
        fallbacks: List<FallbackInheritanceCondition>,
        type: OnChainInheritancePolicyType = OnChainInheritancePolicyType.CONFIGURABLE,
    ) = OnChainInheritancePolicy(
        type = type,
        primary = PrimaryInheritanceCondition(m = 2, n = 4, isCustomized = false, timelock = null),
        fallbacks = fallbacks,
    )

    private fun fallbackKey(xfp: String, name: String, index: Int) =
        InheritanceKey(xfp = xfp, section = InheritanceSection.FALLBACK, name = name, fallbackIndex = index)

    @Test
    fun `each fallback gets only its own keys, in server order, with its own timelock`() {
        val t1 = WalletTimelock(timelockValue = 2_000L)
        val t2 = WalletTimelock(timelockValue = 3_000L)
        val infos = policy(
            listOf(
                FallbackInheritanceCondition(index = 1, m = 2, n = 3, timelock = t2),
                FallbackInheritanceCondition(index = 0, m = 1, n = 1, timelock = t1),
            )
        ).toFallbackInheritanceInfos(
            listOf(
                InheritanceKey(xfp = "p", section = InheritanceSection.PRIMARY, name = "Inheritance key 1"),
                fallbackKey("a", "Fallback key 1", 0),
                fallbackKey("b", "Fallback key 2", 1),
                fallbackKey("c", "Fallback key 3", 1),
                fallbackKey("d", "Fallback key 4", 1),
            )
        )

        assertEquals(listOf(1, 2), infos.map { it.number })
        assertEquals(listOf("a"), infos[0].keys.map { it.xfp })
        assertTrue(infos[0].isSingleKey)
        assertEquals(t1, infos[0].timelock)
        assertEquals(listOf("Fallback key 2", "Fallback key 3", "Fallback key 4"), infos[1].keys.map { it.name })
        assertEquals(2, infos[1].requiredSignatures)
        assertEquals(3, infos[1].totalKeys)
        assertEquals(t2, infos[1].timelock)
    }

    @Test
    fun `a key reused by another condition is listed under each one`() {
        val infos = policy(
            listOf(
                FallbackInheritanceCondition(index = 0, m = 1, n = 1, timelock = null),
                FallbackInheritanceCondition(index = 1, m = 1, n = 1, timelock = null),
            )
        ).toFallbackInheritanceInfos(
            listOf(fallbackKey("a", "Fallback key 1", 0), fallbackKey("a", "Fallback key 2", 1))
        )

        assertEquals(listOf("a"), infos[0].keys.map { it.xfp })
        assertEquals(listOf("a"), infos[1].keys.map { it.xfp })
    }

    @Test
    fun `legacy plan has no fallbacks`() {
        val infos = policy(
            listOf(FallbackInheritanceCondition(index = 0, m = 1, n = 1, timelock = null)),
            type = OnChainInheritancePolicyType.LEGACY,
        ).toFallbackInheritanceInfos(listOf(fallbackKey("a", "Fallback key 1", 0)))

        assertTrue(infos.isEmpty())
    }

    private fun info(index: Int, n: Int, keys: List<InheritanceKey> = emptyList()) =
        FallbackInheritanceInfo(index = index, requiredSignatures = 1, totalKeys = n, keys = keys, timelock = null)

    @Test
    fun `fallback key numbers continue across fallbacks when the plan lists no keys`() {
        val numbers = listOf(info(0, 1), info(1, 3), info(2, 2)).fallbackKeyNumbers()

        assertEquals(listOf(listOf(1), listOf(2, 3, 4), listOf(5, 6)), numbers)
    }

    @Test
    fun `fallback key numbers follow the server names when every key is named`() {
        val numbers = listOf(
            info(0, 1, listOf(fallbackKey("a", "Fallback key 1", 0))),
            info(1, 2, listOf(fallbackKey("b", "Fallback key 3", 1), fallbackKey("c", "Fallback key 4", 1))),
            info(2, 1),
        ).fallbackKeyNumbers()

        assertEquals(listOf(listOf(1), listOf(3, 4), listOf(5)), numbers)
    }

    @Test
    fun `a fallback with unnamed or missing keys is numbered from the previous one`() {
        val numbers = listOf(
            info(0, 2, listOf(fallbackKey("a", "Fallback key 1", 0))),
            info(1, 1, listOf(fallbackKey("b", "", 1))),
        ).fallbackKeyNumbers()

        assertEquals(listOf(listOf(1, 2), listOf(3)), numbers)
    }
}
