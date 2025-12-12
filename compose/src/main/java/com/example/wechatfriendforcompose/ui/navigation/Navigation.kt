package com.example.wechatfriendforcompose.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.wechatfriendforcompose.ui.screens.FriendCircleScreen
import com.example.wechatfriendforcompose.ui.screens.MainScreen
import com.example.wechatfriendforcompose.data.LoadType

/**
 * 导航路由定义
 */
object Routes {
    const val MAIN = "main"
    const val FRIEND_CIRCLE = "friend_circle/{loadType}"

    fun friendCircle(loadType: LoadType) = "friend_circle/${loadType.name}"
}

/**
 * 应用导航
 */
@Composable
fun ComposeNavigation(
    navController: NavHostController,
    startDestination: String? = null
) {
    // 根据传入的activity_type确定起始目的地
    val actualStartDestination = when (startDestination) {
        "minimal" -> Routes.friendCircle(LoadType.MINIMAL)
        "light" -> Routes.friendCircle(LoadType.LIGHT)
        "medium" -> Routes.friendCircle(LoadType.MEDIUM)
        "heavy" -> Routes.friendCircle(LoadType.HEAVY)
        "light_between_frames" -> Routes.friendCircle(LoadType.LIGHT_BETWEEN_FRAMES)
        "medium_between_frames" -> Routes.friendCircle(LoadType.MEDIUM_BETWEEN_FRAMES)
        "heavy_between_frames" -> Routes.friendCircle(LoadType.HEAVY_BETWEEN_FRAMES)
        "light_mixed" -> Routes.friendCircle(LoadType.LIGHT_MIXED)
        "medium_mixed" -> Routes.friendCircle(LoadType.MEDIUM_MIXED)
        "heavy_mixed" -> Routes.friendCircle(LoadType.HEAVY_MIXED)
        else -> Routes.MAIN
    }

    NavHost(
        navController = navController,
        startDestination = actualStartDestination
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToFriendCircle = { loadType ->
                    navController.navigate(Routes.friendCircle(loadType))
                }
            )
        }

        composable(Routes.FRIEND_CIRCLE) { backStackEntry ->
            val loadTypeName = backStackEntry.arguments?.getString("loadType") ?: LoadType.LIGHT.name
            val loadType = LoadType.valueOf(loadTypeName)
            FriendCircleScreen(
                loadType = loadType,
                onBack = { navController.popBackStack() }
            )
        }
    }
}


