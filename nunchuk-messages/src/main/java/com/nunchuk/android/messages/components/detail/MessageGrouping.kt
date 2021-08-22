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
