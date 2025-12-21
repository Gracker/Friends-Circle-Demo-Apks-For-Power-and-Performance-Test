package com.example.wechatfriendforcustomscroller.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room实体，用于将每条朋友圈数据按负载类型持久化成JSON。
 */
@Entity(tableName = "friend_circle_entries")
public class FriendCircleEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int loadType;

    public int entryIndex;

    @NonNull
    public String payloadJson;

    public FriendCircleEntity(int loadType, int entryIndex, @NonNull String payloadJson) {
        this.loadType = loadType;
        this.entryIndex = entryIndex;
        this.payloadJson = payloadJson;
    }
}

