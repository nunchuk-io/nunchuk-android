package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InheritanceClaimDownloadBackupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<String, BackupKey>(dispatcher) {
    override suspend fun execute(parameters: String): BackupKey {
        return userWalletsRepository.inheritanceClaimDownloadBackup(parameters)
    }
}