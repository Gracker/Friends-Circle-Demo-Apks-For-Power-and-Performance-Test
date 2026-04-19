package com.example.scrolling.common.model;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadType;
import com.example.scrolling.common.beans.FriendCircleBean;
import com.example.scrolling.common.beans.OtherInfoBean;
import com.example.scrolling.common.beans.UserBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared data factory for Moments-like timelines.
 *
 * This keeps content generation consistent across modules that only need
 * lightweight timeline data for rendering and scrolling tests.
 */
public final class MomentsDataFactory {

    private static final int DEFAULT_COUNT = 300;
    private static final int AVATAR_COUNT = 11;
    private static final int IMAGE_COUNT = 20;

    private static final String[] USER_NAMES = {
            "夏日柠檬茶☀", "Dream✨追梦人", "落尘_Anson", "江湖故人LIN", "无名花开open",
            "寄风Light", "Vancy薇", "浅末年华Sky", "Mr.Tang唐", "日落海岸line",
            "纸短情长EpLK", "Moon月影", "清欢Ambition", "Lemon_7up", "旧巷老猫",
            "北城以北ζ", "浮生半日CyJ", "孤城°旧梦", "Echo回声", "Coder_张江",
            "浅笑安然", "指尖的阳光", "青春不散场", "白鹿森林", "Alone独走",
            "夏末初秋", "倾城温柔", "星辰大海", "流年浅唱", "半夏微凉"
    };

    private static final String[] LOCATIONS = {
            "深圳-腾讯大厦", "北京-中关村", "上海-陆家嘴", "广州-珠江新城", "杭州-西湖区",
            "成都-天府广场", "武汉-光谷", "西安-大雁塔", "重庆-解放碑", "南京-新街口",
            "天津-小白楼", "长沙-橘子洲", "青岛-栈桥", "厦门-鼓浪屿", "苏州-金鸡湖"
    };

    private static final String[] TIMES = {
            "刚刚", "5分钟前", "10分钟前", "30分钟前", "1小时前",
            "2小时前", "昨天", "前天", "3天前", "一周前"
    };

    private static final String[] SOURCES = {
            "微信", "来自iPhone 16 Pro", "来自小米15", "来自HUAWEI Mate 70", "来自OPPO Find X8",
            "来自vivo X200", "来自Android", "来自平板", "来自Mac", "来自网页版"
    };

    private static final String[] CONTENTS = {
            "今天天气真好，阳光明媚，心情舒畅！#美好生活#",
            "刚刚看了一部超感人的电影，情节扣人心弦，强烈推荐！",
            "周末和朋友一起去爬山，感受大自然的美好。",
            "分享一道最近做得最成功的家常菜，家人都很喜欢。",
            "今天读完一本好书，很多观点都值得反复回味。",
            "终于完成这个阶段的工作目标，继续保持节奏。",
            "和朋友小聚，聊了很多有意思的话题，收获满满。",
            "最近开始规律运动，状态明显变好了。",
            "傍晚的晚霞太美了，随手拍了几张记录一下。",
            "给自己放个小假，喝杯咖啡，慢下来感受生活。"
    };

    private MomentsDataFactory() {
    }

    public static List<FriendCircleBean> create(@LoadType.Type int loadType) {
        return create(loadType, DEFAULT_COUNT);
    }

    public static List<FriendCircleBean> create(@LoadType.Type int loadType, int count) {
        int safeCount = Math.max(0, count);
        List<FriendCircleBean> result = new ArrayList<>(safeCount);

        int positionOffset = LoadConfig.getPositionOffset(loadType);
        long seedOffset = LoadConfig.getDataGenerationSeed(loadType) % USER_NAMES.length;

        for (int i = 0; i < safeCount; i++) {
            int index = i + positionOffset + (int) seedOffset;

            FriendCircleBean bean = new FriendCircleBean();
            bean.setUserBean(makeUser(index));
            bean.setContent(CONTENTS[index % CONTENTS.length]);
            bean.setOtherInfoBean(makeOtherInfo(index, i));
            bean.setImageUrls(makeImageUrls(index, i));
            result.add(bean);
        }

        return result;
    }

    private static UserBean makeUser(int index) {
        UserBean user = new UserBean();
        user.setUserId(String.valueOf(10000 + index));
        user.setUserName(USER_NAMES[index % USER_NAMES.length]);
        user.setUserAvatarUrl("avatar" + ((index % AVATAR_COUNT) + 1));
        return user;
    }

    private static OtherInfoBean makeOtherInfo(int index, int position) {
        OtherInfoBean info = new OtherInfoBean();
        info.setTime(TIMES[index % TIMES.length]);

        if (position % 4 == 0) {
            info.setSource(SOURCES[index % SOURCES.length]);
        }
        if (position % 3 == 0) {
            info.setLocation(LOCATIONS[index % LOCATIONS.length]);
        }
        return info;
    }

    private static List<String> makeImageUrls(int index, int position) {
        List<String> urls = new ArrayList<>();
        if (position % 2 == 0) {
            return urls;
        }

        int imageSize = Math.min(9, (index % 9) + 1);
        for (int i = 0; i < imageSize; i++) {
            urls.add("picture" + (((index + i) % IMAGE_COUNT) + 1));
        }
        return urls;
    }
}
