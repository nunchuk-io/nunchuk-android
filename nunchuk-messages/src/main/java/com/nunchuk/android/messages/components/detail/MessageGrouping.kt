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

package com.nunchuk.android.messages.components.detail

import com.nunchuk.android.messages.util.simpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

internal fun List<Message>.groupByDate(): List<AbsChatModel> {
    val grouping: LinkedHashMap<String, Set<Message>> = LinkedHashMap()
    var messages: MutableSet<Message>
    for (model in this) {
        val hashMapKey: String = Date(model.time).simpleDateFormat()
        if (grouping.containsKey(hashMapKey)) {
            val set = grouping[hashMapKey]!!
            (set as MutableSet).add(model)
        } else {
            messages = LinkedHashSet()
            messages.add(model)
            grouping[hashMapKey] = messages
        }
    }
    return grouping.groupByDate()
}

internal fun LinkedHashMap<String, Set<Message>>.groupByDate(): List<AbsChatModel> {
    val models = ArrayList<AbsChatModel>()
    for (date in keys) {
        val dateItem = DateModel(date)
        models.add(dateItem)
        this[date]!!.mapTo(models, ::MessageModel)
    }
    return models
}
