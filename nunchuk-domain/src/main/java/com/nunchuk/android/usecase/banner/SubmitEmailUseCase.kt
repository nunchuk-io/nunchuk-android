package com.nunchuk.android.usecase.banner

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.BannerRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SubmitEmailUseCase @Inject constructor(
    private val repository: BannerRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<SubmitEmailUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        repository.submitEmail(parameters.bannerId, parameters.email)
    }

    data class Param(val bannerId: String, val email: String)
}