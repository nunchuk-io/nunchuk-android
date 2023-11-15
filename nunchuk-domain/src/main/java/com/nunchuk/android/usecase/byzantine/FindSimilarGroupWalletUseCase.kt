package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.SimilarGroup
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class FindSimilarGroupWalletUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val groupWalletRepository: GroupWalletRepository
) : UseCase<String, List<SimilarGroup>>(ioDispatcher) {

    override suspend fun execute(parameters: String): List<SimilarGroup> =
        groupWalletRepository.findSimilarGroup(parameters)
}