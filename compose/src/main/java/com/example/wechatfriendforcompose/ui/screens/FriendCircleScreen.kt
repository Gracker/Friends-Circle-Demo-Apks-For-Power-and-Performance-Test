package com.example.wechatfriendforcompose.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tracing.trace
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.wechatfriendforcompose.config.LoadConfig
import com.example.wechatfriendforcompose.data.*
import com.example.wechatfriendforcompose.ui.components.NineGridImages
import com.example.wechatfriendforcompose.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 朋友圈列表界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendCircleScreen(
    loadType: LoadType,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // 获取数据
    val friendCircleData = remember(loadType) {
        ComposeDataCenter.getFriendCircleData(loadType)
    }

    // 负载模拟器
    val loadSimulator = remember(loadType) {
        LoadSimulator(loadType)
    }

    // 监听滚动状态，触发负载模拟
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            loadSimulator.startContinuousLoad()
        } else {
            loadSimulator.stopContinuousLoad()
        }
    }

    // 启动持续负载（混合模式）
    DisposableEffect(loadType) {
        loadSimulator.startScheduledLoad()
        onDispose {
            loadSimulator.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item {
                FriendCircleHeader(
                    loadType = loadType,
                    onBack = onBack
                )
            }

            // 朋友圈列表
            items(
                items = friendCircleData,
                key = { it.id }
            ) { item ->
                // 每个item渲染时模拟负载
                loadSimulator.simulateItemLoad()

                FriendCircleItem(
                    item = item,
                    context = context
                )
            }
        }
    }
}

/**
 * 朋友圈Header
 */
@Composable
private fun FriendCircleHeader(
    loadType: LoadType,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(375.dp)
    ) {
        // 背景图
        val bgPainter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(getDrawableId(context, "main_bg"))
                .build()
        )
        Image(
            painter = bgPainter,
            contentDescription = "背景",
            modifier = Modifier
                .fillMaxWidth()
                .height(344.dp),
            contentScale = ContentScale.Crop
        )

        // 返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 36.dp, start = 16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = White
            )
        }

        // 底部白色区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(31.dp)
                .align(Alignment.BottomCenter)
                .background(White)
        )

        // 用户头像
        val avatarPainter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(getDrawableId(context, "main_avatar"))
                .build()
        )
        Image(
            painter = avatarPainter,
            contentDescription = "头像",
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = (-(-32)).dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 用户名（负载类型）
        Text(
            text = loadType.displayName,
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 96.dp, bottom = 40.dp)
        )
    }
}

/**
 * 朋友圈列表项
 */
@Composable
private fun FriendCircleItem(
    item: FriendCircleBean,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 头像
            val avatarPainter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(getDrawableId(context, item.user.avatarUrl))
                    .build()
            )
            Image(
                painter = avatarPainter,
                contentDescription = "头像",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 用户名
                Text(
                    text = item.user.userName,
                    color = LinkBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // 内容
                if (item.content.isNotEmpty()) {
                    Text(
                        text = item.content,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 6.dp),
                        lineHeight = 22.sp
                    )
                }

                // 图片
                if (item.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    NineGridImages(
                        images = item.images,
                        context = context
                    )
                }

                // 位置
                item.otherInfo?.location?.let { location ->
                    Text(
                        text = location,
                        color = LinkBlue,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // 时间和来源
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.otherInfo?.time?.let { time ->
                        Text(
                            text = time,
                            color = TextHint,
                            fontSize = 12.sp
                        )
                    }

                    item.otherInfo?.source?.let { source ->
                        Text(
                            text = source,
                            color = TextHint,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // 点赞和评论区域
                if (item.praises.isNotEmpty() || item.comments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundGray)
                            .padding(4.dp)
                    ) {
                        // 点赞
                        if (item.praises.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "点赞",
                                    tint = LinkBlue,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = buildAnnotatedString {
                                        item.praises.forEachIndexed { index, praise ->
                                            withStyle(SpanStyle(color = LinkBlue)) {
                                                append(praise.user.userName)
                                            }
                                            if (index < item.praises.size - 1) {
                                                append("，")
                                            }
                                        }
                                    },
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // 分隔线
                        if (item.praises.isNotEmpty() && item.comments.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 3.dp),
                                color = Divider,
                                thickness = 0.5.dp
                            )
                        }

                        // 评论
                        item.comments.forEach { comment ->
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = LinkBlue)) {
                                        append(comment.childUser.userName)
                                    }
                                    comment.parentUser?.let { parent ->
                                        append(" 回复 ")
                                        withStyle(SpanStyle(color = LinkBlue)) {
                                            append(parent.userName)
                                        }
                                    }
                                    append("：")
                                    append(comment.content)
                                },
                                fontSize = 14.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Divider,
            thickness = 0.5.dp
        )
    }
}

