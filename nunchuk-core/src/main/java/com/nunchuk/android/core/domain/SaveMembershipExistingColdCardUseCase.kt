/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.domain

import com.nunchuk.android.core.util.gson
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveMembershipExistingColdCardUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val syncKeyUseCase: SyncKeyUseCase
) : UseCase<SaveMembershipExistingColdCardUseCase.Params, Unit>(dispatcher) {
    override suspend fun execute(parameters: Params) {
        val signer = nativeSdk.getSignerByIndex(
            parameters.xfp,
            WalletType.MULTI_SIG.ordinal,
            AddressType.NATIVE_SEGWIT.ordinal,
            parameters.newIndex
        ) ?: throw NullPointerException("Can not get signer by index ${parameters.newIndex}")
        saveMembershipStepUseCase(
            MembershipStepInfo(
                step = parameters.step,
                masterSignerId = signer.masterFingerprint,
                plan = parameters.plan,
                verifyType = VerifyType.NONE,
                extraData = gson.toJson(
                    SignerExtra(
                        derivationPath = signer.derivationPath,
                        isAddNew = true,
                        signerType = signer.type,
                        userKeyFileName = ""
                    )
                ),
                groupId = parameters.groupId
            )
        )
        syncKeyUseCase(
            SyncKeyUseCase.Param(
                step = parameters.step,
                groupId = parameters.groupId,
                signer = signer
            )
        )
    }

    data class Params(
        val step: MembershipStep,
        val xfp: String,
        val plan: MembershipPlan,
        val groupId: String,
        val newIndex: Int,
    )
}