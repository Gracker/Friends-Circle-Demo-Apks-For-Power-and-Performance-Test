package com.example.wechatfriendforwebviewsurface;

import android.os.Trace;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.example.loadconfig.LoadConfig;

/**
 * GeckoView版朋友圈数据中心
 * 负责生成和管理朋友圈数据
 */
public class GeckoViewDataCenter {
    private static final String TAG = "GeckoViewDataCenter";

    private static GeckoViewDataCenter instance;

    // 缓存的JSON数据
    private Map<Integer, String> cachedJsonData = new HashMap<>();

    // 私有构造函数
    private GeckoViewDataCenter() {
        // 单例模式，不做初始化
    }

    // 获取单例实例
    public static GeckoViewDataCenter getInstance() {
        if (instance == null) {
            synchronized (GeckoViewDataCenter.class) {
                if (instance == null) {
                    instance = new GeckoViewDataCenter();
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

        Trace.beginSection("GeckoViewDataCenter_generateJsonData");

        // 根据负载类型确定生成的朋友圈数量
        int count;
        switch (loadType) {
            case com.example.loadconfig.LoadType.LIGHT:
                count = 20;  // 轻负载，20条数据
                break;
            case com.example.loadconfig.LoadType.MEDIUM:
                count = 50;  // 中负载，50条数据
                break;
            case com.example.loadconfig.LoadType.HEAVY:
                count = 100; // 高负载，100条数据
                break;
            default:
                count = 30;
                break;
        }

        String jsonData = generateFriendCircleJsonData(count, loadType);

        // 缓存生成的数据
        cachedJsonData.put(loadType, jsonData);

        Trace.endSection();

        return jsonData;
    }

    /**
     * 生成特定数量的朋友圈数据JSON
     * @param count 数据条数
     * @param loadType 负载类型
     * @return JSON字符串
     */
    private String generateFriendCircleJsonData(int count, int loadType) {
        JSONArray friendCircleArray = new JSONArray();
        Random random = new Random(LoadConfig.DATA_GENERATION_SEED); // 使用统一配置的种子值，确保每次生成的数据顺序一致

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
                int userIndex = i % GeckoViewConstants.USER_NAMES.length;
                String username = GeckoViewConstants.USER_NAMES[userIndex];

                // 头像使用本地资源，确保1-11之间循环
                int avatarIndex = (i % 11) + 1;
                String avatar = "avatar" + avatarIndex + ".jpg";

                // 随机选择发布时间
                String publishTime = GeckoViewConstants.TIMES[random.nextInt(GeckoViewConstants.TIMES.length)];

                // 随机选择位置
                String location = null;
                if (random.nextBoolean()) {  // 50%概率有位置信息
                    location = GeckoViewConstants.LOCATIONS[random.nextInt(GeckoViewConstants.LOCATIONS.length)];
                }

                // 随机选择内容
                String content = GeckoViewConstants.CONTENTS[i % GeckoViewConstants.CONTENTS.length];

                // 固定图片数量和图片序号
                int imageCount = (i % 10); // 0-9张图片
                JSONArray images = new JSONArray();
                for (int j = 0; j < imageCount; j++) {
                    // 确保图片序号在1-11之间
                    int imageIndex = (j % 11) + 1;
                    String image = "local" + imageIndex + ".jpeg";
                    images.put(image);
                }

                // 根据负载类型调整评论数量
                int maxComments;
                switch (loadType) {
                    case com.example.loadconfig.LoadType.LIGHT:
                        maxComments = 5;   // 轻负载: 0-4条评论
                        break;
                    case com.example.loadconfig.LoadType.MEDIUM:
                        maxComments = 15;  // 中负载: 0-14条评论
                        break;
                    case com.example.loadconfig.LoadType.HEAVY:
                        maxComments = 11;  // 高负载: 0-10条评论
                        break;
                    default:
                        maxComments = 11;
                        break;
                }

                // 随机选择评论数量
                int commentCount = random.nextInt(maxComments);
                JSONArray comments = new JSONArray();
                for (int j = 0; j < commentCount; j++) {
                    JSONObject comment = new JSONObject();
                    // 随机选择评论者
                    String commenter = GeckoViewConstants.USER_NAMES[(userIndex + j) % GeckoViewConstants.USER_NAMES.length];
                    // 随机选择评论内容
                    String commentContent = GeckoViewConstants.COMMENT_CONTENTS[j % GeckoViewConstants.COMMENT_CONTENTS.length];

                    comment.put("username", commenter);

                    // 20%的概率是回复评论
                    if (j > 0 && random.nextInt(5) == 0) {
                        String replyTo = GeckoViewConstants.USER_NAMES[(userIndex + j + 1) % GeckoViewConstants.USER_NAMES.length];
                        comment.put("replyTo", replyTo);
                    }

                    comment.put("content", commentContent);
                    comments.put(comment);
                }

                // 根据负载类型调整点赞数量
                int maxPraises;
                switch (loadType) {
                    case com.example.loadconfig.LoadType.LIGHT:
                        maxPraises = 15;   // 轻负载: 0-14个点赞
                        break;
                    case com.example.loadconfig.LoadType.MEDIUM:
                        maxPraises = 35;   // 中负载: 0-34个点赞
                        break;
                    case com.example.loadconfig.LoadType.HEAVY:
                        maxPraises = 21;   // 高负载: 0-20个点赞
                        break;
                    default:
                        maxPraises = 21;
                        break;
                }

                // 随机选择点赞数量
                int praiseCount = random.nextInt(maxPraises);
                JSONArray praises = new JSONArray();
                for (int j = 0; j < praiseCount; j++) {
                    String praiser = GeckoViewConstants.USER_NAMES[(userIndex + j) % GeckoViewConstants.USER_NAMES.length];
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
                    String source = GeckoViewConstants.SOURCES[random.nextInt(GeckoViewConstants.SOURCES.length)];
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
        Trace.beginSection("GeckoViewDataCenter_generateMoreJsonData");

        // 使用随机种子确保每次生成的数据不同但可控
        Random random = new Random(System.currentTimeMillis());

        JSONArray friendCircleArray = new JSONArray();

        try {
            // 生成朋友圈数据条目
            for (int i = 0; i < count; i++) {
                JSONObject item = new JSONObject();

                // 用户名和头像
                int userIndex = random.nextInt(GeckoViewConstants.USER_NAMES.length);
                String username = GeckoViewConstants.USER_NAMES[userIndex];
                int avatarIndex = (random.nextInt(11) + 1);
                String avatar = "avatar" + avatarIndex + ".jpg";

                // 时间和内容
                String publishTime = GeckoViewConstants.TIMES[random.nextInt(GeckoViewConstants.TIMES.length)];
                String content = GeckoViewConstants.CONTENTS[random.nextInt(GeckoViewConstants.CONTENTS.length)];

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
                    String praiser = GeckoViewConstants.USER_NAMES[random.nextInt(GeckoViewConstants.USER_NAMES.length)];
                    praises.put(praiser);
                }

                // 随机添加评论 (0-5条)
                int commentCount = random.nextInt(6);
                JSONArray comments = new JSONArray();
                for (int j = 0; j < commentCount; j++) {
                    JSONObject comment = new JSONObject();
                    String commenter = GeckoViewConstants.USER_NAMES[random.nextInt(GeckoViewConstants.USER_NAMES.length)];
                    String commentContent = GeckoViewConstants.COMMENT_CONTENTS[random.nextInt(GeckoViewConstants.COMMENT_CONTENTS.length)];

                    comment.put("username", commenter);

                    // 20%的概率是回复评论
                    if (j > 0 && random.nextInt(5) == 0) {
                        String replyTo = GeckoViewConstants.USER_NAMES[random.nextInt(GeckoViewConstants.USER_NAMES.length)];
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
                    String location = GeckoViewConstants.LOCATIONS[random.nextInt(GeckoViewConstants.LOCATIONS.length)];
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
                    String source = GeckoViewConstants.SOURCES[random.nextInt(GeckoViewConstants.SOURCES.length)];
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

