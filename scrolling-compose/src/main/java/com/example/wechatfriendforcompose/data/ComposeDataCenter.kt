package com.example.wechatfriendforcompose.data

import kotlin.random.Random

/**
 * Compose版数据中心，生成固定的测试数据
 */
object ComposeDataCenter {

    private const val MINIMAL_FRIEND_CIRCLE_COUNT = 28
    private const val LIGHT_FRIEND_CIRCLE_COUNT = 72
    private const val MEDIUM_FRIEND_CIRCLE_COUNT = 110
    private const val HEAVY_FRIEND_CIRCLE_COUNT = 150

    private const val MINIMAL_DATA_SEED = 21L
    private const val LIGHT_DATA_SEED = 42L
    private const val MEDIUM_DATA_SEED = 142L
    private const val HEAVY_DATA_SEED = 242L

    private var cachedMinimalData: List<FriendCircleBean>? = null
    private var cachedLightData: List<FriendCircleBean>? = null
    private var cachedMediumData: List<FriendCircleBean>? = null
    private var cachedHeavyData: List<FriendCircleBean>? = null

    /**
     * 清除缓存数据
     */
    fun clearCachedData() {
        cachedMinimalData = null
        cachedLightData = null
        cachedMediumData = null
        cachedHeavyData = null
    }

    /**
     * 获取朋友圈数据
     */
    fun getFriendCircleData(loadType: LoadType): List<FriendCircleBean> {
        return when (loadType.getBaseLoadType()) {
            BaseLoadType.MINIMAL -> {
                cachedMinimalData ?: generateData(BaseLoadType.MINIMAL).also { cachedMinimalData = it }
            }
            BaseLoadType.LIGHT -> {
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
            BaseLoadType.MINIMAL -> MINIMAL_DATA_SEED
            BaseLoadType.LIGHT -> LIGHT_DATA_SEED
            BaseLoadType.MEDIUM -> MEDIUM_DATA_SEED
            BaseLoadType.HEAVY -> HEAVY_DATA_SEED
        }

        val positionOffset = when (baseLoadType) {
            BaseLoadType.MINIMAL -> 0
            BaseLoadType.LIGHT -> 50
            BaseLoadType.MEDIUM -> 120
            BaseLoadType.HEAVY -> 240
        }

        val totalCount = when (baseLoadType) {
            BaseLoadType.MINIMAL -> MINIMAL_FRIEND_CIRCLE_COUNT
            BaseLoadType.LIGHT -> LIGHT_FRIEND_CIRCLE_COUNT
            BaseLoadType.MEDIUM -> MEDIUM_FRIEND_CIRCLE_COUNT
            BaseLoadType.HEAVY -> HEAVY_FRIEND_CIRCLE_COUNT
        }
        val seedOffset = (seed % 97).toInt()

        return (0 until totalCount).map { i ->
            val user = UserBean(
                userId = (10000 + i).toString(),
                userName = ComposeConstants.USER_NAMES[(i + seedOffset) % ComposeConstants.USER_NAMES.size],
                avatarUrl = ComposeConstants.AVATAR_RES_NAMES[(i + seedOffset) % ComposeConstants.AVATAR_RES_NAMES.size]
            )

            val content = ComposeConstants.CONTENTS[(i + seedOffset) % ComposeConstants.CONTENTS.size]

            // 图片列表（按负载级别分层）
            val images = when (baseLoadType) {
                BaseLoadType.MINIMAL -> {
                    if (i % 7 == 0) {
                        listOf("picture${(i % 20) + 1}")
                    } else {
                        emptyList()
                    }
                }
                BaseLoadType.LIGHT -> {
                    if (i % 2 != 0) {
                        val count = (i % 6) + 1
                        (0 until count).map { j ->
                            "picture${((i + j) % 20) + 1}"
                        }
                    } else {
                        emptyList()
                    }
                }
                BaseLoadType.MEDIUM -> {
                    if (i % 3 != 0) {
                        val count = (i % 9) + 1
                        (0 until count).map { j ->
                            "picture${((i + j) % 20) + 1}"
                        }
                    } else {
                        emptyList()
                    }
                }
                BaseLoadType.HEAVY -> {
                    val count = (i % 9) + 1
                    (0 until count).map { j ->
                        "picture${((i + j) % 20) + 1}"
                    }
                }
            }

            // 生成点赞
            val praises = generatePraises(i + positionOffset, baseLoadType)

            // 生成评论
            val comments = generateComments(i + positionOffset, baseLoadType)

            // 其他信息
            val otherInfo = OtherInfoBean(
                time = ComposeConstants.TIMES[(i + seedOffset) % ComposeConstants.TIMES.size],
                source = if (i % 4 == 0) ComposeConstants.SOURCES[(i + seedOffset) % ComposeConstants.SOURCES.size] else null,
                location = if (i % 3 == 0) ComposeConstants.LOCATIONS[(i + seedOffset) % ComposeConstants.LOCATIONS.size] else null
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
    private fun generatePraises(position: Int, baseLoadType: BaseLoadType): List<PraiseBean> {
        val count = when (baseLoadType) {
            BaseLoadType.MINIMAL -> if (position % 8 == 0) 1 else 0
            BaseLoadType.LIGHT -> position % 6
            BaseLoadType.MEDIUM -> position % 10 + 6
            BaseLoadType.HEAVY -> position % 14 + 12
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
    private fun generateComments(position: Int, baseLoadType: BaseLoadType): List<CommentBean> {
        val count = when (baseLoadType) {
            BaseLoadType.MINIMAL -> position % 2
            BaseLoadType.LIGHT -> position % 4
            BaseLoadType.MEDIUM -> position % 10 + 10
            BaseLoadType.HEAVY -> position % 18 + 18
        }

        val commentRandom = Random(position * 100L + baseLoadType.ordinal * 10L)
        val replyThreshold = when (baseLoadType) {
            BaseLoadType.MINIMAL -> 1
            BaseLoadType.LIGHT -> 2
            BaseLoadType.MEDIUM -> 4
            BaseLoadType.HEAVY -> 5
        }

        return (0 until count).map { i ->
            val childUser = UserBean(
                userId = (20000 + i + position * 100).toString(),
                userName = ComposeConstants.USER_NAMES[(position + i + 20) % ComposeConstants.USER_NAMES.size],
                avatarUrl = ComposeConstants.AVATAR_RES_NAMES[i % ComposeConstants.AVATAR_RES_NAMES.size]
            )

            val isReply = commentRandom.nextInt(10) < replyThreshold && i > 0
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