/**
 * 获取Drawable资源ID
 */
private fun getDrawableId(context: android.content.Context, name: String): Int {
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

/**
 * 负载模拟器
 */
private class LoadSimulator(private val loadType: LoadType) {
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(LoadConfig.COMPUTATION_SEED)
    private var isRunning = false

    // 绘制资源
    private val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val paint = Paint().apply { isAntiAlias = true }

    // 计算结果存储（防止编译器优化）
    @Volatile private var computationResult = 0.0

    /**
     * 模拟列表项渲染负载
     */
    fun simulateItemLoad() {
        trace("ComposeItemLoad") {
            val iterations = when (loadType) {
                LoadType.MINIMAL -> 0
                LoadType.LIGHT, LoadType.LIGHT_BETWEEN_FRAMES, LoadType.LIGHT_MIXED -> 5
                LoadType.MEDIUM, LoadType.MEDIUM_BETWEEN_FRAMES, LoadType.MEDIUM_MIXED -> 800
                LoadType.HEAVY, LoadType.HEAVY_BETWEEN_FRAMES, LoadType.HEAVY_MIXED -> 2000
            }

            performComputation(iterations)
        }
    }

    /**
     * 启动持续负载（滚动时）
     */
    fun startContinuousLoad() {
        if (loadType == LoadType.MINIMAL || loadType == LoadType.LIGHT) return

        isRunning = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return

                trace("ComposeContinuousLoad") {
                    val iterations = when (loadType) {
                        LoadType.MEDIUM, LoadType.MEDIUM_BETWEEN_FRAMES, LoadType.MEDIUM_MIXED -> 200
                        LoadType.HEAVY, LoadType.HEAVY_BETWEEN_FRAMES, LoadType.HEAVY_MIXED -> 500
                        else -> 0
                    }
                    performComputation(iterations)
                }

                handler.postDelayed(this, 8)
            }
        })
    }

    /**
     * 停止持续负载
     */
    fun stopContinuousLoad() {
        isRunning = false
    }

    /**
     * 启动定时负载（混合模式）
     */
    fun startScheduledLoad() {
        if (!loadType.name.contains("MIXED") && !loadType.name.contains("BETWEEN_FRAMES")) return

        isRunning = true
        scheduleNextTask()
    }

    private fun scheduleNextTask() {
        if (!isRunning) return

        val interval = LoadConfig.MIN_TASK_INTERVAL_MS + 
            random.nextLong(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS)

        handler.postDelayed({
            if (!isRunning) return@postDelayed

            trace("ComposeScheduledLoad") {
                val intensity = when {
                    loadType.name.contains("LIGHT") -> LoadConfig.LightMixedLoad.BETWEEN_FRAME_TASK_INTENSITY
                    loadType.name.contains("MEDIUM") -> LoadConfig.MediumMixedLoad.BETWEEN_FRAME_TASK_INTENSITY
                    loadType.name.contains("HEAVY") -> LoadConfig.HeavyMixedLoad.BETWEEN_FRAME_TASK_INTENSITY
                    else -> 100
                }
                performMixedComputation(intensity)
            }

            scheduleNextTask()
        }, interval)
    }

    /**
     * 执行计算负载
     */
    private fun performComputation(iterations: Int) {
        for (i in 0 until iterations) {
            val x = random.nextFloat() * 100
            val y = random.nextFloat() * 100

            paint.color = AndroidColor.argb(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
            )
            canvas.drawCircle(x, y, 10f, paint)

            if (loadType.name.contains("MEDIUM") || loadType.name.contains("HEAVY")) {
                val sinValue = sin(x.toDouble()) * cos(y.toDouble())
                computationResult += sinValue
            }
        }
    }

    /**
     * 执行混合负载计算
     */
    private fun performMixedComputation(intensity: Int) {
        // 数学计算
        var sum = 0.0
        for (i in 1..intensity) {
            val x = i * 0.1
            sum += sin(x) * cos(x) + sqrt(i.toDouble())
        }
        computationResult += sum

        // 图形绘制
        val graphicsCount = intensity / 3
        for (i in 0 until graphicsCount) {
            val x = random.nextFloat() * 200
            val y = random.nextFloat() * 200
            paint.color = AndroidColor.rgb(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
            )
            canvas.drawCircle(x, y, 5 + random.nextFloat() * 10, paint)
        }
    }

    /**
     * 停止所有负载
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}


