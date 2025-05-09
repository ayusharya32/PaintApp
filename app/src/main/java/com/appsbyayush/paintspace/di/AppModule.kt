package com.appsbyayush.paintspace.di

import android.content.Context
import androidx.room.Room
import com.appsbyayush.paintspace.api.PaintApi
import com.appsbyayush.paintspace.db.PaintDatabase
import com.appsbyayush.paintspace.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideRetrofit(): Retrofit = Retrofit.Builder().apply {
        baseUrl(Constants.BASE_URL)
        addConverterFactory(GsonConverterFactory.create())
    }.build()

    @Singleton
    @Provides
    fun providePaintApi(retrofit: Retrofit): PaintApi {
        return retrofit.create(PaintApi::class.java)
    }

    @Singleton
    @Provides
    fun providePaintDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(context, PaintDatabase::class.java, "paintDB")
        .build()

    @Singleton
    @Provides
    fun providePaintDao(db: PaintDatabase) = db.getPaintDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ) = context.getSharedPreferences(Constants.APP_SHARED_PREFS, Context.MODE_PRIVATE)
}