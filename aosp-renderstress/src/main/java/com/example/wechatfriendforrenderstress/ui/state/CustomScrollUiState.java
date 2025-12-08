package com.example.wechatfriendforrenderstress.ui.state;

import androidx.annotation.Nullable;

import com.example.wechatfriendforrenderstress.LoadProfile;
import com.example.wechatfriendforrenderstress.beans.FriendCircleBean;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel向UI层暴露的简单状态容器。
 */
public class CustomScrollUiState {

    public enum Status {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final Status status;
    private final int loadType;
    private final List<FriendCircleBean> data;
    private final Throwable error;

    private CustomScrollUiState(Status status,
                                int loadType,
                                List<FriendCircleBean> data,
                                Throwable error) {
        this.status = status;
        this.loadType = loadType;
        this.data = data == null ? Collections.emptyList() : data;
        this.error = error;
    }

    public static CustomScrollUiState idle() {
        return new CustomScrollUiState(Status.IDLE, LoadProfile.LOAD_TYPE_LIGHT, Collections.emptyList(), null);
    }

    public static CustomScrollUiState loading(int loadType) {
        return new CustomScrollUiState(Status.LOADING, loadType, Collections.emptyList(), null);
    }

    public static CustomScrollUiState success(int loadType, List<FriendCircleBean> beans) {
        return new CustomScrollUiState(Status.SUCCESS, loadType, beans, null);
    }

    public static CustomScrollUiState error(int loadType, Throwable throwable) {
        return new CustomScrollUiState(Status.ERROR, loadType, Collections.emptyList(), throwable);
    }

    public Status getStatus() {
        return status;
    }

    public int getLoadType() {
        return loadType;
    }

    public List<FriendCircleBean> getData() {
        return data;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }
}

