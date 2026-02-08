package com.example.wechatfriendforrenderstress.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.wechatfriendforrenderstress.CustomScrollDataGenerator;
import com.example.loadconfig.LoadType;
import com.example.scrolling.common.beans.FriendCircleBean;
import com.example.wechatfriendforrenderstress.data.local.FriendCircleDao;
import com.example.wechatfriendforrenderstress.data.local.FriendCircleEntity;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 负责管理自定义滚动列表的数据来源：Room缓存 + 固定数据生成器。
 */
@Singleton
public class FriendCircleRepository {

    private static final String TAG = "FriendCircleRepository";

    private final FriendCircleDao friendCircleDao;
    private final Gson gson;
    private final Context appContext;

    @Inject
    public FriendCircleRepository(FriendCircleDao friendCircleDao,
                                  Gson gson,
                                  @ApplicationContext Context appContext) {
        this.friendCircleDao = friendCircleDao;
        this.gson = gson;
        this.appContext = appContext.getApplicationContext();
    }

    /**
     * 读取指定负载的数据，如必要则先生成再缓存。
     */
    @NonNull
    public List<FriendCircleBean> getFriendCircles(@LoadType.Type int loadType) {
        List<FriendCircleEntity> cached = friendCircleDao.getEntriesForLoad(loadType);
        if (cached != null && !cached.isEmpty()) {
            return mapEntities(cached);
        }
        List<FriendCircleBean> generated = CustomScrollDataGenerator.getInstance()
                .generateDataForLoadType(appContext, loadType);
        persist(loadType, generated);
        return generated;
    }

    private void persist(@LoadType.Type int loadType, List<FriendCircleBean> beans) {
        friendCircleDao.clearLoadType(loadType);
        if (beans == null || beans.isEmpty()) {
            return;
        }
        List<FriendCircleEntity> entities = new ArrayList<>(beans.size());
        for (int i = 0; i < beans.size(); i++) {
            FriendCircleBean bean = beans.get(i);
            try {
                String payload = gson.toJson(bean);
                entities.add(new FriendCircleEntity(loadType, i, payload));
            } catch (Exception e) {
                Log.e(TAG, "serialize friend circle failed", e);
            }
        }
        if (!entities.isEmpty()) {
            friendCircleDao.insertEntries(entities);
        }
    }

    private List<FriendCircleBean> mapEntities(List<FriendCircleEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<FriendCircleBean> beans = new ArrayList<>(entities.size());
        for (FriendCircleEntity entity : entities) {
            try {
                FriendCircleBean bean = gson.fromJson(entity.payloadJson, FriendCircleBean.class);
                beans.add(bean);
            } catch (Exception e) {
                Log.e(TAG, "deserialize friend circle failed", e);
            }
        }
        return beans;
    }
}

