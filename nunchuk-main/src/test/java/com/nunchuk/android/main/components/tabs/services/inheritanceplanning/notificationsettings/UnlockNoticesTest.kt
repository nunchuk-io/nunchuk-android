package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

import com.nunchuk.android.model.inheritance.EmailNotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockNoticesTest {

    private val settings = EmailNotificationSettings(email = "alice@gmail.com")

    @Test
    fun `routes list primary first, then fallbacks in order, ignoring unknown indexes`() {
        val routes = settings.copy(notifyOnTimelockExpiry = true, notifyFallbacks = listOf(2, 0, 5, 0))
            .unlockNoticeRoutes(fallbackCount = 3)

        assertEquals(
            listOf(UnlockNoticeRoute.Primary, UnlockNoticeRoute.Fallback(0), UnlockNoticeRoute.Fallback(2)),
            routes,
        )
    }

    @Test
    fun `fallbacks not sent read as none`() {
        val routes = settings.copy(notifyOnTimelockExpiry = false, notifyFallbacks = null)
            .unlockNoticeRoutes(fallbackCount = 2)

        assertTrue(routes.isEmpty())
        assertFalse(settings.copy(notifyOnTimelockExpiry = false).notifiesOnAnyUnlock(fallbackCount = 2))
    }

    @Test
    fun `switch on picks every timelock, switch off sends an empty fallback list`() {
        val on = settings.copy(notifyOnTimelockExpiry = false, notifyFallbacks = null)
            .withAllUnlockNotices(enabled = true, fallbackCount = 2)
        assertTrue(on.notifyOnTimelockExpiry)
        assertEquals(listOf(0, 1), on.notifyFallbacks)

        val off = on.withAllUnlockNotices(enabled = false, fallbackCount = 2)
        assertFalse(off.notifyOnTimelockExpiry)
        assertEquals(emptyList<Int>(), off.notifyFallbacks)
        assertFalse(off.notifiesOnAnyUnlock(fallbackCount = 2))
    }

    @Test
    fun `checking a route changes only that route`() {
        val start = settings.copy(notifyOnTimelockExpiry = true, notifyFallbacks = listOf(1))

        val withFallback1 = start.withUnlockNotice(UnlockNoticeRoute.Fallback(0), checked = true, fallbackCount = 2)
        assertEquals(listOf(0, 1), withFallback1.notifyFallbacks)
        assertTrue(withFallback1.notifyOnTimelockExpiry)

        val withoutFallback2 = withFallback1.withUnlockNotice(UnlockNoticeRoute.Fallback(1), checked = false, fallbackCount = 2)
        assertEquals(listOf(0), withoutFallback2.notifyFallbacks)

        val withoutPrimary = withoutFallback2.withUnlockNotice(UnlockNoticeRoute.Primary, checked = false, fallbackCount = 2)
        assertFalse(withoutPrimary.notifyOnTimelockExpiry)
        assertEquals(listOf(0), withoutPrimary.notifyFallbacks)
        assertTrue(withoutPrimary.notifiesOnAnyUnlock(fallbackCount = 2))
    }
}
