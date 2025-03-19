package com.example.wechatfriendforwebview;

import android.content.Context;
import android.os.Trace;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * WebView版朋友圈数据中心
 * 负责生成和管理朋友圈数据
 */
public class WebViewDataCenter {
    private static final String TAG = "WebViewDataCenter";
    
    private static WebViewDataCenter instance;
    
    // 缓存的JSON数据
    private Map<Integer, String> cachedJsonData = new HashMap<>();
    
    // 私有构造函数
    private WebViewDataCenter() {
        // 单例模式，不做初始化
    }
    
    // 获取单例实例
    public static WebViewDataCenter getInstance() {
        if (instance == null) {
            synchronized (WebViewDataCenter.class) {
                if (instance == null) {
                    instance = new WebViewDataCenter();
                }
            }
        }
        return instance;
    }
    
    /**
     * 清除缓存的数据
     */
    public void clearCachedData() {
        cachedJsonData.clear();
    }
    
    /**
     * 根据负载类型获取朋友圈数据的JSON字符串
     * @param loadType 负载类型
     * @return JSON字符串
     */
    public String getFriendCircleJsonData(int loadType) {
        // 检查缓存中是否已有数据
        if (cachedJsonData.containsKey(loadType)) {
            return cachedJsonData.get(loadType);
        }
        
        Trace.beginSection("WebViewDataCenter_generateJsonData");
        
        // 根据负载类型确定生成的朋友圈数量
        int count;
        switch (loadType) {
            case WebViewMainActivity.LOAD_TYPE_LIGHT:
                count = 20;  // 轻负载，20条数据
                break;
            case WebViewMainActivity.LOAD_TYPE_MEDIUM:
                count = 50;  // 中负载，50条数据
                break;
            case WebViewMainActivity.LOAD_TYPE_HEAVY:
                count = 100; // 重负载，100条数据
                break;
            default:
                count = 30;
                break;
        }
        
        String jsonData = generateFriendCircleJsonData(count);
        
        // 缓存生成的数据
        cachedJsonData.put(loadType, jsonData);
        
        Trace.endSection();
        
        return jsonData;
    }
    
