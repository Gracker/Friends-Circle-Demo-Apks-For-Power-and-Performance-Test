package com.example.wechatfriendforperformance;

/**
 * Constants for performance test module
 */
public class PerformanceConstants {

    public static final String EMOJI_REGEX = "\\[([\u4e00-\u9fa5\\w])+\\]|[\\ud83c\\udc00-\\ud83c\\udfff]|[\\ud83d\\udc00-\\ud83d\\udfff]|[\\u2600-\\u27ff]";
    public static final String URL_REGEX = "(((http|https)://)|((?<!((http|https)://))www\\.))" + ".*?" + "(?=(&nbsp;|[\\u4e00-\u9fa5]|\\s|　|<br />|$|[<>]))";
    public static final String TOPIC_REGEX = "#[\\p{Print}\\p{InCJKUnifiedIdeographs}&&[^#]]+#";
    public static final String AT_REGEX = "@[\u4e00-\u9fa5a-zA-Z0-9_-·\\.]+[\u200B]";
    public static final String SCHEME_URL = "com.kcrason.url//";
    public static final String SCHEME_EMOJI = "com.kcrason.emoji//";
    public static final String SCHEME_TOPIC = "com.kcrason.topic//";
    public static final String SCHEME_AT = "com.kcrason.at//";
    public static final String BLUE = "#ff0000";

    /**
     * Array of resource names for test images
     */
    // Local image resources used consistently, each friend circle item displays only one image
    public static final String[] IMAGE_RESOURCES = {
            "test_img_1", "test_img_2", "test_img_3", "test_img_4", "test_img_5",
            "test_img_6", "test_img_7", "test_img_8", "test_img_9", "test_img_10",
            "test_img_11", "test_img_12"
    };
    
    /**
     * Array of usernames for test data
     */
    // Fixed usernames, 100 trendy user names
    public static final String[] USER_NAMES = {
            "夏日柠檬茶☀", "Dream✨追梦人", "落尘_Anson", "江湖故人LIN", "无名花开open",
            "寄风Light", "Vancy薇", "浅末年华Sky", "Mr.Tang唐", "日落海岸line",
            "纸短情长EpLK", "Moon月影", "清欢Ambition", "Lemon_7up", "旧巷老猫",
            "北城以北ζ", "浮生半日CyJ", "孤城° 旧梦", "Echo回声", "Coder_张江",
            "浅笑❀安然", "指尖的阳光✿", "青春不散场☂", "白鹿°森林", "Alone独走",
            "夏末初秋°凉", "倾城▽温柔", "星辰⊙大海", "流年✎浅唱", "半夏□微凉",
            "倾城◇月光", "鹿角★情书", "风中↘奇缘", "半世★烟尘", "雨季¢花开",
            "青空↗夏日", "心中☀璀璨", "风落☠尘埃", "指尖☺年华", "南岸★绿茶",
            "尘埃里的花⊙", "梦的⊕尽头", "未来★可期", "倾城★温柔", "旧人⊿旧梦",
            "旧梦★如歌", "素手★琴弦", "ヤ烟雨江畔", "微ミ爱情", "陌路ミ花开",
            "陌上ヤ烟尘", "茶卡■盐湖", "半夏@微凉", "指尖$年华", "风中~奇缘",
            "南烟@南城", "星河★大海", "旧巷☀暖阳", "琴弦★断鸣", "半世★烟尘",
            "流年★如歌", "时光◢倒流", "陌路☞花开", "隔岸●烟火", "冷夜★长冰",
            "久别☺重逢", "墨城★烟雨", "深海★星辰", "酒清★余温", "入戏★太深",
            "落日☜余霞", "星辰★大海", "岁月☛静好", "花开□半夏", "旧事★如昨",
            "情感↘世界", "温情♡脉脉", "Bright夜光", "Thirst渴望", "Trace踪迹",
            "Tender温柔", "Vacant空虚", "Vague恍惚", "Vanish消失", "Vessel容器",
            "Wander流浪", "Whisper耳语", "Witness见证", "Worship崇拜", "Xanadu乐土",
            "Forever¥永恒", "Gardenia栀子", "Harmony和谐", "Illusion幻象", "Journey旅程",
            "Kindness善良", "Liberty自由", "Memories记忆", "Nebula星云", "Observe观察",
            "Paradise天堂", "Quietude寂静", "Rainbow彩虹", "Serenity宁静", "Twilight暮光"
    };
    
