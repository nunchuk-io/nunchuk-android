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

package com.nunchuk.android.signer.components.add

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.usecase.ResultExistingKey

sealed class AddAirgapSignerEvent {
    data class AddAirgapSignerSuccessEvent(val singleSigner: SingleSigner) : AddAirgapSignerEvent()
    data class ParseKeystoneAirgapSignerSuccess(val signers: List<SingleSigner>) : AddAirgapSignerEvent()
    data class AddAirgapSignerErrorEvent(val message: String) : AddAirgapSignerEvent()
    data object ErrorMk4TestNet : AddAirgapSignerEvent()
    data object AddSameKey : AddAirgapSignerEvent()
    data class LoadingEventAirgap(val isLoading: Boolean) : AddAirgapSignerEvent()
    data object NewIndexNotMatchException : AddAirgapSignerEvent()
    data object XfpNotMatchException : AddAirgapSignerEvent()
    data class CheckExisting(val type: ResultExistingKey, val singleSigner: SingleSigner) : AddAirgapSignerEvent()
}
