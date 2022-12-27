package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.BannerApi
import com.nunchuk.android.core.data.model.banner.SubmitEmailViewAssistedWalletRequest
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.banner.BannerPage
import com.nunchuk.android.model.banner.BannerPageItem
import com.nunchuk.android.repository.BannerRepository
import javax.inject.Inject

internal class BannerRepositoryImpl @Inject constructor(
    private val api: BannerApi,
) : BannerRepository {
    override suspend fun submitEmail(reminderId: String?, email: String) {
        api.submitEmail(
            SubmitEmailViewAssistedWalletRequest(
                email = email,
                reminderId = reminderId,
            )
        )
    }

    override suspend fun getAssistedWalletContent(reminderId: String): BannerPage {
        val response = api.getAssistedWalletBannerContent(reminderId)
        val page = response.data.page
        return BannerPage(
            title = page?.content?.title.orEmpty(),
            desc = page?.content?.description.orEmpty(),
            items = page?.content?.items.orEmpty().map {
                BannerPageItem(
                    it.title.orEmpty(),
                    it.description.orEmpty(),
                    it.iconUrl.orEmpty()
                )
            }
        )
    }

    override suspend fun getBanners(): Banner? {
        val response = api.getBanners()
        val banner = response.data.banner ?: return null
        return Banner(
            id = banner.id.orEmpty(),
            desc = banner.content?.description.orEmpty(),
            title = banner.content?.title.orEmpty(),
            url = banner.content?.imageUrl.orEmpty()
        )
    }
}