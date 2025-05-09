package com.appsbyayush.paintspace.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.appsbyayush.paintspace.models.*
import com.appsbyayush.paintspace.paging.GradientSearchRemoteKey
import com.appsbyayush.paintspace.paging.GraphicElementSearchRemoteKey

@Database(
    entities = [Drawing::class, DrawingItem::class, GraphicElement::class, GraphicElementSearchResult::class,
        GraphicElementSearchRemoteKey::class, Gradient::class, GradientSearchResult::class,
        GradientSearchRemoteKey::class, UserGraphicElement::class, UserGradient::class,
        FontItem::class, UserTextElement::class],
    version = 1
)
@TypeConverters(PaintDbConverters::class)
abstract class PaintDatabase: RoomDatabase() {
    abstract fun getPaintDao(): PaintDao
    abstract fun getPaintRemoteKeysDao(): PaintRemoteKeysDao
}