    /**
     * Array of locations for test data
     */
    // Fixed location information, 100 addresses
    public static final String[] LOCATIONS = {
            "深圳-腾讯大厦", "北京-中关村", "上海-陆家嘴", "广州-珠江新城", "杭州-西湖区",
            "成都-天府广场", "武汉-光谷", "西安-大雁塔", "重庆-解放碑", "南京-新街口",
            "天津-小白楼", "长沙-橘子洲", "沈阳-太原街", "青岛-栈桥", "济南-泉城广场",
            "郑州-二七广场", "苏州-金鸡湖", "厦门-鼓浪屿", "大连-星海广场", "福州-三坊七巷",
            "合肥-政务区", "昆明-滇池", "哈尔滨-中央大街", "无锡-鼋头渚", "宁波-天一广场",
            "南昌-滕王阁", "贵阳-甲秀楼", "南宁-青秀山", "兰州-黄河风情线", "惠州-惠州西湖",
            "珠海-情侣路", "佛山-祖庙", "中山-孙中山故居", "东莞-松山湖", "烟台-养马岛",
            "潍坊-风筝广场", "芜湖-鸠兹广场", "嘉兴-南湖", "绍兴-鲁迅故居", "台州-椒江",
            "温州-南塘老街", "泉州-开元寺", "漳州-九龙江", "莆田-湄洲岛", "三亚-亚龙湾",
            "海口-骑楼老街", "太原-汾河公园", "大同-云冈石窟", "石家庄-正定古城", "保定-直隶总督署",
            "秦皇岛-北戴河", "唐山-南湖公园", "廊坊-燕郊", "邯郸-丛台公园", "邢台-邢襄古城",
            "张家口-张库大道", "承德-避暑山庄", "沧州-铁狮子", "衡水-衡水湖", "长春-伪满皇宫",
            "吉林-松花江", "四平-四平战役纪念馆", "辽源-龙首山", "通化-鸭绿江", "白山-六道江",
            "松原-查干湖", "白城-洮儿河", "呼和浩特-昭君墓", "包头-九原区", "乌海-黄河", 
            "赤峰-红山", "通辽-科尔沁", "鄂尔多斯-响沙湾", "呼伦贝尔-莫日格勒河", "巴彦淖尔-乌梁素海",
            "乌兰察布-集宁", "兴安盟-阿尔山", "锡林郭勒-锡林浩特", "阿拉善-巴丹吉林沙漠", "银川-西夏陵",
            "石嘴山-星海湖", "吴忠-黄河", "固原-须弥山石窟", "中卫-沙坡头", "拉萨-布达拉宫",
            "日喀则-扎什伦布寺", "昌都-卡若区", "林芝-巴松措", "山南-雍布拉康", "那曲-色林错",
            "阿里-冈仁波齐", "西宁-东关清真大寺", "海东-互助土族故土园", "海北-青海湖", "黄南-隆务寺",
            "海南-贵德国家地质公园", "果洛-星空帐篷", "玉树-结古寺", "海西-茶卡盐湖", "兰州-黄河铁桥",
            "嘉峪关-嘉峪关城楼", "金昌-金川公园", "白银-白银区", "天水-麦积山", "武威-雷台"
    };
    
    /**
     * Array of post times for test data
     */
    // Fixed publishing times
    public static final String[] TIMES = {
            "刚刚", "5分钟前", "10分钟前", "30分钟前", "1小时前", 
            "2小时前", "昨天", "前天", "3天前", "一周前",
            "15分钟前", "45分钟前", "3小时前", "4小时前", "5小时前",
            "6小时前", "7小时前", "8小时前", "9小时前", "10小时前",
            "11小时前", "12小时前", "13小时前", "14小时前", "15小时前",
            "16小时前", "17小时前", "18小时前", "19小时前", "20小时前",
            "21小时前", "22小时前", "23小时前", "昨天上午", "昨天下午",
            "昨天晚上", "前天上午", "前天下午", "前天晚上", "大前天",
            "4天前", "5天前", "6天前", "一周零1天前", "一周零2天前", 
            "一周零3天前", "一周零4天前", "一周零5天前", "一周零6天前", "两周前",
            "两周零1天前", "两周零2天前", "两周零3天前", "两周零4天前", "两周零5天前",
            "两周零6天前", "三周前", "三周零1天前", "三周零2天前", "三周零3天前",
            "三周零4天前", "三周零5天前", "三周零6天前", "四周前", "一个月前",
            "一个月零1天前", "一个月零2天前", "一个月零3天前", "一个月零4天前", "一个月零5天前",
            "一个月零6天前", "一个月零1周前", "一个月零2周前", "一个月零3周前", "两个月前",
            "两个月零1天前", "两个月零2天前", "两个月零3天前", "两个月零4天前", "两个月零5天前",
            "两个月零6天前", "两个月零1周前", "两个月零2周前", "两个月零3周前", "三个月前",
            "三个月零1天前", "三个月零2天前", "三个月零3天前", "三个月零4天前", "三个月零5天前",
            "三个月零6天前", "三个月零1周前", "三个月零2周前", "三个月零3周前", "半年前"
    };
    
