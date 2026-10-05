package com.nova.browser.di

import android.content.Context
import androidx.room.Room
import com.nova.browser.data.BookmarkDao
import com.nova.browser.data.DownloadDao
import com.nova.browser.data.HistoryDao
import com.nova.browser.data.NovaDatabase
import com.nova.browser.data.TabDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): NovaDatabase =
        Room.databaseBuilder(ctx, NovaDatabase::class.java, "nova.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTabDao(db: NovaDatabase): TabDao = db.tabDao()

    @Provides
    fun provideHistoryDao(db: NovaDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideBookmarkDao(db: NovaDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideDownloadDao(db: NovaDatabase): DownloadDao = db.downloadDao()
}