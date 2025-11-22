package com.example.wechatfriendforrenderstress.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Room数据库定义。
 */
@Database(entities = {FriendCircleEntity.class}, version = 1, exportSchema = false)
public abstract class FriendCircleDatabase extends RoomDatabase {
    public abstract FriendCircleDao friendCircleDao();
}

