package com.nunchuk.android.core.util

import androidx.core.view.isVisible
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.widget.NCAvatarView

fun NCAvatarView.displayAvatar(imageLoader: ImageLoader, avatarUrl: String?, name: String) {
    binding.name.text = name
    if (avatarUrl.isNullOrEmpty()) {
        binding.avatarWrapper.isVisible = false
        binding.name.isVisible = true
    } else {
        binding.avatarWrapper.isVisible = true
        binding.name.isVisible = false
        imageLoader.loadImage(avatarUrl, binding.avatar) {
            binding.avatarWrapper.isVisible = false
            binding.name.isVisible = true
        }
    }
}
