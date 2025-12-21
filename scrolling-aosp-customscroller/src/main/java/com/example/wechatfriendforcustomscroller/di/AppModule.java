package com.example.wechatfriendforcustomscroller.di;

import android.content.Context;

import androidx.room.Room;

import com.example.wechatfriendforcustomscroller.data.local.FriendCircleDao;
import com.example.wechatfriendforcustomscroller.data.local.FriendCircleDatabase;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * 全局依赖注入模块，提供Room数据库和IO线程池等依赖。
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Qualifier
    public @interface IoExecutor {}

    @Provides
    @Singleton
    public FriendCircleDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, FriendCircleDatabase.class, "custom_scroll_feed.db")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public FriendCircleDao provideFriendCircleDao(FriendCircleDatabase database) {
        return database.friendCircleDao();
    }

    @Provides
    @Singleton
    @IoExecutor
    public ExecutorService provideIoExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }
}

