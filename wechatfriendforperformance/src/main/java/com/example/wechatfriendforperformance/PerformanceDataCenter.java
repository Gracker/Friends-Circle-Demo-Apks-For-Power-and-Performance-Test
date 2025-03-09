package com.example.wechatfriendforperformance;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.example.wechatfriendforperformance.beans.CommentBean;
import com.example.wechatfriendforperformance.beans.FriendCircleBean;
import com.example.wechatfriendforperformance.beans.OtherInfoBean;
import com.example.wechatfriendforperformance.beans.PraiseBean;
import com.example.wechatfriendforperformance.beans.UserBean;
import com.example.wechatfriendforperformance.utils.PerformanceSpanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能测试专用的数据中心，生成固定的测试数据
 */
public class PerformanceDataCenter {

    // 固定的头像资源名称数组
    public static final String[] AVATAR_URLS = {
            "avatar1", "avatar2", "avatar3", "avatar4", "avatar5", 
            "avatar6", "avatar7", "avatar8", "avatar9", "avatar10", "avatar11"
    };

    public static void init() {
        // 初始化操作，如果需要的话
    }

    /**
     * 生成固定的朋友圈数据，每次都返回相同的数据
     */
    public static List<FriendCircleBean> makeFriendCircleBeans(Context context) {
        List<FriendCircleBean> friendCircleBeans = new ArrayList<>();
        
        // 生成固定的100个朋友圈项目
        for (int i = 0; i < 100; i++) {
            FriendCircleBean friendCircleBean = new FriendCircleBean();
            
            // 设置视图类型，循环使用三种类型
            int viewType = i % 3;
            friendCircleBean.setViewType(viewType);
            
            // 设置内容，循环使用预定义的内容
            int contentIndex = i % PerformanceConstants.CONTENTS.length;
            friendCircleBean.setContent(PerformanceConstants.CONTENTS[contentIndex]);
            
            // 设置评论，根据位置循环变化评论数量
            friendCircleBean.setCommentBeans(makeCommentBeans(context, i));
            
            // 设置点赞，根据位置循环变化点赞数量
            List<PraiseBean> praiseBeans = makePraiseBeans(i);
            friendCircleBean.setPraiseSpan(PerformanceSpanUtils.makePraiseSpan(context, praiseBeans));
            friendCircleBean.setPraiseBeans(praiseBeans);
            
            // 设置图片，根据索引循环使用1、3、6、9张图片
            List<String> imageUrls = makeImageUrls(i);
            friendCircleBean.setImageUrls(imageUrls);
            
            // 设置用户信息
            UserBean userBean = new UserBean();
            int userIndex = i % PerformanceConstants.USER_NAMES.length;
            userBean.setUserName(PerformanceConstants.USER_NAMES[userIndex]);
            
            // 根据位置轮流使用avatar1-avatar11作为头像
            int avatarIndex = i % AVATAR_URLS.length;
            userBean.setUserAvatarUrl(AVATAR_URLS[avatarIndex]);
            
            userBean.setUserId(i);
            friendCircleBean.setUserBean(userBean);
            
            // 设置其他信息
            OtherInfoBean otherInfoBean = new OtherInfoBean();
            int timeIndex = i % PerformanceConstants.TIMES.length;
            otherInfoBean.setTime(PerformanceConstants.TIMES[timeIndex]);
            int sourceIndex = i % PerformanceConstants.SOURCES.length;
            otherInfoBean.setSource(PerformanceConstants.SOURCES[sourceIndex]);

            // 设置位置信息，随机选择是否显示位置
            if (i % 3 == 0) { // 三分之一的概率显示位置
                int locationIndex = i % PerformanceConstants.LOCATIONS.length;
                otherInfoBean.setLocation(PerformanceConstants.LOCATIONS[locationIndex]);
            }

            friendCircleBean.setOtherInfoBean(otherInfoBean);
            
            friendCircleBeans.add(friendCircleBean);
        }
        
        return friendCircleBeans;
    }
    
    /**
     * 生成图片URL列表，根据索引循环使用1、3、6、9张图片
     */
    private static List<String> makeImageUrls(int position) {
        List<String> imageUrls = new ArrayList<>();
        
        // 根据位置决定图片数量：1、3、6、9张循环
        int imageCount;
        switch (position % 4) {
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
        }
        
        // 添加图片
        for (int i = 0; i < imageCount; i++) {
            int imageIndex = (position + i) % PerformanceConstants.SINGLE_IMAGE_URLS.length;
            imageUrls.add(PerformanceConstants.SINGLE_IMAGE_URLS[imageIndex]);
        }
        
        return imageUrls;
    }

    /**
     * 生成评论数据，根据位置循环变化评论数量
     */
    private static List<CommentBean> makeCommentBeans(Context context, int position) {
        List<CommentBean> commentBeans = new ArrayList<>();
        
        // 评论数量循环变化：0-5条
        int commentCount = position % 6;
        
        for (int i = 0; i < commentCount; i++) {
            CommentBean commentBean = new CommentBean();
            
            // 为了增加互动性，偶数索引为单条评论，奇数索引为回复评论
            if (i % 2 == 0) {
                commentBean.setCommentType(PerformanceConstants.CommentType.COMMENT_TYPE_SINGLE);
                int userIndex = (position + i) % PerformanceConstants.USER_NAMES.length;
                commentBean.setChildUserName(PerformanceConstants.USER_NAMES[userIndex]);
            } else {
                commentBean.setCommentType(PerformanceConstants.CommentType.COMMENT_TYPE_REPLY);
                int childUserIndex = (position + i) % PerformanceConstants.USER_NAMES.length;
                commentBean.setChildUserName(PerformanceConstants.USER_NAMES[childUserIndex]);
                int parentUserIndex = (position + i + 1) % PerformanceConstants.USER_NAMES.length;
                commentBean.setParentUserName(PerformanceConstants.USER_NAMES[parentUserIndex]);
            }
            
            // 设置评论内容
            int contentIndex = (position + i) % PerformanceConstants.COMMENT_CONTENTS.length;
            commentBean.setCommentContent(PerformanceConstants.COMMENT_CONTENTS[contentIndex]);
            commentBean.build(context);
            
            commentBeans.add(commentBean);
        }
        
        return commentBeans;
    }

    /**
     * 生成点赞数据，根据位置循环变化点赞数量
     */
    private static List<PraiseBean> makePraiseBeans(int position) {
        List<PraiseBean> praiseBeans = new ArrayList<>();
        
        // 点赞数量循环变化：1-8个
        int praiseCount = position % 8 + 1;
        
        for (int i = 0; i < praiseCount; i++) {
            PraiseBean praiseBean = new PraiseBean();
            int userIndex = (position + i) % PerformanceConstants.USER_NAMES.length;
            praiseBean.setPraiseUserName(PerformanceConstants.USER_NAMES[userIndex]);
            praiseBean.setPraiseUserId(String.valueOf(userIndex));
            praiseBeans.add(praiseBean);
        }
        
        return praiseBeans;
    }
} 