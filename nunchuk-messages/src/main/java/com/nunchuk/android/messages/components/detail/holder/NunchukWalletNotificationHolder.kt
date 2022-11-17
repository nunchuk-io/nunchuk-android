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

package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.WalletEventType
import com.nunchuk.android.messages.util.bindNotificationBackground
import com.nunchuk.android.messages.util.displayNameOrId
import com.nunchuk.android.messages.util.getBodyElementValueByKey
import com.nunchuk.android.utils.CrashlyticsReporter

internal class NunchukWalletNotificationHolder(
    val binding: ItemNunchukNotificationBinding,
    val viewConfig: () -> Unit,
    val finalizeWallet: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukWalletMessage) {
        val sender = model.sender.displayNameOrId()
        when (model.msgType) {
            WalletEventType.JOIN -> {
                val fingerPrint = getFingerPrint(model)
                binding.notification.text = getHtmlString(R.string.nc_message_wallet_join, sender, fingerPrint)
                binding.root.setOnClickListener { viewConfig() }
            }
            WalletEventType.CREATE -> {
                binding.notification.text = getString(R.string.nc_message_wallet_created)
            }
            WalletEventType.LEAVE -> {
                val fingerPrint = getFingerPrint(model)
                binding.notification.text = getHtmlString(R.string.nc_message_wallet_leave, sender, fingerPrint)
            }
            WalletEventType.CANCEL -> {
                binding.notification.text = getHtmlString(R.string.nc_message_wallet_cancel, sender)
            }
            WalletEventType.READY -> {
                if (model.isOwner) {
                    binding.notification.text = getHtmlString(R.string.nc_message_wallet_finalize)
                    binding.root.setOnClickListener { finalizeWallet() }
                } else {
                    binding.notification.text = getHtmlString(R.string.nc_message_wallet_ready)
                }
            }
            else -> {
                binding.notification.text = "${model.msgType}"
            }
        }
        binding.root.bindNotificationBackground(model.msgType == WalletEventType.CREATE)
    }

    private fun getFingerPrint(model: NunchukWalletMessage): String {
        val fingerPrint = try {
            val keyValue = model.timelineEvent.getBodyElementValueByKey("key")
            val signer = keyValue.replace("\"", "").toSigner()
            signer.fingerPrint
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
            ""
        }
        return fingerPrint
    }

}