package com.example.agribotz.app.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

class UpdateVariableRequestJsonAdapter(
    private val moshi: Moshi
) : JsonAdapter<UpdateVariableRequest>() {

    override fun toJson(writer: JsonWriter, value: UpdateVariableRequest?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        val wasSerializeNulls = writer.serializeNulls
        writer.serializeNulls = true

        try {
            writer.beginObject()

            writer.name("variableId")
            writer.value(value.variableId)

            writer.name("value")
            val bodyValue = value.value

            if (bodyValue == null) {
                writer.nullValue()
            } else {
                @Suppress("UNCHECKED_CAST")
                val adapter = moshi.adapter(bodyValue.javaClass) as JsonAdapter<Any>
                adapter.toJson(writer, bodyValue)
            }

            writer.endObject()
        } finally {
            writer.serializeNulls = wasSerializeNulls
        }
    }

    override fun fromJson(reader: JsonReader): UpdateVariableRequest? {
        var variableId: String? = null
        var value: Any? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "variableId" -> variableId = reader.nextString()
                "value" -> {
                    value = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull<Unit>()
                        null
                    } else {
                        reader.readJsonValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return variableId?.let {
            UpdateVariableRequest(
                variableId = it,
                value = value
            )
        }
    }
}