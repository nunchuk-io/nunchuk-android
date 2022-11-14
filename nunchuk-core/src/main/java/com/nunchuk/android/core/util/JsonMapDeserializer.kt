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

package com.nunchuk.android.core.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.internal.LinkedTreeMap
import java.lang.reflect.Type
import kotlin.math.ceil

class JsonMapDeserializer : JsonDeserializer<Map<String, Any>?> {

    @Suppress("UNCHECKED_CAST")
    @Throws(JsonParseException::class)
    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ) = read(jsonElement) as Map<String, Any>?

    private fun read(jsonElement: JsonElement): Any? {
        when {
            jsonElement.isJsonArray -> return readJsonArray(jsonElement)
            jsonElement.isJsonObject -> return readJsonObject(jsonElement)
            jsonElement.isJsonPrimitive -> {
                val prim = jsonElement.asJsonPrimitive
                when {
                    prim.isBoolean -> return prim.asBoolean
                    prim.isString -> return prim.asString
                    prim.isNumber -> {
                        val num = prim.asNumber
                        return if (ceil(num.toDouble()) == num.toLong().toDouble()) num.toLong() else num.toDouble()
                    }
                }
            }
        }
        return null
    }

    private fun readJsonArray(jsonElement: JsonElement) = jsonElement.asJsonArray.map(::read)

    private fun readJsonObject(jsonElement: JsonElement): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = LinkedTreeMap()
        val entrySet = jsonElement.asJsonObject.entrySet()
        for ((key, value) in entrySet) {
            map[key] = read(value)
        }
        return map
    }
}