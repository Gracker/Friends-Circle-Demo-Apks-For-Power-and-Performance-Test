package com.example.wechatfriendforcompose.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wechatfriendforcompose.data.LoadType
import com.example.wechatfriendforcompose.ui.theme.*
import com.example.wechatfriendforcompose.R

/**
 * 主界面 - 选择负载类型
 */
@Composable
fun MainScreen(
    onNavigateToFriendCircle: (LoadType) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppInfoCard()
        Spacer(modifier = Modifier.height(24.dp))

        // 负载选择卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 帧内负载
                Text(
                    text = "帧内负载 (Frame Load)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LoadButton(
                    text = "最轻负载 (Minimal)",
                    color = Color(0xFF4CAF50),
                    onClick = { onNavigateToFriendCircle(LoadType.MINIMAL) }
                )

                LoadButton(
                    text = "轻负载 (帧内)",
                    color = PrimaryBlue,
                    onClick = { onNavigateToFriendCircle(LoadType.LIGHT) }
                )

                LoadButton(
                    text = "中负载 (帧内)",
                    color = AccentTeal,
                    onClick = { onNavigateToFriendCircle(LoadType.MEDIUM) }
                )

                LoadButton(
                    text = "高负载 (帧内)",
                    color = DeepPurple,
                    onClick = { onNavigateToFriendCircle(LoadType.HEAVY) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Divider
                )

                // 帧间负载
                Text(
                    text = "帧间负载 (Between Frame Load)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LoadButton(
                    text = "轻负载 (帧间)",
                    color = LightBlue,
                    onClick = { onNavigateToFriendCircle(LoadType.LIGHT_BETWEEN_FRAMES) }
                )

                LoadButton(
                    text = "中负载 (帧间)",
                    color = LightCyan,
                    onClick = { onNavigateToFriendCircle(LoadType.MEDIUM_BETWEEN_FRAMES) }
                )

                LoadButton(
                    text = "高负载 (帧间)",
                    color = Indigo,
                    onClick = { onNavigateToFriendCircle(LoadType.HEAVY_BETWEEN_FRAMES) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Divider
                )

                // 混合负载
                Text(
                    text = "混合负载 (帧内 + 帧间)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LoadButton(
                    text = "轻负载 (混合)",
                    color = Orange,
                    onClick = { onNavigateToFriendCircle(LoadType.LIGHT_MIXED) }
                )

                LoadButton(
                    text = "中负载 (混合)",
                    color = DeepOrange,
                    onClick = { onNavigateToFriendCircle(LoadType.MEDIUM_MIXED) }
                )

                LoadButton(
                    text = "高负载 (混合)",
                    color = LightRed,
                    onClick = { onNavigateToFriendCircle(LoadType.HEAVY_MIXED) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "应用介绍",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "本应用使用Jetpack Compose开发，模拟微信朋友圈的滑动场景，用于测试不同计算负载下的滑动性能表现。\n\n" +
                           "✨ Compose特点：\n" +
                           "• 声明式UI框架\n" +
                           "• Kotlin开发语言\n" +
                           "• 现代化的UI工具包\n\n" +
                           "通过对比不同负载模式，可以分析Compose与传统View的性能差异。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        // 版权信息
        Text(
            text = "© 2025 Compose朋友圈性能测试应用",
            fontSize = 12.sp,
            color = TextHint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun AppInfoCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(id = R.string.app_feature),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = context.packageName,
                fontSize = 12.sp,
                color = TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(bottom = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = White
        )
    }
}


