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
            "Digital Knight", "Pixel Wanderer", "Code Voyager", "Tech Explorer", "Cyber Nomad",
            "Data Dreamer", "Binary Traveler", "Algorithm Artisan", "Network Navigator", "Quantum Quester",
            "Software Sage", "Cloud Crusader", "Virtual Venturer", "Logic Luminary", "System Seeker",
            "Protocol Pioneer", "Interface Innovator", "Command Conqueror", "Stack Surveyor", "Memory Master",
            "Syntax Savant", "Kernel Keeper", "Browser Bard", "Hardware Hero", "Function Finder",
            "Bug Bouncer", "Cache Custodian", "Render Ranger", "Process Pathfinder", "Upload Ultron",
            "Download Defender", "Thread Theorist", "Bandwidth Bard", "Firewall Fighter", "Router Ranger",
            "Cursor Champion", "Domain Dynamo", "Encryption Expert", "Graphics Guardian", "Mobile Maestro",
            "Tablet Tactician", "Laptop Legend", "Keyboard Knight", "Mouse Maverick", "Screen Sorcerer",
            "Wearable Warrior", "App Architect", "Plugin Pioneer", "Database Detective", "Script Sleuth"
    };
    
    /**
     * Array of locations for test data
     */
    // Fixed location information, 100 addresses
    public static final String[] LOCATIONS = {
            "Empire State Building, New York", "Eiffel Tower, Paris", "Marina Bay Sands, Singapore",
            "Sydney Opera House, Australia", "Taj Mahal, India", "Burj Khalifa, Dubai",
            "Big Ben, London", "Colosseum, Rome", "Red Square, Moscow", "The Great Wall, China",
            "Machu Picchu, Peru", "Christ the Redeemer, Rio de Janeiro", "Sagrada Familia, Barcelona",
            "Angkor Wat, Cambodia", "Hagia Sophia, Istanbul", "Brandenburg Gate, Berlin",
            "Golden Gate Bridge, San Francisco", "Victoria Harbour, Hong Kong", "Grand Palace, Bangkok",
            "Forbidden City, Beijing", "Petra, Jordan", "Neuschwanstein Castle, Germany",
            "Hollywood Sign, Los Angeles", "Shibuya Crossing, Tokyo", "CN Tower, Toronto",
            "Statue of Liberty, New York", "Park Güell, Barcelona", "Louvre Museum, Paris",
            "Santorini, Greece", "Great Pyramid of Giza, Egypt"
    };
    
    /**
     * Array of post times for test data
     */
    // Fixed publishing times
    public static final String[] TIMES = {
            "Just now", "5 minutes ago", "15 minutes ago", "Half an hour ago", "1 hour ago",
            "2 hours ago", "This morning", "Last night", "Yesterday", "2 days ago",
            "This week", "Last week", "Last month", "Two months ago", "Half a year ago",
            "Last year", "2 years ago", "A long time ago"
    };
    
    /**
     * Array of sources for test data
     */
    // Fixed sources, 100 device sources
    public static final String[] SOURCES = {
            "iPhone 14 Pro Max", "Samsung Galaxy S22 Ultra", "Google Pixel 7 Pro",
            "Huawei P50 Pro", "Xiaomi 12 Pro", "OnePlus 10 Pro", "OPPO Find X5 Pro",
            "vivo X80 Pro", "Honor Magic4 Pro", "realme GT 2 Pro", "Nothing Phone (1)",
            "Motorola Edge 30 Pro", "Sony Xperia 1 IV", "ZTE Axon 40 Ultra",
            "ASUS ROG Phone 6 Pro", "Lenovo Legion Y90", "Nubia Red Magic 7 Pro",
            "BlackShark 5 Pro", "Apple iPad Pro", "Samsung Galaxy Tab S8 Ultra",
            "Microsoft Surface Pro 8", "HUAWEI MatePad Pro", "Xiaomi Pad 5 Pro",
            "iQOO 9 Pro", "Redmi K50 Pro", "Poco F4 GT"
    };
    
    /**
     * Array of content for test data
     */
    // Fixed friend circle content
    public static final String[] CONTENTS = {
            "Just visited a fantastic tech exhibition! The future is now.",
            "Working on an exciting new project. Can't wait to share the results!",
            "Beautiful sunset view from my office window today.",
            "Met some amazing developers at the conference. So inspired!",
            "Just released a new app update with awesome features. Check it out!",
            "Celebrating 5 years at my company today. Time flies!",
            "Enjoying a peaceful morning coffee while coding.",
            "Visited the new tech hub downtown. Impressive facilities!",
            "Weekend hike with colleagues. Team building at its best!",
            "Learning a new programming language. Challenging but fun!",
            "Just solved that bug that's been bothering me for days. What a relief!",
            "Attending an AI workshop this weekend. Anyone else going?",
            "My workspace setup is finally complete. Productivity mode on!",
            "Excited about the new tech gadgets announced today!",
            "Taking a well-deserved break after completing the project on time.",
            "Just received an award for our innovative mobile app!",
            "Brainstorming session with the team. Great ideas flowing!",
            "Working remotely from a beautiful beach today. Best of both worlds!",
            "Nostalgic about my first coding project 10 years ago.",
            "Just upgraded my development environment. Everything runs so smoothly now!"
    };
    
    /**
     * Array of comments for test data
     */
    // Fixed comment content
    public static final String[] COMMENT_CONTENTS = {
            "This is amazing! Keep up the good work!",
            "Congratulations on your achievement!",
            "I've been following your progress. Very impressive!",
            "Can you share more details about this?",
            "This reminds me of a similar project I worked on.",
            "Looking forward to seeing the final results!",
            "Would love to collaborate with you on something similar.",
            "Thanks for sharing this inspiration!",
            "The design looks very sleek and modern.",
            "How long did it take you to complete this?",
            "This is exactly what I've been looking for!",
            "You've come a long way since your first project.",
            "Is this available for public use yet?",
            "Your attention to detail is outstanding.",
            "Have you considered adding this feature?",
            "This will definitely disrupt the market!",
            "I'm showing this to my team for inspiration.",
            "The user interface is very intuitive.",
            "What tools did you use to create this?",
            "This solves a real problem. Great job!",
            "I've been waiting for something like this!",
            "Your work ethic is truly admirable.",
            "This could be a game-changer in the industry.",
            "The performance improvements are significant.",
            "You've really mastered this technology!",
            "Can't wait to try it out myself!",
            "This is setting a new standard.",
            "You make it look so easy!",
            "Very creative approach to solving this problem.",
            "I'm impressed by the efficiency of your solution."
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
} 