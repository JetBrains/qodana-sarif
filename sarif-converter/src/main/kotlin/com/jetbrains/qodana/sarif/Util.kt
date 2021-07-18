package com.jetbrains.qodana.sarif

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

class Util {
    companion object {
        inline fun JsonReader._object(block: JsonReader.(String) -> Unit) {
            beginObject()
            while (peek() == JsonToken.NAME) {
                block(nextName())
            }
            endObject()
        }

        inline fun JsonReader._object(fieldName: String, block: JsonReader.() -> Unit) {
            beginObject()
            while (peek() == JsonToken.NAME) {
                if (nextName() == fieldName) block() else skipValue()
            }
            endObject()
        }



        inline fun JsonReader.objectArray(withObjectBoundaries: Boolean = false, block: JsonReader.() -> Unit) {
            beginArray()
            while (peek() == JsonToken.BEGIN_OBJECT) {
                if (withObjectBoundaries) beginObject()
                block()
                if (withObjectBoundaries) endObject()
            }
            endArray()
        }

        inline fun JsonReader.stringArray(block: JsonReader.() -> Unit) {
            beginArray()
            block()
            endArray()
        }
    }
}