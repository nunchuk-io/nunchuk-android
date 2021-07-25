package com.nunchuk.android.contact.components.pending

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.components.pending.PendingContactTab.RECEIVED
import com.nunchuk.android.contact.components.pending.PendingContactTab.SENT
import com.nunchuk.android.contact.components.pending.receive.ReceivedFragment
import com.nunchuk.android.contact.components.pending.sent.SentFragment

class PendingContactsPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            RECEIVED.position -> ReceivedFragment.newInstance()
            SENT.position -> SentFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun getCount() = PendingContactTab.values().size

    override fun getPageTitle(position: Int) = when (position) {
        RECEIVED.position -> context.getString(R.string.nc_contact_received)
        SENT.position -> context.getString(R.string.nc_contact_sent)
        else -> ""
    }

}