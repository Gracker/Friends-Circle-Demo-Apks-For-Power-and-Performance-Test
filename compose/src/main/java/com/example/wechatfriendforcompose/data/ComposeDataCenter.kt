package com.example.wechatfriendforcompose.data

import kotlin.random.Random

/**
 * Compose版数据中心，生成固定的测试数据
 */
object ComposeDataCenter {
    
    private const val FRIEND_CIRCLE_COUNT = 100
    
    private var cachedLightData: List<FriendCircleBean>? = null
    private var cachedMediumData: List<FriendCircleBean>? = null
    private var cachedHeavyData: List<FriendCircleBean>? = null
    
    /**
     * 清除缓存数据
     */
    fun clearCachedData() {
        cachedLightData = null
        cachedMediumData = null
        cachedHeavyData = null
    }
    
    /**
     * 获取朋友圈数据
     */
    fun getFriendCircleData(loadType: LoadType): List<FriendCircleBean> {
        return when (loadType.getBaseLoadType()) {
            BaseLoadType.MINIMAL, BaseLoadType.LIGHT -> {
                cachedLightData ?: generateData(BaseLoadType.LIGHT).also { cachedLightData = it }
            }
            BaseLoadType.MEDIUM -> {
                cachedMediumData ?: generateData(BaseLoadType.MEDIUM).also { cachedMediumData = it }
            }
            BaseLoadType.HEAVY -> {
                cachedHeavyData ?: generateData(BaseLoadType.HEAVY).also { cachedHeavyData = it }
            }
        }
    }
    
    /**
     * 生成朋友圈数据
     */
    private fun generateData(baseLoadType: BaseLoadType): List<FriendCircleBean> {
        val seed = when (baseLoadType) {
            BaseLoadType.MINIMAL, BaseLoadType.LIGHT -> 42L
            BaseLoadType.MEDIUM -> 142L
            BaseLoadType.HEAVY -> 242L
        }
        
        val random = Random(seed)
        val positionOffset = when (baseLoadType) {
            BaseLoadType.MINIMAL, BaseLoadType.LIGHT -> 0
            BaseLoadType.MEDIUM -> 100
            BaseLoadType.HEAVY -> 200
        }
        
        return (0 until FRIEND_CIRCLE_COUNT).map { i ->
            val user = UserBean(
                userId = (10000 + i).toString(),
                userName = ComposeConstants.USER_NAMES[i % ComposeConstants.USER_NAMES.size],
                avatarUrl = ComposeConstants.AVATAR_RES_NAMES[i % ComposeConstants.AVATAR_RES_NAMES.size]
            )
            
            val content = ComposeConstants.CONTENTS[i % ComposeConstants.CONTENTS.size]
            
            // 图片列表（奇数位置有图片）
            val images = if (i % 2 != 0) {
                val count = (i % 9) + 1
                (0 until count).map { j ->
                    "picture${((i + j) % 20) + 1}"
                }
            } else {
                emptyList()
            }
            
            // 生成点赞
            val praises = generatePraises(i + positionOffset, baseLoadType, random)
            
            // 生成评论
            val comments = generateComments(i + positionOffset, baseLoadType, random)
            
            // 其他信息
            val otherInfo = OtherInfoBean(
                time = ComposeConstants.TIMES[i % ComposeConstants.TIMES.size],
                source = if (i % 4 == 0) ComposeConstants.SOURCES[i % ComposeConstants.SOURCES.size] else null,
                location = if (i % 3 == 0) ComposeConstants.LOCATIONS[i % ComposeConstants.LOCATIONS.size] else null
            )
            
            FriendCircleBean(
                id = i,
                user = user,
                content = content,
                images = images,
                praises = praises,
                comments = comments,
                otherInfo = otherInfo
            )
        }
    }
    
    /**
     * 生成点赞数据
     */
    private fun generatePraises(position: Int, baseLoadType: BaseLoadType, random: Random): List<PraiseBean> {
        val count = when (baseLoadType) {
            BaseLoadType.MINIMAL, BaseLoadType.LIGHT -> position % 6
            BaseLoadType.MEDIUM -> position % 8 + 5
            BaseLoadType.HEAVY -> position % 11 + 10
        }
        
        return (0 until count).map { i ->
            val nameIndex = (position + i) % ComposeConstants.USER_NAMES.size
            PraiseBean(
                user = UserBean(
                    userId = (30000 + i + position * 100).toString(),
                    userName = ComposeConstants.USER_NAMES[nameIndex],
                    avatarUrl = ComposeConstants.AVATAR_RES_NAMES[i % ComposeConstants.AVATAR_RES_NAMES.size]
                )
            )
        }
    }
    
    /**
     * 生成评论数据
     */
    private fun generateComments(position: Int, baseLoadType: BaseLoadType, random: Random): List<CommentBean> {
        val count = when (baseLoadType) {
            BaseLoadType.MINIMAL, BaseLoadType.LIGHT -> position % 3
            BaseLoadType.MEDIUM -> position % 8 + 8
            BaseLoadType.HEAVY -> position % 15 + 15
        }
        
        val commentRandom = Random(position * 100L + baseLoadType.ordinal * 10)
        
        return (0 until count).map { i ->
            val childUser = UserBean(
                userId = (20000 + i + position * 100).toString(),
                userName = ComposeConstants.USER_NAMES[(position + i + 20) % ComposeConstants.USER_NAMES.size],
                avatarUrl = ComposeConstants.AVATAR_RES_NAMES[i % ComposeConstants.AVATAR_RES_NAMES.size]
            )
            
            val isReply = commentRandom.nextInt(10) < 3 && i > 0
            val parentUser = if (isReply) {
                val replyToIndex = commentRandom.nextInt(i)
                UserBean(
                    userId = (20000 + replyToIndex + position * 100).toString(),
                    userName = ComposeConstants.USER_NAMES[(position + replyToIndex + 20) % ComposeConstants.USER_NAMES.size],
                    avatarUrl = ComposeConstants.AVATAR_RES_NAMES[replyToIndex % ComposeConstants.AVATAR_RES_NAMES.size]
                )
            } else null
            
            CommentBean(
                childUser = childUser,
                parentUser = parentUser,
                content = ComposeConstants.COMMENT_CONTENTS[(position + i) % ComposeConstants.COMMENT_CONTENTS.size]
            )
        }
    }
}


