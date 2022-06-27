package com.nunchuk.android.messages.components.list

import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class RoomShareViewPool @Inject constructor() {
    val recycledViewPool = RecyclerView.RecycledViewPool()
}