package com.example.wechatfriendforperformance;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.example.wechatfriendforperformance.beans.CommentBean;
import com.example.wechatfriendforperformance.beans.FriendCircleBean;
import com.example.wechatfriendforperformance.beans.OtherInfoBean;
import com.example.wechatfriendforperformance.beans.PraiseBean;
import com.example.wechatfriendforperformance.beans.UserBean;
import com.example.wechatfriendforperformance.utils.PerformanceSpanUtils;
import com.example.wechatfriendforperformance.adapters.PerformanceFriendCircleAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.wechatfriendforperformance.PerformanceConstants.*;

/**
 * Performance test data center, generate fixed test data
 */
public class PerformanceDataCenter {

    private static final String TAG = "PerformanceDataCenter";

    private static final PerformanceDataCenter instance = new PerformanceDataCenter();
    private List<FriendCircleBean> cachedLightLoadFriendCircleBeans;
    private List<FriendCircleBean> cachedMediumLoadFriendCircleBeans;
    private List<FriendCircleBean> cachedHeavyLoadFriendCircleBeans;
    
    // Fixed avatar resource name array
    private static final String[] AVATAR_RES_NAMES = {
            "avatar1", "avatar2", "avatar3", "avatar4", "avatar5",
            "avatar6", "avatar7", "avatar8", "avatar9", "avatar10",
            "avatar11"
    };
    
    private PerformanceDataCenter() {
        // Initialization operations, if needed
    }
    
    public static PerformanceDataCenter getInstance() {
        return instance;
    }
    
    /**
     * 清除缓存的数据，确保下次获取数据时重新生成
     */
    public void clearCachedData() {
        Log.d(TAG, "clearCachedData: 清除所有缓存数据");
        cachedLightLoadFriendCircleBeans = null;
        cachedMediumLoadFriendCircleBeans = null;
        cachedHeavyLoadFriendCircleBeans = null;
    }
    
