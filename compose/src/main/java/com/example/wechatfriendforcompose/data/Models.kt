package com.example.wechatfriendforcompose.data

import androidx.compose.ui.text.AnnotatedString

/**
 * 朋友圈数据模型
 */
data class FriendCircleBean(
    val id: Int,
    val user: UserBean,
    val content: String,
    val images: List<String> = emptyList(),
    val comments: List<CommentBean> = emptyList(),
    val praises: List<PraiseBean> = emptyList(),
    val otherInfo: OtherInfoBean? = null,
    var praiseSpan: AnnotatedString? = null
)

/**
 * 用户数据模型
 */
data class UserBean(
    val userId: String,
    val userName: String,
    val avatarUrl: String
)

/**
 * 评论数据模型
 */
data class CommentBean(
    val childUser: UserBean,
    val parentUser: UserBean? = null,
    val content: String,
    var commentSpan: AnnotatedString? = null
)

/**
 * 点赞数据模型
 */
data class PraiseBean(
    val user: UserBean
)

/**
 * 其他信息（时间、来源、位置）
 */
data class OtherInfoBean(
    val time: String? = null,
    val source: String? = null,
    val location: String? = null
)


