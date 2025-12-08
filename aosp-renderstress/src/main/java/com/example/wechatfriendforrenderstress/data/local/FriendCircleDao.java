package com.example.wechatfriendforrenderstress.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO接口，提供按负载类型读取/写入朋友圈数据。
 */
@Dao
public interface FriendCircleDao {

    @Query("SELECT * FROM friend_circle_entries WHERE loadType = :loadType ORDER BY entryIndex ASC")
    List<FriendCircleEntity> getEntriesForLoad(int loadType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEntries(List<FriendCircleEntity> entities);

    @Query("DELETE FROM friend_circle_entries WHERE loadType = :loadType")
    void clearLoadType(int loadType);
}

