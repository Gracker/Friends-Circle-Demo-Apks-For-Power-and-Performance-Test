package com.example.wechatfriendforwebview;

import android.os.Trace;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Random;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadType;

/**
 * WebView版朋友圈数据中心
 * 负责生成和管理朋友圈数据
 */
public class WebViewDataCenter {
    private static final String TAG = "WebViewDataCenter";
    private static final int MINIMAL_ITEM_COUNT = 10;
    private static final int LIGHT_ITEM_COUNT = 20;
    private static final int MEDIUM_ITEM_COUNT = 50;
    private static final int HEAVY_ITEM_COUNT = 100;

    private static final int MINIMAL_MAX_COMMENTS = 2;
    private static final int LIGHT_MAX_COMMENTS = 5;
    private static final int MEDIUM_MAX_COMMENTS = 15;
    private static final int HEAVY_MAX_COMMENTS = 30;

    private static final int MINIMAL_MAX_PRAISES = 5;
    private static final int LIGHT_MAX_PRAISES = 15;
    private static final int MEDIUM_MAX_PRAISES = 35;
    private static final int HEAVY_MAX_PRAISES = 60;
    private static final long MORE_DATA_BATCH_SEED_DELTA = 1009L;
    private static final long MORE_DATA_COUNT_SEED_DELTA = 31L;

    // 使用volatile保证多线程可见性，修复DCL问题
    private static volatile WebViewDataCenter instance;

    // 使用线程安全的ConcurrentHashMap替代HashMap
    private final Map<Integer, String> cachedJsonData = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger moreDataBatchCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    // 私有构造函数
    private WebViewDataCenter() {
        // 单例模式，不做初始化
    }

    // 获取单例实例（Double-Checked Locking with volatile）
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
        moreDataBatchCounter.set(0);
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
        int count = getItemCountByLoadType(loadType);

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
        final int maxComments = getMaxCommentsByLoadType(loadType);
        final int maxPraises = getMaxPraisesByLoadType(loadType);

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

                // 根据负载类型调整评论数量
                // 随机选择评论数量
                int commentCount = random.nextInt(maxComments);
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

                // 根据负载类型调整点赞数量
                // 随机选择点赞数量
                int praiseCount = random.nextInt(maxPraises);
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

        int batchIndex = moreDataBatchCounter.getAndIncrement();
        long seed = LoadConfig.DATA_GENERATION_SEED
                + (long) count * MORE_DATA_COUNT_SEED_DELTA
                + (long) batchIndex * MORE_DATA_BATCH_SEED_DELTA;
        Random random = new Random(seed);

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

    private int getItemCountByLoadType(int loadType) {
        switch (LoadType.getLoadLevel(loadType)) {
            case 0:
                return MINIMAL_ITEM_COUNT;
            case 1:
                return LIGHT_ITEM_COUNT;
            case 2:
                return MEDIUM_ITEM_COUNT;
            case 3:
                return HEAVY_ITEM_COUNT;
            default:
                return LIGHT_ITEM_COUNT;
        }
    }

    private int getMaxCommentsByLoadType(int loadType) {
        switch (LoadType.getLoadLevel(loadType)) {
            case 0:
                return MINIMAL_MAX_COMMENTS;
            case 1:
                return LIGHT_MAX_COMMENTS;
            case 2:
                return MEDIUM_MAX_COMMENTS;
            case 3:
                return HEAVY_MAX_COMMENTS;
            default:
                return LIGHT_MAX_COMMENTS;
        }
    }

    private int getMaxPraisesByLoadType(int loadType) {
        switch (LoadType.getLoadLevel(loadType)) {
            case 0:
                return MINIMAL_MAX_PRAISES;
            case 1:
                return LIGHT_MAX_PRAISES;
            case 2:
                return MEDIUM_MAX_PRAISES;
            case 3:
                return HEAVY_MAX_PRAISES;
            default:
                return LIGHT_MAX_PRAISES;
        }
    }
}
