package com.example.wechatfriendforcompose.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 微信风格颜色
val WeChatGreen = Color(0xFF07C160)
val WeChatGreenDark = Color(0xFF059B4B)
val BackgroundGray = Color(0xFFF2F2F2)
val TextPrimary = Color(0xFF333333)
val TextSecondary = Color(0xFF666666)
val TextHint = Color(0xFF999999)
val LinkBlue = Color(0xFF576B95)
val Divider = Color(0xFFDCDCDC)
val White = Color(0xFFFFFFFF)

// 负载类型颜色
val PrimaryBlue = Color(0xFF2196F3)
val AccentTeal = Color(0xFF009688)
val DeepPurple = Color(0xFF673AB7)
val LightBlue = Color(0xFF4FC3F7)
val LightCyan = Color(0xFF4DD0E1)
val Indigo = Color(0xFF7986CB)
val Orange = Color(0xFFFF7043)
val DeepOrange = Color(0xFFF4511E)
val LightRed = Color(0xFFE57373)

private val LightColorScheme = lightColorScheme(
    primary = WeChatGreen,
    onPrimary = White,
    primaryContainer = WeChatGreenDark,
    secondary = LinkBlue,
    onSecondary = White,
    background = BackgroundGray,
    surface = White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = WeChatGreen,
    onPrimary = White,
    primaryContainer = WeChatGreenDark,
    secondary = LinkBlue,
    onSecondary = White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = White,
    onSurface = White,
)

@Composable
fun ComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}