    /**
     * Array of sources for test data
     */
    // Fixed sources, 100 device sources
    public static final String[] SOURCES = {
            "微信", "来自iPhone 14 Pro", "来自小米13", "来自HUAWEI P60", "来自OPPO Find X6",
            "来自vivo X100", "来自Android", "朋友圈", "手机发布", "平板发布",
            "来自iPhone 14", "来自iPhone 14 Plus", "来自iPhone 14 Pro Max", "来自iPhone 13", "来自iPhone 13 mini",
            "来自iPhone 13 Pro", "来自iPhone 13 Pro Max", "来自iPhone SE", "来自iPhone 12", "来自iPhone 12 mini",
            "来自iPhone 12 Pro", "来自iPhone 12 Pro Max", "来自小米14", "来自小米14 Pro", "来自小米Ultra",
            "来自Redmi K70", "来自Redmi K70 Pro", "来自Redmi Note 13", "来自小米平板6", "来自小米平板6 Pro",
            "来自HUAWEI Mate 60", "来自HUAWEI Mate 60 Pro", "来自HUAWEI Mate X5", "来自HUAWEI P50", "来自HUAWEI P50 Pro",
            "来自HUAWEI nova 12", "来自HUAWEI MatePad Pro", "来自荣耀Magic6", "来自荣耀Magic6 Pro", "来自荣耀100",
            "来自荣耀100 Pro", "来自荣耀X50", "来自荣耀平板9", "来自OPPO Find X7", "来自OPPO Find X7 Ultra",
            "来自OPPO Reno11", "来自OPPO Reno11 Pro", "来自OPPO Pad 3", "来自vivo X100 Pro", "来自vivo X100 Ultra",
            "来自vivo S18", "来自vivo S18 Pro", "来自vivo Pad3", "来自一加12", "来自一加Ace3",
            "来自魅族21", "来自努比亚Z60 Ultra", "来自三星Galaxy S23", "来自三星Galaxy S23+", "来自三星Galaxy S23 Ultra",
            "来自三星Galaxy Z Fold5", "来自三星Galaxy Z Flip5", "来自三星Galaxy Tab S9", "来自三星Galaxy Tab S9+", "来自三星Galaxy Tab S9 Ultra",
            "来自Google Pixel 8", "来自Google Pixel 8 Pro", "来自Google Pixel 8a", "来自Google Pixel Fold", "来自Google Pixel Tablet",
            "来自Sony Xperia 1 V", "来自Sony Xperia 5 V", "来自Sony Xperia 10 V", "来自Motorola Edge 40", "来自Motorola Edge 40 Pro",
            "来自Motorola Razr 40", "来自Motorola Razr 40 Ultra", "来自ASUS ROG Phone 7", "来自ASUS ROG Phone 7 Ultimate", "来自ASUS Zenfone 10",
            "来自黑鲨5 Pro", "来自红魔8 Pro", "来自红魔8 Pro+", "来自iPad Pro", "来自iPad Air",
            "来自iPad mini", "来自iPad", "来自Galaxy Book4", "来自华为MateBook", "来自小米笔记本Pro",
            "来自联想拯救者", "来自Mac", "来自iPhone", "来自华为手机", "来自小米手机",
            "来自OPPO手机", "来自vivo手机", "来自Android手机", "来自iOS设备", "来自平板设备"
    };
    
