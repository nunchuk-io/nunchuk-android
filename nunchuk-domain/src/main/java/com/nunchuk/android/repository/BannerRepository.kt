package com.nunchuk.android.repository

import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.banner.BannerPage

interface BannerRepository {
    suspend fun submitEmail(reminderId: String, email: String)
    suspend fun getAssistedWalletContent(reminderId: String): BannerPage
    suspend fun getBanners(): Banner
}