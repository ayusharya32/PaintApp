package com.appsbyayush.paintspace.utils

import android.net.Uri
import android.util.Log
import com.google.gson.*
import java.lang.reflect.Type

private const val TAG = "GsonTypeAdapters"

class UriSerializer : JsonSerializer<Uri?> {
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        Log.d(TAG, "serialize: $src")
        return JsonPrimitive(src.toString())
    }
}

class UriDeserializer : JsonDeserializer<Uri?> {
    override fun deserialize(
        src: JsonElement, srcType: Type?,
        context: JsonDeserializationContext?
    ): Uri? {
        val uri = try {
            Uri.parse(src.asString)
        } catch(e: Exception) {
            Log.d(TAG, "deserialize: $e")
            null
        }

        Log.d(TAG, "deserialize: URI $uri")
        return uri
    }
}