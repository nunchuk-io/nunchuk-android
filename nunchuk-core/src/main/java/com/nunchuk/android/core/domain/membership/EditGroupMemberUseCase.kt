package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class EditGroupMemberUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<EditGroupMemberUseCase.Param, ByzantineGroup>(dispatcher) {
    override suspend fun execute(parameters: Param): ByzantineGroup {
        val authorizations = mutableListOf<String>()
        parameters.signatures.forEach { (masterFingerprint, signature) ->
            val requestToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
            authorizations.add(requestToken)
        }
        return userWalletRepository.editGroupMember(
            authorizations = authorizations,
            verifyToken = parameters.verifyToken,
            members = parameters.members,
            securityQuestionToken = parameters.securityQuestionToken,
            groupId = parameters.groupId,
            confirmCode = parameters.confirmCode
        )
    }

    class Param(
        val signatures: Map<String, String> = emptyMap(),
        val members: List<AssistedMember>,
        val verifyToken: String,
        val securityQuestionToken: String,
        val groupId: String,
        val confirmCode: String
    )
}