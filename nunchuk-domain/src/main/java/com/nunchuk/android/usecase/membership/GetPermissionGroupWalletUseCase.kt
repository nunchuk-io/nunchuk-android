package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetPermissionGroupWalletUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GroupWalletType, DefaultPermissions>(ioDispatcher) {

    override suspend fun execute(parameters: GroupWalletType): DefaultPermissions {
        return repository.getPermissionGroupWallet(parameters)
    }
}