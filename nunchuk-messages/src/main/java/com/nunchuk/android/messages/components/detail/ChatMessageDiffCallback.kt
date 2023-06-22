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

package com.nunchuk.android.messages.components.detail

import androidx.recyclerview.widget.DiffUtil

internal object ChatMessageDiffCallback : DiffUtil.ItemCallback<AbsChatModel>() {
    override fun areItemsTheSame(p0: AbsChatModel, p1: AbsChatModel): Boolean {
        return p0.getType() == p1.getType()
    }

    override fun areContentsTheSame(p0: AbsChatModel, p1: AbsChatModel): Boolean {
        if (p0 is DateModel && p1 is DateModel) {
            return p0.getType() == p1.getType() && p0.date == p1.date
        } else if (p0 is MessageModel && p1 is MessageModel) {
            return areMessagesTheSame(p0.message, p1.message)
        }
        return p0.getType() == p1.getType()
    }

    private fun areMessagesTheSame(p0: Message, p1: Message): Boolean = p0 == p1
}