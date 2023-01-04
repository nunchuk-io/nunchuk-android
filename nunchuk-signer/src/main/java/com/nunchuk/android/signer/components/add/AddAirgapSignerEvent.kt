/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

sealed class AddAirgapSignerEvent {
    data class AddAirgapSignerSuccessEvent(val singleSigner: SingleSigner) : AddAirgapSignerEvent()
    data class ParseKeystoneAirgapSignerSuccess(val signers: List<SingleSigner>) : AddAirgapSignerEvent()
    data class AddAirgapSignerErrorEvent(val message: String) : AddAirgapSignerEvent()
    object InvalidAirgapSignerSpecEvent : AddAirgapSignerEvent()
    object AirgapSignerNameRequiredEvent : AddAirgapSignerEvent()
    object ErrorMk4TestNet : AddAirgapSignerEvent()
    object AddSameKey : AddAirgapSignerEvent()
    data class ParseKeystoneAirgapSigner(val signers: List<SingleSigner>) : AddAirgapSignerEvent()
    data class LoadingEventAirgap(val isLoading: Boolean) : AddAirgapSignerEvent()
}