    /**
     * Array of content for test data
     */
    // Fixed friend circle content
    public static final String[] CONTENTS = {
            "今天天气真好，阳光明媚，心情舒畅！#美好生活# @大海 @蓝天",
            "刚刚看了一部超感人的电影，情节扣人心弦，演员表演细腻，强烈推荐！",
            "周末和朋友一起去爬山，感受大自然的美好，呼吸新鲜空气，真是身心愉悦！#户外运动#",
            "分享一道我最拿手的菜：红烧排骨。秘诀是加入适量的冰糖和料酒，煮出来的排骨又香又软，家人都很喜欢！#美食分享#",
            "今天读完了村上春树的《挪威的森林》，文字优美，故事情节令人深思。最喜欢里面的一句话 #读书笔记#",
            "刚入手一台新相机，迫不及待地出门拍了几张照片，还在学习阶段，希望能拍出更好的作品！",
            "终于完成了这个月的工作目标，虽然过程很辛苦，但结果很满意。努力永远不会白费！#职场生活#",
            "今天是我们在一起的第三个年头，感谢一路上的陪伴与支持。爱情是彼此成长的过程，希望我们可以一直走下去。",
            "分享一个实用的生活小技巧：用白醋清洁水龙头，可以轻松去除水垢，让它焕然一新！#生活小窍门#",
            "今天去了一家新开的咖啡馆，环境优雅，咖啡香醇，店主还是个很有故事的人，聊了很多有趣的事情。推荐大家有空去尝试！#咖啡控#",
            "最近开始学习瑜伽，虽然一开始很不适应，但坚持了一个月后，明显感觉身体更灵活了，心情也更加平静。运动真的很重要！#健康生活#",
            "今天带孩子去动物园，看到他对各种动物充满好奇，眼睛里闪烁着兴奋的光芒，那一刻觉得所有的辛苦都值得了。#亲子时光#",
            "分享一本最近在读的书《原则》，作者是桥水基金创始人Ray Dalio，讲述了他的生活和工作原则，很有启发。#读书推荐#",
            "今天参加了一场公益活动，和志愿者们一起清理海滩垃圾，虽然很累，但看到干净的沙滩，心里特别满足。保护环境，从我做起！#环保行动#",
            "刚刚学会了一首新的吉他曲，虽然弹得不是很流畅，但很有成就感。学习一项新技能的过程总是充满挑战和乐趣。#音乐生活#",
            "今天去看了樱花，花海中漫步，仿佛置身于粉色的梦境中。生活中处处有美，关键是我们要善于发现。#春日赏花#",
            "今天尝试了一家朋友强烈推荐的火锅店，麻辣鲜香，食材新鲜，绝对是近期吃过最满意的一顿！#美食探店#",
            "最近开始学习投资理财，发现这是一个需要长期学习和实践的过程。与其盲目跟风，不如提升自己的财商。#财富管理#",
            "周末带父母去做了全身检查，健康真的是最重要的。希望他们永远健健康康，平平安安。#关爱父母#",
            "今天给自己放了一天假，睡到自然醒，看了一部电影，约朋友喝下午茶，感觉整个人都放松了。有时候，我们需要慢下来，善待自己。#生活方式#",
            "分享一个旅行小技巧：出门前做好详细的旅行计划，但也要给偶然的惊喜留出空间。最难忘的往往是计划之外的风景。#旅行笔记#",
            "今天参加了一场演讲比赛，虽然没有获奖，但克服了紧张情绪，完整地表达了自己的观点，对我来说已经是一种突破。#成长记录#",
            "最近迷上了拍摄延时摄影，记录城市的日出日落，光影变化，感受时间流动的魅力。#摄影日记#"
    };
    
