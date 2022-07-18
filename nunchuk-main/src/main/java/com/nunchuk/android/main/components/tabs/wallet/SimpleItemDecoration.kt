package com.nunchuk.android.main.components.tabs.wallet

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.main.R

class SimpleItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val spaceInDp = context.resources.getDimensionPixelSize(R.dimen.nc_padding_8)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) != 0) {
            outRect.left = spaceInDp
        }
    }
}