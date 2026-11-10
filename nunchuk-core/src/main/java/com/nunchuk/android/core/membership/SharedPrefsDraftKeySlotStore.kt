package com.nunchuk.android.core.membership

import com.google.gson.Gson
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.model.inheritance.DraftKeySlot
import com.nunchuk.android.model.inheritance.DraftKeySlotStore
import javax.inject.Inject

/** [DraftKeySlotStore] on the app preferences; written synchronously so a kill right after keeps it. */
internal class SharedPrefsDraftKeySlotStore @Inject constructor(
    private val preferences: NCSharePreferences,
    private val gson: Gson,
) : DraftKeySlotStore {
    override fun load(): DraftKeySlot? =
        preferences.draftKeySlot.takeIf { it.isNotEmpty() }
            ?.let { runCatching { gson.fromJson(it, DraftKeySlot::class.java) }.getOrNull() }

    override fun save(slot: DraftKeySlot?) {
        preferences.draftKeySlot = slot?.let(gson::toJson).orEmpty()
    }
}
