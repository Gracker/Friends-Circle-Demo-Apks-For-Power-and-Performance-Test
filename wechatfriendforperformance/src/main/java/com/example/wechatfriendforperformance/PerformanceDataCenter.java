package com.example.wechatfriendforperformance;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;
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
        Log.d(TAG, "PerformanceDataCenter初始化");
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
                
                // 为了调试，每次都重新生成数据
                if (true || cachedLightLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 强制生成轻负载数据，loadType=" + loadType);
                    cachedLightLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                    // 打印点赞和评论数量统计
                    printStatistics(cachedLightLoadFriendCircleBeans, loadType);
                } else {
                    Log.d(TAG, "getFriendCircleBeans: 使用缓存的轻负载数据");
                }
                return cachedLightLoadFriendCircleBeans;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                loadTypeStr = "中负载";
                
                // 为了调试，每次都重新生成数据
                if (true || cachedMediumLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 强制生成中负载数据，loadType=" + loadType);
                    cachedMediumLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                    // 打印点赞和评论数量统计
                    printStatistics(cachedMediumLoadFriendCircleBeans, loadType);
                } else {
                    Log.d(TAG, "getFriendCircleBeans: 使用缓存的中负载数据");
                }
                return cachedMediumLoadFriendCircleBeans;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                loadTypeStr = "高负载";
                
                // 为了调试，每次都重新生成数据
                if (true || cachedHeavyLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 强制生成高负载数据，loadType=" + loadType);
                    cachedHeavyLoadFriendCircleBeans = generateFriendCircleBeans(loadType);
                    // 打印点赞和评论数量统计
                    printStatistics(cachedHeavyLoadFriendCircleBeans, loadType);
                } else {
                    Log.d(TAG, "getFriendCircleBeans: 使用缓存的高负载数据");
                }
                return cachedHeavyLoadFriendCircleBeans;
            default:
                loadTypeStr = "未知负载，使用轻负载";
                
                // 为了调试，每次都重新生成数据
                if (true || cachedLightLoadFriendCircleBeans == null) {
                    Log.d(TAG, "getFriendCircleBeans: 强制生成默认轻负载数据，loadType=" + PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
                    cachedLightLoadFriendCircleBeans = generateFriendCircleBeans(PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
                    // 打印点赞和评论数量统计
                    printStatistics(cachedLightLoadFriendCircleBeans, PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
                } else {
                    Log.d(TAG, "getFriendCircleBeans: 使用缓存的默认轻负载数据");
                }
                return cachedLightLoadFriendCircleBeans;
        }
    }
    
    /**
     * 打印生成数据的统计信息
     */
    private void printStatistics(List<FriendCircleBean> beans, int loadType) {
        if (beans == null || beans.isEmpty()) {
            Log.e(TAG, "printStatistics: 数据为空，无法统计");
            return;
        }
        
        String loadTypeStr;
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                loadTypeStr = "轻负载";
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                loadTypeStr = "中负载";
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                loadTypeStr = "高负载";
                break;
            default:
                loadTypeStr = "未知负载";
                break;
        }
        
        int totalPraise = 0;
        int totalComment = 0;
        int minPraise = Integer.MAX_VALUE;
        int maxPraise = 0;
        int minComment = Integer.MAX_VALUE;
        int maxComment = 0;
        
        for (FriendCircleBean bean : beans) {
            int praiseCount = bean.getPraiseBeans() != null ? bean.getPraiseBeans().size() : 0;
            int commentCount = bean.getCommentBeans() != null ? bean.getCommentBeans().size() : 0;
            
            totalPraise += praiseCount;
            totalComment += commentCount;
            
            minPraise = Math.min(minPraise, praiseCount);
            maxPraise = Math.max(maxPraise, praiseCount);
            
            minComment = Math.min(minComment, commentCount);
            maxComment = Math.max(maxComment, commentCount);
        }
        
        double avgPraise = (double) totalPraise / beans.size();
        double avgComment = (double) totalComment / beans.size();
        
        Log.d(TAG, "==================== " + loadTypeStr + " 统计信息 ====================");
        Log.d(TAG, "数据条数: " + beans.size());
        Log.d(TAG, "点赞数量: 总计=" + totalPraise + ", 平均=" + String.format("%.2f", avgPraise) + 
                ", 最小=" + minPraise + ", 最大=" + maxPraise);
        Log.d(TAG, "评论数量: 总计=" + totalComment + ", 平均=" + String.format("%.2f", avgComment) + 
                ", 最小=" + minComment + ", 最大=" + maxComment);
        Log.d(TAG, "=================================================");
    }
    
    private List<FriendCircleBean> generateFriendCircleBeans(int loadType) {
        // Generate fixed 100 friend circle items
        List<FriendCircleBean> friendCircleBeans = new ArrayList<>();
        Log.d(TAG, "generateFriendCircleBeans: 开始生成负载类型=" + loadType + "的朋友圈数据");
        
        // 确保随机数生成器使用固定种子，保证结果可重现
        mRandom = new Random(42 + loadType * 100);
        
        for (int i = 0; i < 100; i++) {
            FriendCircleBean bean = new FriendCircleBean();
            // Set view type, cycling through three types
            int viewType = i % 3;
            bean.setViewType(viewType);
            
            // Set content, cycling through predefined contents
            bean.setContent(CONTENTS[i % CONTENTS.length]);
            
            // 根据负载类型生成不同的position偏移量，
            // 确保生成不同数量的评论和点赞
            int positionOffset;
            switch (loadType) {
                case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                    positionOffset = i; // 轻负载使用原始位置
                    break;
                case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                    positionOffset = i + 100; // 中负载使用原始位置+100
                    break;
                case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                    positionOffset = i + 200; // 高负载使用原始位置+200
                    break;
                default:
                    positionOffset = i;
                    break;
            }
            
            // Set comments, cycling comment count based on position and load type
            List<CommentBean> commentBeans = generateCommentBeans(positionOffset, loadType);
            // 处理评论内容，设置必要的字段
            for (CommentBean commentBean : commentBeans) {
                if (commentBean.getFromUserBean() != null) {
                    commentBean.setChildUserName(commentBean.getFromUserBean().getUserName());
                }
                if (commentBean.getToUserBean() != null) {
                    commentBean.setParentUserName(commentBean.getToUserBean().getUserName());
                }
                commentBean.setCommentContent(commentBean.getContent());
                // 构建带样式的评论内容
                commentBean.build(null);
            }
            bean.setCommentBeans(commentBeans);
            
            // Set likes, cycling like count based on position and load type
            List<PraiseBean> praiseBeans = generatePraiseBeans(positionOffset, loadType);
            // 处理点赞内容，设置必要的字段
            for (PraiseBean praiseBean : praiseBeans) {
                if (praiseBean.getUserBean() != null) {
                    praiseBean.setPraiseUserName(praiseBean.getUserBean().getUserName());
                    praiseBean.setPraiseUserId(praiseBean.getUserBean().getUserId());
                }
            }
            bean.setPraiseBeans(praiseBeans);
            // 生成点赞文本样式
            bean.setPraiseSpan(PerformanceSpanUtils.makePraiseSpan(null, praiseBeans));
            
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
        
        Log.d(TAG, "generateFriendCircleBeans: 完成生成负载类型=" + loadType + "的朋友圈数据，数据条数=" + friendCircleBeans.size());
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
                
                // 生成时直接设置childUserName和commentContent
                commentBean.setChildUserName(userBean.getUserName());
                commentBean.setCommentContent(COMMENT_CONTENTS[contentIndex]);
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
                
                // 生成时直接设置childUserName、parentUserName和commentContent
                commentBean.setChildUserName(fromUserBean.getUserName());
                commentBean.setParentUserName(toUserBean.getUserName());
                commentBean.setCommentContent(COMMENT_CONTENTS[contentIndex]);
            }
            
            // 直接在这里构建评论内容样式
            commentBean.build(null);
            
            commentBeans.add(commentBean);
        }
        
        Log.d(TAG, "generateCommentBeans: 完成生成评论数据，数量=" + commentBeans.size());
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
            
            // 直接设置praiseUserName和praiseUserId
            praiseBean.setPraiseUserName(userBean.getUserName());
            praiseBean.setPraiseUserId(userBean.getUserId());
            
            praiseBeans.add(praiseBean);
        }
        
        Log.d(TAG, "generatePraiseBeans: 完成生成点赞数据，数量=" + praiseBeans.size());
        return praiseBeans;
    }

    /**
     * 直接生成指定负载类型的数据，不使用缓存
     * @param loadType 负载类型常量
     * @return 朋友圈数据列表
     */
    public List<FriendCircleBean> generateDataForLoadType(int loadType) {
        String loadTypeStr;
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                loadTypeStr = "轻负载";
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                loadTypeStr = "中负载";
                break;
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                loadTypeStr = "高负载";
                break;
            default:
                loadTypeStr = "未知负载";
                break;
        }
        
        Log.d(TAG, "generateDataForLoadType: 正在生成" + loadTypeStr + "数据");
        
        // 直接生成数据，不使用缓存
        List<FriendCircleBean> friendCircleBeans = generateFriendCircleBeans(loadType);
        
        // 打印统计信息
        printStatistics(friendCircleBeans, loadType);
        
        return friendCircleBeans;
    }
} 