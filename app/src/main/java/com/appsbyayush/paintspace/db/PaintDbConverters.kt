package com.appsbyayush.paintspace.db

import android.graphics.Typeface
import android.net.Uri
import androidx.room.TypeConverter
import com.appsbyayush.paintspace.models.DrawingImage
import com.appsbyayush.paintspace.models.DrawingPath
import com.appsbyayush.paintspace.models.DrawingText
import com.appsbyayush.paintspace.utils.UriDeserializer
import com.appsbyayush.paintspace.utils.UriSerializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

class PaintDbConverters {
    private val gsonUriSerializer = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriSerializer())
        .create()

    private val gsonUriDeserializer = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriDeserializer())
        .create()

    @TypeConverter
    fun fromDrawingPathToJson(drawingPath: DrawingPath?): String? {
        return if(drawingPath != null) Gson().toJson(drawingPath) else null
    }

    @TypeConverter
    fun fromJsonToDrawingPath(jsonString: String?): DrawingPath? {
        return if(jsonString != null) Gson().fromJson(jsonString, DrawingPath::class.java) else null
    }

    @TypeConverter
    fun fromDrawingImageToJson(drawingImage: DrawingImage?): String? {
        return if(drawingImage != null) gsonUriSerializer.toJson(drawingImage) else null
    }

    @TypeConverter
    fun fromJsonToDrawingImage(jsonString: String?): DrawingImage? {
        return if(jsonString != null) gsonUriDeserializer
            .fromJson(jsonString, DrawingImage::class.java) else null
    }

    @TypeConverter
    fun fromDrawingTextToJson(drawingText: DrawingText?): String? {
        return if(drawingText != null) gsonUriSerializer.toJson(drawingText) else null
    }

    @TypeConverter
    fun fromJsonToDrawingText(jsonString: String?): DrawingText? {
        return if(jsonString != null) gsonUriDeserializer
            .fromJson(jsonString, DrawingText::class.java) else null
    }

    @TypeConverter
    fun fromUriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun fromStringToUri(uriString: String?): Uri? {
        return if(uriString != null) Uri.parse(uriString) else null
    }

    @TypeConverter
    fun fromTimestampToDate(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun stringListToJson(stringList: List<String>?): String? {
        return if (stringList == null) null else Gson().toJson(stringList)
    }

    @TypeConverter
    fun jsonToStringList(listJson: String?): List<String>? {
        val gson = Gson()
        val type: Type = object : TypeToken<List<String>>() {}.type
        return if (listJson == null) null else gson.fromJson<List<String>>(listJson, type)
    }

    @TypeConverter
    fun typefaceToJson(typeface: Typeface?): String? {
        return if(typeface != null) Gson().toJson(typeface) else null
    }

    @TypeConverter
    fun fromJsonToTypeface(jsonString: String?): Typeface? {
        return if(jsonString != null) Gson().fromJson(jsonString, Typeface::class.java) else null
    }
}