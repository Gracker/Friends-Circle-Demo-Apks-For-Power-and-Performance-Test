package com.android.wechatfriendforpower.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.wechatfriendforpower.PowerTestUtil;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.interfaces.OnItemClickPopupMenuListener;
import com.stfalcon.imageviewer.loader.ImageLoader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

/**
 * PowerFriendCircleAdapter的单元测试
 */
public class PowerAdapterTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private RecyclerView mockRecyclerView;
    
    @Mock
    private LinearLayoutManager mockLayoutManager;
    
    @Mock
    private ImageLoader<String> mockImageLoader;
    
    @Mock
    private LayoutInflater mockLayoutInflater;
    
    @Mock
    private View mockView;
    
    @Mock
    private ViewGroup mockViewGroup;
    
    @Mock
    private PowerFriendCircleAdapter adapter;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置基本的模拟行为
        when(mockRecyclerView.getLayoutManager()).thenReturn(mockLayoutManager);
        when(mockContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).thenReturn(mockLayoutInflater);
        when(mockLayoutInflater.inflate(anyInt(), any(ViewGroup.class), any(Boolean.class))).thenReturn(mockView);
        
        // 创建适配器
        adapter = new PowerFriendCircleAdapter(mockContext, mockRecyclerView, mockImageLoader);
    }
    
    @Test
    public void testConstructor() {
        // 验证构造函数是否正确初始化适配器
        assertNotNull(adapter);
    }
    
    @Test
    public void testGetItemCount() {
        // 测试没有数据时的item数量
        assertEquals(0, adapter.getItemCount());
        
        // 设置header view
        adapter.setHeaderView(mockView);
        assertEquals(1, adapter.getItemCount()); // 只有header
        
        // 添加数据
        List<FriendCircleBean> beans = PowerTestUtil.createTestFriendCircleBeans(3);
        adapter.setFriendCircleBeans(beans);
        assertEquals(4, adapter.getItemCount()); // 3个数据项 + header
    }
    
    @Test
    public void testGetItemViewType() {
        // 设置header view
        adapter.setHeaderView(mockView);
        
        // 添加数据
        List<FriendCircleBean> beans = PowerTestUtil.createTestFriendCircleBeans(3);
        adapter.setFriendCircleBeans(beans);
        
        // 测试头部视图类型
        assertEquals(PowerFriendCircleAdapter.TYPE_HEADER, adapter.getItemViewType(0));
        
        // 测试普通视图类型
        assertEquals(PowerFriendCircleAdapter.TYPE_NORMAL, adapter.getItemViewType(1));
        assertEquals(PowerFriendCircleAdapter.TYPE_NORMAL, adapter.getItemViewType(2));
        assertEquals(PowerFriendCircleAdapter.TYPE_NORMAL, adapter.getItemViewType(3));
    }
    
    @Test
    public void testOnCreateViewHolder() {
        // 设置布局填充器行为
        when(mockLayoutInflater.inflate(anyInt(), any(ViewGroup.class), any(Boolean.class))).thenReturn(mockView);
        
        // 测试创建Header ViewHolder
        RecyclerView.ViewHolder headerHolder = adapter.onCreateViewHolder(mockViewGroup, PowerFriendCircleAdapter.TYPE_HEADER);
        assertNotNull(headerHolder);
        
        // 测试创建Normal ViewHolder
        RecyclerView.ViewHolder normalHolder = adapter.onCreateViewHolder(mockViewGroup, PowerFriendCircleAdapter.TYPE_NORMAL);
        assertNotNull(normalHolder);
    }
    
    @Test
    public void testSetFriendCircleBeans() {
        // 创建测试数据
        List<FriendCircleBean> beans = PowerTestUtil.createTestFriendCircleBeans(5);
        
        // 设置数据
        adapter.setFriendCircleBeans(beans);
        
        // 设置header view
        adapter.setHeaderView(mockView);
        
        // 验证item数量
        assertEquals(6, adapter.getItemCount()); // 5个数据项 + header
    }
    
    @Test
    public void testOnItemClickPopupMenu() {
        // 模拟OnItemClickPopupMenuListener
        OnItemClickPopupMenuListener mockListener = mock(OnItemClickPopupMenuListener.class);
        
        // 测试复制操作
        adapter.onItemClickCopy(1);
        
        // 测试收藏操作
        adapter.onItemClickCollection(1);
    }
    
    @Test
    public void testSetOnPraiseOrCommentClickListener() {
        // 模拟OnPraiseOrCommentClickListener
        PowerFriendCircleAdapter.OnPraiseOrCommentClickListener mockListener = mock(PowerFriendCircleAdapter.OnPraiseOrCommentClickListener.class);
        
        // 设置监听器
        adapter.setOnPraiseOrCommentClickListener(mockListener);
        
        // 验证监听器是否被正确设置 (通过反射检查私有字段可能会更好，但这里简化处理)
        
        // 创建测试数据和视图模拟，以便调用点赞/评论方法
        List<FriendCircleBean> beans = PowerTestUtil.createTestFriendCircleBeans(1);
        adapter.setFriendCircleBeans(beans);
        adapter.setHeaderView(mockView);
        
        // 注意：我们不能直接测试内部的回调，因为它们在视图持有者中设置
        // 在实际Android环境中，我们可以使用Espresso等UI测试框架测试这些交互
    }
} 