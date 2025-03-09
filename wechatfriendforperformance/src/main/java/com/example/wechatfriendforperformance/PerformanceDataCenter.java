package com.example.wechatfriendforperformance;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.widget.Toast;

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
    
    // 使用一个固定种子的随机数生成器，确保生成的数据是可重现的
    private Random mRandom = new Random(42);
    
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
                if (cachedLightLoadFriendCircleBeans == null) {
                    cachedLightLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                } else {
                }
                return cachedLightLoadFriendCircleBeans;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                if (cachedMediumLoadFriendCircleBeans == null) {
                    cachedMediumLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                } else {
                }
                return cachedMediumLoadFriendCircleBeans;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                if (cachedHeavyLoadFriendCircleBeans == null) {
                    cachedHeavyLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                } else {
                }
                return cachedHeavyLoadFriendCircleBeans;
            default:
                if (cachedLightLoadFriendCircleBeans == null) {
                    cachedLightLoadFriendCircleBeans = generateFriendCircleBeans(PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
                } else {
                }
                return cachedLightLoadFriendCircleBeans;
        }
    }
    
    /**
     * 打印数据统计信息
     */
    private void printStatistics(List<FriendCircleBean> beans, int loadType) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        
        String loadTypeStr = getLoadTypeString(loadType);
        
        int totalPraise = 0;
        int totalComment = 0;
        int maxPraise = 0;
        int maxComment = 0;
        
        for (FriendCircleBean bean : beans) {
            int praiseCount = bean.getPraiseBeans() != null ? bean.getPraiseBeans().size() : 0;
            int commentCount = bean.getCommentBeans() != null ? bean.getCommentBeans().size() : 0;
            
            totalPraise += praiseCount;
            totalComment += commentCount;
            maxPraise = Math.max(maxPraise, praiseCount);
            maxComment = Math.max(maxComment, commentCount);
        }
    }
    
    /**
     * 生成朋友圈数据
     */
    private List<FriendCircleBean> generateFriendCircleBeans(int loadType) {
        List<FriendCircleBean> friendCircleBeans = new ArrayList<>();
        
        // 初始化随机数生成器
        long randomSeed = 42;
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                randomSeed = 42;
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                randomSeed = 142;
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                randomSeed = 242;
                break;
        }
        
        mRandom = new Random(randomSeed);
        
        // 确保生成固定数量的朋友圈数据
        for (int i = 0; i < FRIEND_CIRCLE_COUNT; i++) {
            FriendCircleBean friendCircleBean = new FriendCircleBean();
            
            // User information
            UserBean userBean = new UserBean();
            userBean.setUserName(randomUserNickname(i));
            userBean.setUserId(String.valueOf(10000 + i));
            
            // 获取头像资源ID
            String avatarResName = AVATAR_RES_NAMES[i % AVATAR_RES_NAMES.length];
            userBean.setUserAvatarUrl(avatarResName);
            
            friendCircleBean.setUserBean(userBean);
            
            // Content and other info
            String content = getRandomContent(i);
            friendCircleBean.setContentSpan(content);
            
            // Set publish time
            OtherInfoBean otherInfoBean = new OtherInfoBean();
            otherInfoBean.setTime(getRandomPublishTime(i));
            
            // Set post source if position is appropriate
            if (i % 4 == 0) {
                otherInfoBean.setSource(getRandomSource(i));
            }
            
            // Set location if position is appropriate
            if (i % 3 == 0) {
                otherInfoBean.setLocation(getRandomLocation(i));
            }
            
            friendCircleBean.setOtherInfoBean(otherInfoBean);
            
            // Add images if position is appropriate
            if (i % 2 != 0) {
                List<String> imageUrls = new ArrayList<>();
                generateImageUrls(imageUrls, i);
                friendCircleBean.setImageUrls(imageUrls);
            }
            
            // Generate comment and praise data for different load types
            int positionOffset = 0;
            switch (loadType) {
                case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                    positionOffset = 0;
                    break;
                case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                    positionOffset = 100;
                    break;
                case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                    positionOffset = 200;
                    break;
            }
            
            // Generate praises for this item
            List<PraiseBean> praiseBeans = generatePraiseBeans(i + positionOffset, loadType);
            friendCircleBean.setPraiseBeans(praiseBeans);
            
            // Generate comments for this item
            List<CommentBean> commentBeans = generateCommentBeans(i + positionOffset, loadType);
            friendCircleBean.setCommentBeans(commentBeans);
            
            // Add to list
            friendCircleBeans.add(friendCircleBean);
        }
        
        return friendCircleBeans;
    }
    
    private void generateImageUrls(List<String> imageUrls, int position) {
        if (imageUrls == null) {
            return;
        }
        
        int count = Math.min(9, (position % 9) + 1);
        
        for (int i = 0; i < count; i++) {
            // Position based deterministic image url
            int imageNumber = (position + i) % 20 + 1;
            String imageName = "picture" + imageNumber;
            imageUrls.add(imageName);
        }
    }
    
    /**
     * 根据负载类型生成评论数据
     */
    private List<CommentBean> generateCommentBeans(int position, int loadType) {
        // 根据不同的负载类型，生成不同数量的评论
        int commentCount = 0;
        
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                // 轻负载: 0-2条评论
                commentCount = position % 3;
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                // 中负载: 5-12条评论
                commentCount = position % 8 + 5;
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                // 高负载: 10-24条评论
                commentCount = position % 15 + 10;
                break;
            default:
                // 默认为轻负载
                commentCount = position % 3;
                break;
        }
        
        List<CommentBean> commentBeans = new ArrayList<>();
        
        if (commentCount <= 0) {
            return commentBeans;
        }
        
        // 确保随机数生成的可重现性
        Random random = new Random(position * 100 + loadType * 10);
        
        for (int i = 0; i < commentCount; i++) {
            CommentBean commentBean = new CommentBean();
            
            // 评论发起人
            UserBean childUserBean = new UserBean();
            childUserBean.setUserId(String.valueOf(20000 + i + position * 100));
            childUserBean.setUserName(randomCommentUserName(i, position));
            childUserBean.setUserAvatarUrl(AVATAR_RES_NAMES[i % AVATAR_RES_NAMES.length]);
            commentBean.setChildUserBean(childUserBean);
            
            // 是否是回复别人的评论
            boolean isReply = random.nextInt(10) < 3; // 30%的概率是回复他人
            
            if (isReply && i > 0) {
                // 被回复的用户
                UserBean parentUserBean = new UserBean();
                int replyToIndex = random.nextInt(i);
                parentUserBean.setUserId(String.valueOf(20000 + replyToIndex + position * 100));
                parentUserBean.setUserName(randomCommentUserName(replyToIndex, position));
                parentUserBean.setUserAvatarUrl(AVATAR_RES_NAMES[replyToIndex % AVATAR_RES_NAMES.length]);
                commentBean.setParentUserBean(parentUserBean);
            }
            
            // 评论内容
            String commentContent = PerformanceConstants.COMMENT_CONTENTS[
                    (position + i) % PerformanceConstants.COMMENT_CONTENTS.length];
            commentBean.setContent(commentContent);
            
            // 构建评论文本
            commentBean.build();
            
            commentBeans.add(commentBean);
        }
        
        return commentBeans;
    }
    
    /**
     * 根据负载类型生成点赞数据
     */
    private List<PraiseBean> generatePraiseBeans(int position, int loadType) {
        // 根据不同的负载类型，生成不同数量的点赞
        int praiseCount = 0;
        
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                // 轻负载: 0-5个点赞
                praiseCount = position % 6;
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                // 中负载: 5-12个点赞
                praiseCount = position % 8 + 5;
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                // 高负载: 10-20个点赞
                praiseCount = position % 11 + 10;
                break;
            default:
                // 默认为轻负载
                praiseCount = position % 6;
                break;
        }
        
        List<PraiseBean> praiseBeans = new ArrayList<>();
        
        if (praiseCount <= 0) {
            return praiseBeans;
        }
        
        for (int i = 0; i < praiseCount; i++) {
            PraiseBean praiseBean = new PraiseBean();
            
            UserBean userBean = new UserBean();
            userBean.setUserId(String.valueOf(30000 + i + position * 100));
            userBean.setUserName(randomPraiseUserName(i, position));
            userBean.setUserAvatarUrl(AVATAR_RES_NAMES[i % AVATAR_RES_NAMES.length]);
            
            praiseBean.setUserBean(userBean);
            praiseBeans.add(praiseBean);
        }
        
        return praiseBeans;
    }
    
    /**
     * 直接生成指定负载类型的数据，不使用缓存
     */
    public List<FriendCircleBean> generateDataForLoadType(int loadType) {
        String loadTypeStr = getLoadTypeString(loadType);
        
        // 生成数据并返回
        List<FriendCircleBean> friendCircleBeans = generateFriendCircleBeans(loadType);
        
        // 打印统计信息
        printStatistics(friendCircleBeans, loadType);
        
        return friendCircleBeans;
    }
    
    private String getLoadTypeString(int loadType) {
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                return "轻负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                return "中负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                return "高负载";
            default:
                return "未知负载";
        }
    }
} 