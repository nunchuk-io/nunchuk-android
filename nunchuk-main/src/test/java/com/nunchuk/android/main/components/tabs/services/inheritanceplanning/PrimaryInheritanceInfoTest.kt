package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import com.nunchuk.android.model.InheritanceKey
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryInheritanceInfoTest {

    private fun policy(
        m: Int,
        n: Int,
        isCustomized: Boolean,
        type: OnChainInheritancePolicyType = OnChainInheritancePolicyType.CONFIGURABLE,
    ) = OnChainInheritancePolicy(
        type = type,
        primary = PrimaryInheritanceCondition(m = m, n = n, isCustomized = isCustomized, timelock = null),
    )

    private fun primaryKey(xfp: String, name: String) =
        InheritanceKey(xfp = xfp, section = InheritanceSection.PRIMARY, name = name)

    private fun fallbackKey(xfp: String, name: String) =
        InheritanceKey(xfp = xfp, section = InheritanceSection.FALLBACK, name = name, fallbackIndex = 0)

    @Test
    fun `guided default lists the server's primary inheritance key and counts the owner keys`() {
        val info = policy(m = 2, n = 4, isCustomized = false).toPrimaryInheritanceInfo(
            listOf(primaryKey("6b35f210", "Inheritance key 1"), fallbackKey("1111aaaa", "Fallback key 1"))
        )!!

        assertEquals(listOf("6b35f210"), info.inheritanceKeys.map { it.xfp })
        assertEquals("Inheritance key 1", info.inheritanceKeys.single().name)
        assertEquals(2, info.requiredSignatures)
        assertEquals(4, info.totalKeys)
        assertEquals(3, info.ownerKeyCount)
    }

    @Test
    fun `customized primary lists every primary key and has no owner keys`() {
        val info = policy(m = 2, n = 3, isCustomized = true).toPrimaryInheritanceInfo(
            listOf(primaryKey("a", "Inheritance key 1"), primaryKey("b", "Inheritance key 2"), primaryKey("c", "Inheritance key 3"))
        )!!

        assertEquals(listOf("a", "b", "c"), info.inheritanceKeys.map { it.xfp })
        assertEquals(0, info.ownerKeyCount)
    }

    @Test
    fun `customized 1 of 1 is a single key`() {
        val info = policy(m = 1, n = 1, isCustomized = true)
            .toPrimaryInheritanceInfo(listOf(primaryKey("a", "Inheritance key 1")))!!

        assertTrue(info.isSingleKey)
    }

    @Test
    fun `no key marked for primary inheritance is never guessed`() {
        val policy = policy(m = 2, n = 4, isCustomized = false)

        assertNull(policy.toPrimaryInheritanceInfo(emptyList()))
        // Keys without a section (legacy shape) or only fallback keys must not be promoted.
        assertNull(policy.toPrimaryInheritanceInfo(listOf(InheritanceKey(xfp = "owner1"))))
        assertNull(policy.toPrimaryInheritanceInfo(listOf(fallbackKey("1111aaaa", "Fallback key 1"))))
    }

    @Test
    fun `legacy policy keeps the legacy copy`() {
        val policy = policy(m = 1, n = 3, isCustomized = false, type = OnChainInheritancePolicyType.LEGACY)

        assertNull(policy.toPrimaryInheritanceInfo(listOf(primaryKey("a", "Inheritance key 1"))))
    }
}
