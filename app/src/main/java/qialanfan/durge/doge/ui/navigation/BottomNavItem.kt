package qialanfan.durge.doge.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Start : BottomNavItem(
        route = "start",
        title = "开始",
        icon = Icons.Default.RadioButtonUnchecked
    )
    
    data object Home : BottomNavItem(
        route = "home",
        title = "首页",
        icon = Icons.Default.Home
    )
    
    data object Strategy : BottomNavItem(
        route = "strategy",
        title = "策略组",
        icon = Icons.Default.List
    )
    
    data object Tools : BottomNavItem(
        route = "tools",
        title = "工具",
        icon = Icons.Default.Settings
    )
    
    data object More : BottomNavItem(
        route = "more",
        title = "更多",
        icon = Icons.Default.MoreHoriz
    )
} 