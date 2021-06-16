package com.nunchuk.android.main.components.tabs.chat

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.chat.ChatFragmentTab.CONTACTS
import com.nunchuk.android.main.components.tabs.chat.ChatFragmentTab.MESSAGES
import com.nunchuk.android.main.components.tabs.chat.contacts.ContactsFragment
import com.nunchuk.android.main.components.tabs.chat.messages.MessagesFragment

class ChatFragmentPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            MESSAGES.position -> MessagesFragment.newInstance()
            CONTACTS.position -> ContactsFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun getCount() = ChatFragmentTab.values().size

    override fun getPageTitle(position: Int) = when (position) {
        MESSAGES.position -> context.getString(R.string.nc_title_message)
        CONTACTS.position -> context.getString(R.string.nc_title_contacts)
        else -> ""
    }

}