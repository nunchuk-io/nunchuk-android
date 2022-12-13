package com.nunchuk.android.usecase.banner

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.banner.BannerPage
import com.nunchuk.android.repository.BannerRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAssistedWalletPageContentUseCase @Inject constructor(
    private val repository: BannerRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<String, BannerPage>(ioDispatcher) {
    override suspend fun execute(parameters: String): BannerPage {
        return repository.getAssistedWalletContent(parameters)
    }
}