    public List<FriendCircleBean> getFriendCircleBeans() {
        return getFriendCircleBeans(PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
    }

    public List<FriendCircleBean> getFriendCircleBeans(int loadType) {
        String loadTypeStr = "";
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                loadTypeStr = "轻负载";
                if (cachedLightLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 生成轻负载数据");
                    cachedLightLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                }
                return cachedLightLoadFriendCircleBeans;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                loadTypeStr = "中负载";
                if (cachedMediumLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 生成中负载数据");
                    cachedMediumLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                }
                return cachedMediumLoadFriendCircleBeans;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                loadTypeStr = "高负载";
                if (cachedHeavyLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 生成高负载数据");
                    cachedHeavyLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                }
                return cachedHeavyLoadFriendCircleBeans;
            default:
                loadTypeStr = "未知负载，使用轻负载";
                if (cachedLightLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 生成默认轻负载数据");
                    cachedLightLoadFriendCircleBeans = generateFriendCircleBeans(PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
                }
                return cachedLightLoadFriendCircleBeans;
        }
    }
    
    private List<FriendCircleBean> generateFriendCircleBeans(int loadType) {
        // Generate fixed 100 friend circle items
        List<FriendCircleBean> friendCircleBeans = new ArrayList<>();
        Log.d(TAG, "generateFriendCircleBeans: 开始生成负载类型=" + loadType + "的朋友圈数据");
        
        for (int i = 0; i < 100; i++) {
            FriendCircleBean bean = new FriendCircleBean();
            // Set view type, cycling through three types
            int viewType = i % 3;
            bean.setViewType(viewType);
            
            // Set content, cycling through predefined contents
            bean.setContent(CONTENTS[i % CONTENTS.length]);
            
            // Set comments, cycling comment count based on position and load type
            List<CommentBean> commentBeans = generateCommentBeans(i, loadType);
            bean.setCommentBeans(commentBeans);
            
            // Set likes, cycling like count based on position and load type
            List<PraiseBean> praiseBeans = generatePraiseBeans(i, loadType);
            bean.setPraiseBeans(praiseBeans);
            
            // Set images, cycling through 1, 3, 6, 9 images based on index
            List<String> imageUrls = new ArrayList<>();
            generateImageUrls(imageUrls, i);
            bean.setImageUrls(imageUrls);
            
            // Set user information
            UserBean userBean = new UserBean();
            userBean.setUserId(String.valueOf(i));
            userBean.setUserName(USER_NAMES[i % USER_NAMES.length]);
            
            // Cycle through avatar1-avatar11 as avatars based on position
            int avatarIndex = i % AVATAR_RES_NAMES.length;
            userBean.setUserAvatarUrl(AVATAR_RES_NAMES[avatarIndex]);
            bean.setUserBean(userBean);
            
            // Set other information
            OtherInfoBean otherInfoBean = new OtherInfoBean();
            otherInfoBean.setTime(TIMES[i % TIMES.length]);
            otherInfoBean.setSource(SOURCES[i % SOURCES.length]);
            
            // Set location information, randomly choose whether to display location
            if (i % 3 == 0) { // One third probability to show location
                otherInfoBean.setLocation(LOCATIONS[i % LOCATIONS.length]);
            }
            
            bean.setOtherInfoBean(otherInfoBean);
            friendCircleBeans.add(bean);
        }
        return friendCircleBeans;
    }
    
    private void generateImageUrls(List<String> imageUrls, int position) {
        // Determine image count based on position: cycling through 1, 3, 6, 9 images
        int imageCount;
        int remainder = position % 4;
        switch (remainder) {
            case 0:
                imageCount = 1;
                break;
            case 1:
                imageCount = 3;
                break;
            case 2:
                imageCount = 6;
                break;
            case 3:
                imageCount = 9;
                break;
            default:
                imageCount = 1;
                break;
        }
        
        // Add images
        for (int i = 0; i < imageCount; i++) {
            // Each position uses different subset of images to increase variety
            int imageIndex = (position + i) % IMAGE_RESOURCES.length;
            imageUrls.add(IMAGE_RESOURCES[imageIndex]);
        }
    }
    
    private List<CommentBean> generateCommentBeans(int position, int loadType) {
        List<CommentBean> commentBeans = new ArrayList<>();
        
        // Comment count based on load type
        int commentCount;
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                commentCount = position % 3; // 0-2 comments for light load
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                commentCount = position % 8 + 5; // 5-12 comments for medium load
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                commentCount = position % 15 + 10; // 10-24 comments for heavy load
                break;
            default:
                commentCount = position % 3;
                break;
        }
        
        // 打印日志，记录评论数量
        Log.d(TAG, "generateCommentBeans: position=" + position + ", loadType=" + loadType + ", commentCount=" + commentCount);
        
        if (commentCount == 0) {
            return commentBeans; // No comments
        }
        
        for (int i = 0; i < commentCount; i++) {
            CommentBean commentBean = new CommentBean();
            // To increase interactivity, even indices are single comments, odd indices are reply comments
            if (i % 2 == 0) {
                commentBean.setCommentType(COMMENT_TYPE_SINGLE);
                int contentIndex = (position + i) % COMMENT_CONTENTS.length;
                UserBean userBean = new UserBean();
                userBean.setUserId(String.valueOf(position * 10 + i));
                userBean.setUserName(USER_NAMES[(position + i) % USER_NAMES.length]);
                userBean.setUserAvatarUrl(AVATAR_RES_NAMES[i % AVATAR_RES_NAMES.length]);
                commentBean.setFromUserBean(userBean);
                
                // Set comment content
                commentBean.setContent(COMMENT_CONTENTS[contentIndex]);
            } else {
                commentBean.setCommentType(COMMENT_TYPE_REPLY);
                int contentIndex = (position + i) % COMMENT_CONTENTS.length;
                
                UserBean fromUserBean = new UserBean();
                fromUserBean.setUserId(String.valueOf(position * 10 + i));
                fromUserBean.setUserName(USER_NAMES[(position + i + 1) % USER_NAMES.length]);
                fromUserBean.setUserAvatarUrl(AVATAR_RES_NAMES[(i + 1) % AVATAR_RES_NAMES.length]);
                commentBean.setFromUserBean(fromUserBean);
                
                UserBean toUserBean = new UserBean();
                toUserBean.setUserId(String.valueOf(position * 10 + i - 1));
                toUserBean.setUserName(USER_NAMES[(position + i - 1) % USER_NAMES.length]);
                toUserBean.setUserAvatarUrl(AVATAR_RES_NAMES[(i - 1) % AVATAR_RES_NAMES.length]);
                commentBean.setToUserBean(toUserBean);
                
                commentBean.setContent(COMMENT_CONTENTS[contentIndex]);
            }
            commentBeans.add(commentBean);
        }
        return commentBeans;
    }
    
    private List<PraiseBean> generatePraiseBeans(int position, int loadType) {
        List<PraiseBean> praiseBeans = new ArrayList<>();
        
        // Like count based on load type
        int praiseCount;
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                praiseCount = position % 3 + 1; // 1-3 likes for light load
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                praiseCount = position % 10 + 5; // 5-14 likes for medium load
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                praiseCount = position % 20 + 15; // 15-34 likes for heavy load
                break;
            default:
                praiseCount = position % 3 + 1;
                break;
        }
        
        // 打印日志，记录点赞数量
        Log.d(TAG, "generatePraiseBeans: position=" + position + ", loadType=" + loadType + ", praiseCount=" + praiseCount);
        
        for (int i = 0; i < praiseCount; i++) {
            PraiseBean praiseBean = new PraiseBean();
            UserBean userBean = new UserBean();
            userBean.setUserId(String.valueOf(position * 10 + i));
            userBean.setUserName(USER_NAMES[(position + i) % USER_NAMES.length]);
            userBean.setUserAvatarUrl(AVATAR_RES_NAMES[i % AVATAR_RES_NAMES.length]);
            praiseBean.setUserBean(userBean);
            praiseBeans.add(praiseBean);
        }
        return praiseBeans;
    }
} 