    /**
     * Array of comments for test data
     */
    // Fixed comment content
    public static final String[] COMMENT_CONTENTS = {
            "赞同你的观点，说得太好了！",
            "照片拍得真不错，请问是用什么相机拍的？",
            "谢谢分享，受益匪浅！",
            "这家店我也去过，确实很赞！",
            "支持你，继续加油！",
            "哈哈，笑死我了，太有趣了！",
            "我也有同感，最近正在经历类似的事情。",
            "这个地方看起来很美，有机会一定要去看看。",
            "文笔真好，读起来很舒服。",
            "请问这个活动具体在哪里举行？我也想参加。",
            "学习了，这个方法我会试试看。",
            "照片里的你看起来气色很好！",
            "我也想学这个技能，有什么入门建议吗？",
            "完全同意你的看法，这确实是一个值得思考的问题。",
            "这个食谱看起来很美味，周末我也要尝试做一下。",
            "这本书我也在读，确实很有启发性。",
            "你的进步很明显，继续保持！",
            "这个地方我去过，确实如你所说的那样美。",
            "看了你的分享，我也有了新的想法，谢谢！",
            "你的经历很鼓舞人心，感谢分享！",
            "我也经历过类似的事情，确实如你所说。",
            "请问这个产品在哪里可以买到？",
            "看到你的分享很开心，希望你一切都好！",
            "这个观点很独特，让我重新思考了这个问题。",
            "太有才了，佩服佩服！",
            "我也是这个活动的参与者，很高兴在这里看到你的分享！",
            "照片拍得很美，构图很巧妙。",
            "这个菜看起来很美味，回头我也试试。",
            "你的文字总是那么温暖，读起来很舒服。",
            "分享得很详细，对我帮助很大，谢谢！",
            "看到你的进步很开心，继续加油！",
            "这个地方我一直想去，看了你的照片更加期待了。",
            "完全赞同你的观点，说到我心坎里了。",
            "谢谢推荐，我会去试试看的。",
            "你的经历给了我很多启发，感谢分享！",
            "这个方法真的有效吗？我有点疑惑。",
            "照片里的风景太美了，仿佛置身其中。",
            "我也喜欢这首歌，旋律很美。",
            "看了你的文字，仿佛看到了自己的影子。",
            "这个活动很有意义，希望能有更多人参与。",
            "你的坚持是最棒的，为你点赞！",
            "这道菜我也会做，但没你做得好看。",
            "你的分享总是那么实用，感谢！",
            "这个地方我去过，但没发现你说的那个景点，下次一定要找找。",
            "看到你的分享，我也想出去走走了。",
            "你的文字很有画面感，读起来很享受。",
            "请问这个活动还会再举办吗？",
            "照片拍得很专业，请问你是专业摄影师吗？",
            "我也尝试过这个方法，确实很有效。",
            "看到你的进步很开心，继续加油！",
            "这个观点很新颖，让我学到了新东西。",
            "我也想参加这样的活动，有什么渠道可以了解吗？",
            "你的经历很励志，给了我很多勇气。",
            "这个地方看起来很适合周末放松，谢谢推荐！",
            "你的文笔真好，把简单的事情描述得很生动。",
            "我也遇到过类似的情况，你的解决方法很值得借鉴。",
            "照片里的表情很自然，拍得真好！",
            "这个观点我以前没想过，确实很有道理。",
            "我也是这个领域的爱好者，很高兴看到你的分享！",
            "看了你的文字，仿佛看到了自己年轻时的影子。",
            "请问这个技能需要什么基础？我也想学习。",
            "你的坚持很令人佩服，相信会有好结果的！",
            "这个地方的风景如此美丽，一定要亲眼去看看。",
            "你的分享很真实，没有过度美化，很欣赏这点。",
            "我也喜欢这样的生活方式，简单而充实。",
            "看到你的变化很惊喜，继续加油！",
            "这个方法我试过，确实很有效，推荐给大家。",
            "照片里的笑容很灿烂，看得出你很开心。",
            "你的文字总是能触动我的心弦，谢谢分享。",
            "这个活动很有意义，希望能持续下去。",
            "我也经历过这样的事情，感同身受。",
            "这个地方我去过，但没你拍得这么美。",
            "看了你的分享，我也想尝试一下这个爱好。"
    };
    
    /**
     * Friend circle view type constants
     */
    // Friend circle type constants
    public class FriendCircleType {
        public static final int FRIEND_CIRCLE_TYPE_NORMAL = 0;
        public static final int FRIEND_CIRCLE_TYPE_TEXT_ONLY = 1;
        public static final int FRIEND_CIRCLE_TYPE_SHARE = 2;
    }
    
    /**
     * Comment type constants
     */
    // Comment type constants
    public static final int COMMENT_TYPE_SINGLE = 0;
    public static final int COMMENT_TYPE_REPLY = 1;
    
    /**
     * Translation status constants
     */
    // Translation status constants
    public class TranslationState {
        public static final int TRANSLATION_STATE_HIDE = 0;
        public static final int TRANSLATION_STATE_SHOW = 1;
        public static final int TRANSLATION_STATE_LOADING = 2;
    }

    public final static class CommentType {
        //单一评论
        public final static int COMMENT_TYPE_SINGLE = 0;
        //回复评论
        public final static int COMMENT_TYPE_REPLY = 1;
    }
} 