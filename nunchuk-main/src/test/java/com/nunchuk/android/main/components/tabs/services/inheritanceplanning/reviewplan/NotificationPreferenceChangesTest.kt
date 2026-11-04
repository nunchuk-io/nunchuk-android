package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import com.nunchuk.android.model.inheritance.EmailNotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPreferenceChangesTest {

    private val alice = EmailNotificationSettings(email = "alice@example.com", notifyFallbacks = emptyList())

    @Test
    fun `a new recipient is changed everywhere`() {
        assertEquals(RecipientNotificationChanges.ALL, alice.changesFrom(previous = null, fallbackCount = 2))
    }

    @Test
    fun `only the value that differs is marked`() {
        val changes = alice.copy(includeWalletConfiguration = false).changesFrom(alice, fallbackCount = 2)

        assertEquals(RecipientNotificationChanges(includeWalletConfiguration = true), changes)
    }

    @Test
    fun `with fallbacks the unlock notices compare as the picked timelocks`() {
        val before = alice.copy(notifyOnTimelockExpiry = false, notifyFallbacks = listOf(0))
        val after = before.copy(notifyFallbacks = listOf(0, 1))

        assertEquals(RecipientNotificationChanges(unlockNotices = true), after.changesFrom(before, fallbackCount = 2))
        // Same picks in another order, or indexes past the plan's fallbacks, are not a change.
        assertEquals(
            RecipientNotificationChanges(),
            before.copy(notifyFallbacks = listOf(0, 5)).changesFrom(before, fallbackCount = 2),
        )
    }

    @Test
    fun `without fallbacks the unlock notice is the single timelock flag`() {
        val changes = alice.copy(notifyOnTimelockExpiry = false).changesFrom(alice, fallbackCount = 0)

        assertEquals(RecipientNotificationChanges(unlockNotices = true), changes)
        // notify_fallbacks is ignored when the plan has none.
        assertEquals(RecipientNotificationChanges(), alice.copy(notifyFallbacks = listOf(0)).changesFrom(alice, fallbackCount = 0))
    }
}
