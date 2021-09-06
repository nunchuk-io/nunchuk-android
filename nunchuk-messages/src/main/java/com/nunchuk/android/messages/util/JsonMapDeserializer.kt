package com.nunchuk.android.messages.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.internal.LinkedTreeMap
import java.lang.reflect.Type
import kotlin.math.ceil

class JsonMapDeserializer : JsonDeserializer<Map<String, Any>?> {

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