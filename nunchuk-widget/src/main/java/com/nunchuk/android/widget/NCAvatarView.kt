package com.nunchuk.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.nunchuk.android.widget.databinding.NcAvatarViewBinding

class NCAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val binding = NcAvatarViewBinding.inflate(LayoutInflater.from(context), this, true)

}
