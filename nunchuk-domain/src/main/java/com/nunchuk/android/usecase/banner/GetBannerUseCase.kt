package com.nunchuk.android.usecase.banner

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.repository.BannerRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetBannerUseCase @Inject constructor(
    private val repository: BannerRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<Unit, Banner?>(ioDispatcher) {
    override suspend fun execute(parameters: Unit): Banner? {
        return repository.getBanners()
    }
}