    /**
     * 生成特定数量的朋友圈数据JSON
     * @param count 数据条数
     * @return JSON字符串
     */
    private String generateFriendCircleJsonData(int count) {
        JSONArray friendCircleArray = new JSONArray();
        Random random = new Random(123); // 使用固定的种子值，确保每次生成的数据顺序一致
        
        try {
            // 第一条固定为"朋友圈"头部
            JSONObject header = new JSONObject();
            header.put("type", "header");
            header.put("avatar", "main_avatar.jpg");
            header.put("nickname", "朋友圈");
            friendCircleArray.put(header);
            
            // 生成朋友圈数据条目
            for (int i = 0; i < count; i++) {
                JSONObject item = new JSONObject();
                
                // 固定用户序号，确保每次相同位置显示相同的用户
                int userIndex = i % WebViewConstants.USER_NAMES.length;
                String username = WebViewConstants.USER_NAMES[userIndex];
                
                // 头像使用本地资源，确保1-11之间循环
                int avatarIndex = (i % 11) + 1;
                String avatar = "avatar" + avatarIndex + ".jpg";
                
                // 随机选择发布时间
                String publishTime = WebViewConstants.TIMES[random.nextInt(WebViewConstants.TIMES.length)];
                
                // 随机选择位置
                String location = null;
                if (random.nextBoolean()) {  // 50%概率有位置信息
                    location = WebViewConstants.LOCATIONS[random.nextInt(WebViewConstants.LOCATIONS.length)];
                }
                
                // 随机选择内容
                String content = WebViewConstants.CONTENTS[i % WebViewConstants.CONTENTS.length];
                
                // 固定图片数量和图片序号
                int imageCount = (i % 10); // 0-9张图片
                JSONArray images = new JSONArray();
                for (int j = 0; j < imageCount; j++) {
                    // 确保图片序号在1-11之间
                    int imageIndex = (j % 11) + 1;
                    String image = "local" + imageIndex + ".jpeg";
                    images.put(image);
                }
                
                // 随机选择评论数量 (0-10条)
                int commentCount = random.nextInt(11);  // 0-10
                JSONArray comments = new JSONArray();
                for (int j = 0; j < commentCount; j++) {
                    JSONObject comment = new JSONObject();
                    // 随机选择评论者
                    String commenter = WebViewConstants.USER_NAMES[(userIndex + j) % WebViewConstants.USER_NAMES.length];
                    // 随机选择评论内容
                    String commentContent = WebViewConstants.COMMENT_CONTENTS[j % WebViewConstants.COMMENT_CONTENTS.length];
                    
                    comment.put("username", commenter);
                    
                    // 20%的概率是回复评论
                    if (j > 0 && random.nextInt(5) == 0) {
                        String replyTo = WebViewConstants.USER_NAMES[(userIndex + j + 1) % WebViewConstants.USER_NAMES.length];
                        comment.put("replyTo", replyTo);
                    }
                    
                    comment.put("content", commentContent);
                    comments.put(comment);
                }
                
                // 随机选择点赞数量 (0-20人)
                int praiseCount = random.nextInt(21);  // 0-20
                JSONArray praises = new JSONArray();
                for (int j = 0; j < praiseCount; j++) {
                    String praiser = WebViewConstants.USER_NAMES[(userIndex + j) % WebViewConstants.USER_NAMES.length];
                    praises.put(praiser);
                }
                
                // 组装JSON对象
                item.put("type", "normal");
                item.put("id", i);
                item.put("username", username);
                item.put("avatar", avatar);
                item.put("content", content);
                item.put("time", publishTime);
                if (location != null) {
                    item.put("location", location);
                }
                if (images.length() > 0) {
                    item.put("images", images);
                }
                if (comments.length() > 0) {
                    item.put("comments", comments);
                }
                if (praises.length() > 0) {
                    item.put("praises", praises);
                }
                
                // 随机选择发布来源
                if (random.nextBoolean()) {  // 50%概率有来源
                    String source = WebViewConstants.SOURCES[random.nextInt(WebViewConstants.SOURCES.length)];
                    item.put("source", source);
                }
                
                friendCircleArray.put(item);
            }
            
            // 转换为JSON字符串
            JSONObject resultJson = new JSONObject();
            resultJson.put("data", friendCircleArray);
            return resultJson.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "生成JSON数据出错", e);
            return "{\"data\":[]}";
        }
    }

    /**
     * 获取更多朋友圈数据的JSON字符串
     * @param count 要获取的数据条数
     * @return JSON字符串
     */
    public String getMoreFriendCircleJsonData(int count) {
        Trace.beginSection("WebViewDataCenter_generateMoreJsonData");
        
        // 使用随机种子确保每次生成的数据不同但可控
        Random random = new Random(System.currentTimeMillis());
        
        JSONArray friendCircleArray = new JSONArray();
        
        try {
            // 生成朋友圈数据条目
            for (int i = 0; i < count; i++) {
                JSONObject item = new JSONObject();
                
                // 用户名和头像
                int userIndex = random.nextInt(WebViewConstants.USER_NAMES.length);
                String username = WebViewConstants.USER_NAMES[userIndex];
                int avatarIndex = (random.nextInt(11) + 1);
                String avatar = "avatar" + avatarIndex + ".jpg";
                
                // 时间和内容
                String publishTime = WebViewConstants.TIMES[random.nextInt(WebViewConstants.TIMES.length)];
                String content = WebViewConstants.CONTENTS[random.nextInt(WebViewConstants.CONTENTS.length)];
                
                // 随机添加图片 (0-5张)
                int imageCount = random.nextInt(6);
                JSONArray images = new JSONArray();
                for (int j = 0; j < imageCount; j++) {
                    int imageIndex = (random.nextInt(11) + 1);
                    String image = "local" + imageIndex + ".jpeg";
                    images.put(image);
                }
                
                // 随机添加点赞 (0-10人)
                int praiseCount = random.nextInt(11);
                JSONArray praises = new JSONArray();
                for (int j = 0; j < praiseCount; j++) {
                    String praiser = WebViewConstants.USER_NAMES[random.nextInt(WebViewConstants.USER_NAMES.length)];
                    praises.put(praiser);
                }
                
                // 随机添加评论 (0-5条)
                int commentCount = random.nextInt(6);
                JSONArray comments = new JSONArray();
                for (int j = 0; j < commentCount; j++) {
                    JSONObject comment = new JSONObject();
                    String commenter = WebViewConstants.USER_NAMES[random.nextInt(WebViewConstants.USER_NAMES.length)];
                    String commentContent = WebViewConstants.COMMENT_CONTENTS[random.nextInt(WebViewConstants.COMMENT_CONTENTS.length)];
                    
                    comment.put("username", commenter);
                    
                    // 20%的概率是回复评论
                    if (j > 0 && random.nextInt(5) == 0) {
                        String replyTo = WebViewConstants.USER_NAMES[random.nextInt(WebViewConstants.USER_NAMES.length)];
                        comment.put("replyTo", replyTo);
                    }
                    
                    comment.put("content", commentContent);
                    comments.put(comment);
                }
                
                // 组装JSON对象
                item.put("type", "normal");
                item.put("id", random.nextInt(10000) + 1000); // 随机ID，避免冲突
                item.put("username", username);
                item.put("avatar", avatar);
                item.put("content", content);
                item.put("time", publishTime);
                
                // 50%概率添加位置信息
                if (random.nextBoolean()) {
                    String location = WebViewConstants.LOCATIONS[random.nextInt(WebViewConstants.LOCATIONS.length)];
                    item.put("location", location);
                }
                
                // 添加图片、评论和点赞
                if (images.length() > 0) {
                    item.put("images", images);
                }
                if (comments.length() > 0) {
                    item.put("comments", comments);
                }
                if (praises.length() > 0) {
                    item.put("praises", praises);
                }
                
                // 随机添加来源 (30%概率)
                if (random.nextInt(10) < 3) {
                    String source = WebViewConstants.SOURCES[random.nextInt(WebViewConstants.SOURCES.length)];
                    item.put("source", source);
                }
                
                friendCircleArray.put(item);
            }
            
            // 转换为JSON字符串
            JSONObject resultJson = new JSONObject();
            resultJson.put("data", friendCircleArray);
            
            Trace.endSection();
            
            return resultJson.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "生成更多JSON数据出错", e);
            Trace.endSection();
            return "{\"data\":[]}";
        }
    }
} 