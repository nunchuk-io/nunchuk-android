package com.nunchuk.android.messages.pending

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.nunchuk.android.messages.pending.PendingContactTab.RECEIVED
import com.nunchuk.android.messages.pending.receive.ReceivedFragment
import com.nunchuk.android.messages.pending.sent.SentFragment

class PendingContactsPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            RECEIVED.position -> ReceivedFragment.newInstance()
            PendingContactTab.SENT.position -> SentFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun getCount() = PendingContactTab.values().size

    // FIXME localize
    override fun getPageTitle(position: Int) = when (position) {
        RECEIVED.position -> "Received"
        PendingContactTab.SENT.position -> "Sent"
        else -> ""
    }

}