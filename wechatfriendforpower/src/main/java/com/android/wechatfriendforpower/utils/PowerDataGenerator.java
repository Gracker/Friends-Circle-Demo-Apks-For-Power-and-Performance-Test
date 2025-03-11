package com.android.wechatfriendforpower.utils;

import android.content.Context;

import com.android.wechatfriendforpower.beans.CommentBean;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.beans.OtherInfoBean;
import com.android.wechatfriendforpower.beans.PraiseBean;
import com.android.wechatfriendforpower.beans.UserBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 数据生成器，用于生成模拟数据
 */
public class PowerDataGenerator {
    
    private final Context context;
    private final Random random = new Random();
    
    // 示例头像URL
    private final String[] avatarUrls = {
            "https://example.com/avatar1.png",
            "https://example.com/avatar2.png",
            "https://example.com/avatar3.png",
            "https://example.com/avatar4.png",
            "https://example.com/avatar5.png"
    };
    
    // 示例图片URL
    private final String[] imageUrls = {
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg",
            "https://example.com/image3.jpg",
            "https://example.com/image4.jpg",
            "https://example.com/image5.jpg",
            "https://example.com/image6.jpg",
            "https://example.com/image7.jpg",
            "https://example.com/image8.jpg",
            "https://example.com/image9.jpg"
    };
    
    // 示例内容文本
    private final String[] contents = {
            "今天天气真好，出去玩了一天，感觉心情舒畅！",
            "分享一篇很有启发的文章，推荐给大家阅读。",
            "这是一段很长的文本，超过了显示限制，需要点击查看全文才能看完。这是一段很长的文本，超过了显示限制，需要点击查看全文才能看完。这是一段很长的文本，超过了显示限制，需要点击查看全文才能看完。",
            "刚刚参加了一个有趣的活动，认识了很多新朋友。",
            "新学了一道菜，很好吃，下次有机会分享给大家。"
    };
    
    public PowerDataGenerator(Context context) {
        this.context = context;
    }
    
    /**
     * 生成模拟朋友圈数据
     * @param count 生成的数据条数
     * @return 朋友圈数据列表
     */
    public List<FriendCircleBean> generateFakeFriendCircleData(int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        List<FriendCircleBean> result = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            FriendCircleBean bean = new FriendCircleBean();
            
            // 设置用户信息
            bean.setUserBean(generateUser());
            
            // 设置内容
            bean.setContent(generateContent());
            
            // 设置图片（50%的概率有图片）
            if (random.nextBoolean()) {
                int imageCount = random.nextInt(9) + 1; // 1-9张图片
                bean.setImageUrls(generateImages(imageCount));
            }
            
            // 设置点赞（70%的概率有点赞）
            if (random.nextFloat() < 0.7f) {
                int praiseCount = random.nextInt(10) + 1; // 1-10个点赞
                bean.setPraiseBeans(generatePraises(praiseCount));
            }
            
            // 设置评论（60%的概率有评论）
            if (random.nextFloat() < 0.6f) {
                int commentCount = random.nextInt(5) + 1; // 1-5条评论
                bean.setCommentBeans(generateComments(commentCount));
            }
            
            // 设置其他信息
            bean.setOtherInfoBean(generateOtherInfo());
            
            result.add(bean);
        }
        
        return result;
    }
    
    /**
     * 生成用户数据
     * @return 用户数据
     */
    public UserBean generateUser() {
        UserBean userBean = new UserBean();
        int userId = random.nextInt(10000);
        userBean.setUserId(String.valueOf(userId));
        userBean.setUserName("用户" + userId);
        userBean.setUserAvatarUrl(avatarUrls[random.nextInt(avatarUrls.length)]);
        return userBean;
    }
    
    /**
     * 生成点赞数据
     * @param count 点赞数量
     * @return 点赞数据列表
     */
    public List<PraiseBean> generatePraises(int count) {
        List<PraiseBean> result = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            PraiseBean praiseBean = new PraiseBean();
            praiseBean.setPraiseUserName("点赞用户" + i);
            praiseBean.setPraiseUserId(String.valueOf(random.nextInt(10000)));
            result.add(praiseBean);
        }
        
        return result;
    }
    
    /**
     * 生成评论数据
     * @param count 评论数量
     * @return 评论数据列表
     */
    public List<CommentBean> generateComments(int count) {
        List<CommentBean> result = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            CommentBean commentBean = new CommentBean();
            
            // 创建评论者
            UserBean childUser = new UserBean();
            childUser.setUserId(String.valueOf(random.nextInt(10000)));
            childUser.setUserName("评论用户" + i);
            childUser.setUserAvatarUrl(avatarUrls[random.nextInt(avatarUrls.length)]);
            commentBean.setChildUserBean(childUser);
            
            // 50%的概率是回复评论
            if (i > 0 && random.nextBoolean()) {
                UserBean parentUser = new UserBean();
                parentUser.setUserId(String.valueOf(random.nextInt(10000)));
                parentUser.setUserName("被回复用户" + (i - 1));
                parentUser.setUserAvatarUrl(avatarUrls[random.nextInt(avatarUrls.length)]);
                commentBean.setParentUserBean(parentUser);
            }
            
            commentBean.setContent("这是第" + i + "条评论内容，随机生成的文本。");
            result.add(commentBean);
        }
        
        return result;
    }
    
    /**
     * 生成图片URL列表
     * @param count 图片数量
     * @return 图片URL列表
     */
    public List<String> generateImages(int count) {
        List<String> result = new ArrayList<>(count);
        
        for (int i = 0; i < count && i < imageUrls.length; i++) {
            result.add(imageUrls[i]);
        }
        
        return result;
    }
    
    /**
     * 生成其他信息
     * @return 其他信息
     */
    public OtherInfoBean generateOtherInfo() {
        OtherInfoBean otherInfoBean = new OtherInfoBean();
        otherInfoBean.setTime(PowerUtils.formatTimeString(System.currentTimeMillis() - random.nextInt(86400000))); // 随机24小时内
        
        String[] sources = {"Android客户端", "iOS客户端", "网页版"};
        otherInfoBean.setSource(sources[random.nextInt(sources.length)]);
        
        String[] locations = {"北京市", "上海市", "广州市", "深圳市", "杭州市"};
        if (random.nextBoolean()) { // 50%的概率有位置信息
            otherInfoBean.setLocation(locations[random.nextInt(locations.length)]);
        }
        
        return otherInfoBean;
    }
    
    /**
     * 生成朋友圈内容
     * @return 内容文本
     */
    public String generateContent() {
        return contents[random.nextInt(contents.length)];
    }
} 