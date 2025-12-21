package com.example.wechatfriendforrenderstress;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.loadconfig.LoadType;
import com.example.wechatfriendforrenderstress.data.FriendCircleRepository;
import com.example.wechatfriendforrenderstress.di.AppModule;
import com.example.wechatfriendforrenderstress.ui.state.CustomScrollUiState;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * MVVM中的ViewModel层，负责触发数据加载并暴露UI状态。
 */
@HiltViewModel
public class CustomScrollViewModel extends ViewModel {

    private final FriendCircleRepository repository;
    private final ExecutorService ioExecutor;
    private final MutableLiveData<CustomScrollUiState> uiState = new MutableLiveData<>(CustomScrollUiState.idle());

    @Inject
    public CustomScrollViewModel(FriendCircleRepository repository,
                                 @AppModule.IoExecutor ExecutorService ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<CustomScrollUiState> getUiState() {
        return uiState;
    }

    public void loadFeed(@LoadType.Type int loadType) {
        uiState.postValue(CustomScrollUiState.loading(loadType));
        ioExecutor.execute(() -> {
            try {
                uiState.postValue(CustomScrollUiState.success(loadType, repository.getFriendCircles(loadType)));
            } catch (Exception e) {
                uiState.postValue(CustomScrollUiState.error(loadType, e));
            }
        });
    }
}

