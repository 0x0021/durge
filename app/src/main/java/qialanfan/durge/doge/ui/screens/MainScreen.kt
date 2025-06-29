package qialanfan.durge.doge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import qialanfan.durge.doge.viewmodel.ConfigViewModel
import qialanfan.durge.doge.viewmodel.MainViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.math.sqrt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import qialanfan.durge.doge.ui.navigation.BottomNavItem
import qialanfan.durge.doge.ui.theme.Blue500

@Composable
fun MainScreen(
    mainViewModel: MainViewModel? = null,
    onRequestVpnPermission: (() -> Unit)? = null
) {
    val navController = rememberNavController()
    val configViewModel: ConfigViewModel = viewModel()
    
    // 从ViewModel获取代理运行状态，如果没有ViewModel则使用本地状态
    val isProxyRunning = if (mainViewModel != null) {
        mainViewModel.isProxyRunning.collectAsState().value
    } else {
        remember { mutableStateOf(false) }.value
    }
    
    // 监听VPN启动错误
    val vpnStartupError = mainViewModel?.vpnStartupError?.collectAsState()?.value
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = Color.Transparent, // 改为透明，让内容背景显示
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // 禁用默认的window insets
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black) // 保持黑色作为基础背景
                .padding(paddingValues)
        ) {
            NavigationGraph(
                navController = navController,
                isProxyRunning = isProxyRunning,
                mainViewModel = mainViewModel,
                onRequestVpnPermission = onRequestVpnPermission,
                configViewModel = configViewModel
            )
            
            // VPN启动错误弹窗
            vpnStartupError?.let { errorMessage ->
                VpnStartupErrorDialog(
                    errorMessage = errorMessage,
                    onDismiss = {
                        mainViewModel?.clearVpnStartupError()
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Start,
        BottomNavItem.Home,
        BottomNavItem.Strategy,
        BottomNavItem.Tools,
        BottomNavItem.More
    )
    
    NavigationBar(
        containerColor = Color.Black,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        items.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue500,
                    selectedTextColor = Blue500,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    isProxyRunning: Boolean,
    mainViewModel: MainViewModel?,
    onRequestVpnPermission: (() -> Unit)?,
    configViewModel: ConfigViewModel
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Start.route
    ) {
        composable(BottomNavItem.Start.route) {
            StartScreen(
                isProxyRunning = isProxyRunning,
                mainViewModel = mainViewModel,
                onRequestVpnPermission = onRequestVpnPermission,
                configViewModel = configViewModel
            )
        }
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                isProxyRunning = isProxyRunning,
                mainViewModel = mainViewModel,
                onRequestVpnPermission = onRequestVpnPermission,
                configViewModel = configViewModel
            )
        }
        composable(BottomNavItem.Strategy.route) {
            StrategyScreen()
        }
        composable(BottomNavItem.Tools.route) {
            ToolsScreen()
        }
        composable(BottomNavItem.More.route) {
            MoreScreen()
        }
    }
}

@Composable
fun StartScreen(
    isProxyRunning: Boolean,
    mainViewModel: MainViewModel?,
    onRequestVpnPermission: (() -> Unit)?,
    configViewModel: ConfigViewModel
) {
    val context = LocalContext.current
    
    // 直接在同一个页面内切换，不使用 AnimatedContent
    if (isProxyRunning) {
        // 代理运行中 - 显示渐变背景的运行状态页面
        RunningScreen(
            onStop = { 
                mainViewModel?.stopVpnService(context)
            },
            configViewModel = configViewModel,
            mainViewModel = mainViewModel
        )
    } else {
        // 代理未启动 - 显示启动页面
        StartupScreen(
            onStart = { 
                onRequestVpnPermission?.invoke()
            },
            configViewModel = configViewModel
        )
    }
}

@Composable
fun StartupScreen(
    onStart: () -> Unit,
    configViewModel: ConfigViewModel? = null
) {
    var buttonPressed by remember { mutableStateOf(false) }
    
    // 按钮颜色动画
    val buttonColor by animateFloatAsState(
        targetValue = if (buttonPressed) 1f else 0f,
        animationSpec = tween(300),
        label = "button_color"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars) // 添加状态栏padding，与RunningScreen一致
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // 状态文字 - 动态变化
            Text(
                text = if (buttonPressed) "启动中..." else "点击启动",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 占位空间，对应RunningScreen的时间显示
            Spacer(modifier = Modifier.height(50.dp)) // 对应时间文字的高度
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // 电源按钮 - 位置与RunningScreen完全一致
            FloatingActionButton(
                onClick = { 
                    buttonPressed = true
                    onStart() // 调用启动回调
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                containerColor = androidx.compose.ui.graphics.lerp(
                    Blue500, 
                    Color.White, 
                    buttonColor
                ),
                contentColor = androidx.compose.ui.graphics.lerp(
                    Color.White, 
                    Color.Black, 
                    buttonColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "启动",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 配置名称显示区域 - 放在底部
            configViewModel?.let { viewModel ->
                val activeConfig by viewModel.activeConfig.collectAsState()
                val configName = activeConfig?.name ?: "Default"
                Text(
                    text = configName,
                    color = Blue500,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp)) // 与RunningScreen底部间距一致
        }
    }
}

@Composable
fun RunningScreen(
    onStop: () -> Unit,
    configViewModel: ConfigViewModel? = null,
    mainViewModel: MainViewModel? = null
) {
    // 获取Clash统计数据
    val clashStats = mainViewModel?.clashStats?.collectAsState()?.value
    // 添加退出动画状态
    var isExiting by remember { mutableStateOf(false) }
    
    // 计时器状态 - 初始化时就计算真实的运行时间
    var elapsedSeconds by remember { 
        val startTimestamp = mainViewModel?.getVpnStartTimestamp() ?: 0L
        val initialSeconds = if (startTimestamp > 0L) {
            ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
        } else {
            0
        }
        mutableStateOf(initialSeconds)
    }
    
    // 按钮位置状态 - 用于精确定位雷达动画
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    
    val gradientColors = listOf(
        Color(0xFF40E0D0), // Turquoise
        Color(0xFF4169E1), // RoyalBlue  
        Color(0xFF9370DB)  // MediumPurple
    )
    
    val density = LocalDensity.current
    
    // 动画状态
    var cardsVisible by remember { mutableStateOf(false) }
    var contentAlpha by remember { mutableStateOf(0f) }
    var circleRadius by remember { mutableStateOf(0f) }
    var radarStarted by remember { mutableStateOf(false) }
    var radarVisible by remember { mutableStateOf(false) } // 控制雷达是否可见
    
    // 水波纹动画状态 - 简单的计数器驱动
    var animationTime by remember { mutableStateOf(0f) }
    
    // 动画效果
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else contentAlpha,
        animationSpec = tween(if (isExiting) 300 else 600), // 加快内容淡出速度
        label = "content_alpha"
    )
    
    val animatedRadius by animateFloatAsState(
        targetValue = if (isExiting) 0f else circleRadius,
        animationSpec = tween(if (isExiting) 800 else 1800, easing = FastOutSlowInEasing), // 加快圆形收缩速度
        label = "circle_radius"
    )
    
    // 水波纹动画效果 - 无限雷达扫描，始终运行但只在需要时绘制
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40000f, // 始终运行40秒循环
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animation_time"
    )
    
    // 雷达动画的真正连续时间计数器
    var radarTime by remember { mutableStateOf(0f) }
    var hasActiveRings by remember { mutableStateOf(false) }
    
    // 使用LaunchedEffect创建真正连续的时间计数器
    LaunchedEffect(radarStarted) {
        if (radarStarted) {
            val startTime = System.currentTimeMillis()
            var lastUpdateTime = 0L
            
            while (radarStarted) {
                val currentTime = System.currentTimeMillis()
                
                // 跳帧优化：如果帧率过高，跳过此次更新
                if (currentTime - lastUpdateTime < 32) { // 限制在30fps以下
                    kotlinx.coroutines.delay(5)
                    continue
                }
                
                radarTime = (currentTime - startTime).toFloat()
                lastUpdateTime = currentTime
                
                // 检查是否还有活跃的圆环
                val ringInterval = 5000f // 进一步增加间隔
                val ringLifetime = 10000f // 进一步减少生存时间
                var activeRingCount = 0
                
                for (i in 0 until 4) { // 进一步减少最大圆环数量
                    val ringStartTime = i * ringInterval
                    if (radarTime >= ringStartTime) {
                        val ringAge = radarTime - ringStartTime
                        if (ringAge <= ringLifetime) {
                            activeRingCount++
                        }
                    }
                }
                
                hasActiveRings = activeRingCount > 0
                
                // 如果没有活跃圆环且已经运行了足够长时间，停止动画以节约电量
                if (!hasActiveRings && radarTime > 30000f) { // 30秒后如果没有活跃圆环就停止
                    radarStarted = false
                    radarVisible = false
                    break
                }
                
                kotlinx.coroutines.delay(33) // 降低到30fps，减少渲染压力
            }
        }
    }
    
    // 启动时触发动画
    LaunchedEffect(Unit) {
        // 计算屏幕对角线长度，确保圆形能覆盖整个屏幕
        with(density) {
            val screenWidth = 400.dp.toPx() // 大概的屏幕宽度
            val screenHeight = 800.dp.toPx() // 大概的屏幕高度
            val maxRadius = sqrt(screenWidth * screenWidth + screenHeight * screenHeight)
            
            // 同时开始圆形展开和卡片滑入
            circleRadius = maxRadius
            cardsVisible = true
        }
        
        kotlinx.coroutines.delay(400) // 延迟更长时间，配合更慢的圆形展开
        contentAlpha = 1f // 内容淡入
        
        // 等待页面完全加载完成后启动雷达动画
        kotlinx.coroutines.delay(1000) // 等待所有转场动画完成
        radarStarted = true // 启动雷达动画计时器
        
        // 再稍微延迟一下让雷达可见，确保动画系统准备好
        kotlinx.coroutines.delay(100)
        radarVisible = true // 让雷达可见
    }
    
    // 计时器效果 - 基于VPN服务的真实启动时间
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // 每秒更新一次
            if (!isExiting) { // 只有在非退出状态时才计时
                // 获取VPN服务的启动时间戳
                val startTimestamp = mainViewModel?.getVpnStartTimestamp() ?: 0L
                if (startTimestamp > 0L) {
                    // 基于真实的VPN启动时间计算运行时长
                    val currentTime = System.currentTimeMillis()
                    elapsedSeconds = ((currentTime - startTimestamp) / 1000).toInt()
                } else {
                    // 如果无法获取启动时间，使用本地计时器（向下兼容）
                    elapsedSeconds++
                }
            }
        }
    }
    
    // 监听退出动画完成
    LaunchedEffect(isExiting) {
        if (isExiting) {
            kotlinx.coroutines.delay(800) // 等待退出动画完成，匹配更快的圆形收缩速度
            onStop() // 调用真正的停止回调
        }
    }
    

    
    // 使用两层Box结构：外层处理背景延伸，内层处理内容padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // 基础黑色背景
    ) {
        // 渐变背景层 - 延伸到整个屏幕包括状态栏
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                // 优化：减少不必要的重绘
            val center = Offset(size.width / 2f, size.height / 2f)
            
            if (animatedRadius > 0f) {
                // 绘制圆形渐变
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = gradientColors,
                        center = center,
                        radius = animatedRadius
                    ),
                    radius = animatedRadius,
                    center = center
                )
            }
            
            // 使用实际记录的按钮位置，如果还没有记录则使用默认位置
            val buttonCenter = if (buttonPosition != Offset.Zero) {
                buttonPosition
            } else {
                // 默认位置：屏幕中心偏上
                Offset(size.width / 2f, size.height * 0.45f)
            }
            
            // 绘制无限雷达扫描效果 - 只有在雷达可见且有活跃圆环且未退出时才绘制
            if (radarVisible && radarTime > 0f && hasActiveRings && !isExiting) {
                val maxRadius = kotlin.math.max(size.width, size.height) * 0.6f // 进一步减少扩散范围
                
                // 与LaunchedEffect保持一致的参数
                val ringInterval = 5000f // 5秒间隔，与LaunchedEffect一致
                val ringLifetime = 10000f // 10秒生命周期，与LaunchedEffect一致
                
                // 显示足够多的圆环，确保连续效果
                val maxRings = 4 // 与LaunchedEffect保持一致
                
                for (i in 0 until maxRings) {
                    val ringStartTime = i * ringInterval
                    
                    // 只有当雷达时间超过该圆环的启动时间时才显示
                    if (radarTime >= ringStartTime) {
                        val ringAge = radarTime - ringStartTime
                        
                        // 只显示生命周期内的圆环
                        if (ringAge <= ringLifetime) {
                            val progress = ringAge / ringLifetime // 0到1的进度
                            val buttonRadiusPx = 40.dp.toPx() // 按钮半径转换为像素
                            val radius = buttonRadiusPx + (maxRadius * progress) // 从按钮边缘扩散
                            
                            // 设置极低的透明度，进一步减少渲染负担
                            val alpha = 0.05f // 降低到5%透明度，几乎不可见
                    
                    if (alpha > 0.03f) { // 进一步降低最小透明度阈值
                        // 先绘制内侧阴影效果，紧贴主圆环
                        val shadowAlpha = alpha * 0.3f // 阴影更透明
                        val shadowStrokeWidth = 10.dp.toPx() // 阴影线宽，扩大范围
                        val shadowRadius = radius - shadowStrokeWidth / 2f // 阴影圆环紧贴主圆环内侧
                        if (shadowRadius > buttonRadiusPx) { // 确保阴影圆环不会小于按钮
                            drawCircle(
                                color = Color.White.copy(alpha = shadowAlpha),
                                radius = shadowRadius,
                                center = buttonCenter,
                                style = Stroke(width = shadowStrokeWidth) // 阴影更粗，扩大范围
                            )
                        }
                        
                        // 绘制主圆环，覆盖在阴影上方
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = radius,
                            center = buttonCenter,
                            style = Stroke(width = 2.dp.toPx()) // 主圆环
                        )
                    }
                }
                    }
                }
            }
        }) // Canvas的onDraw lambda结束
        
        // 内容层 - 有适当的状态栏padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars) // 为状态栏添加padding
                .padding(24.dp)
                .alpha(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // 状态文字
            Text(
                text = "点击停止",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 时间显示
            Text(
                text = formatTime(elapsedSeconds),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // 电源按钮
            FloatingActionButton(
                onClick = { 
                    // 启动退出动画
                    isExiting = true
                    radarStarted = false
                    radarVisible = false
                    cardsVisible = false
                },
                modifier = Modifier
                    .size(80.dp)
                    .onGloballyPositioned { coordinates ->
                        // 记录按钮的实际位置（中心点）
                        val position = coordinates.positionInRoot()
                        buttonPosition = Offset(
                            position.x + coordinates.size.width / 2f,
                            position.y + coordinates.size.height / 2f
                        )
                    },
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "停止",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 统计卡片 - 带动画，贴着底部tab栏顶部20px
            AnimatedVisibility(
                visible = cardsVisible && !isExiting,
                enter = slideInVertically(
                    animationSpec = tween(600),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(200), // 进一步加快卡片退出速度
                    targetOffsetY = { it }
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.BarChart,
                            iconColor = Color(0xFFFF6B6B),
                            title = "连接数",
                            value = "${clashStats?.requestCount ?: 0}"
                        )
                        
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconColor = Color(0xFF4ECDC4),
                            title = "总流量",
                            value = clashStats?.totalTraffic ?: "0 B"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ArrowDownward,
                            iconColor = Color(0xFF4FC3F7),
                            title = "下载",
                            value = clashStats?.downloadSpeed ?: "0 B/s"
                        )
                        
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ArrowUpward,
                            iconColor = Color(0xFFFFB74D),
                            title = "上传",
                            value = clashStats?.uploadSpeed ?: "0 B/s"
                        )
                    }
                }
            }
            
            // 配置名称显示区域 - 运行时隐藏，让界面更简洁
            /*
            configViewModel?.let { viewModel ->
                val activeConfig by viewModel.activeConfig.collectAsState()
                val configName = activeConfig?.name ?: "Default"
                Text(
                    text = configName,
                    color = Blue500,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            */
            
            Spacer(modifier = Modifier.height(4.dp)) // 非常接近底部tab栏
        }
    }
}

@Composable
fun HomeScreen(
    isProxyRunning: Boolean,
    mainViewModel: MainViewModel?,
    onRequestVpnPermission: (() -> Unit)?,
    configViewModel: ConfigViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        // 顶部区域
        TopSection(
            isProxyRunning = isProxyRunning,
            mainViewModel = mainViewModel,
            onRequestVpnPermission = onRequestVpnPermission,
            configViewModel = configViewModel
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 标签栏
        TabSection()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 功能卡片区域
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutboundModeCard()
            }
            item {
                ResourceCard()
            }
            item {
                ClashPanelCard()
            }
        }
    }
}

@Composable
fun AnimatedToggleButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = if (isRunning) Color.Red else Blue500,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "backgroundColor"
    )
    
    // 轻微的缩放动画（点击反馈）
    val scale by animateFloatAsState(
        targetValue = 1f, // 保持原始大小，不改变
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(40.dp) // 保持原来的高度
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // 保持原来的文字，只添加颜色过渡动画
        Text(
            text = if (isRunning) "停止" else "启动",
            color = Color.White,
            fontSize = 16.sp, // 保持原来的字体大小
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // 从16dp调整为12dp，让卡片更紧凑
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp) // 从20dp调整为16dp
                )
                Spacer(modifier = Modifier.width(6.dp)) // 从8dp调整为6dp
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp, // 从14sp调整为12sp
                    fontWeight = FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp)) // 从8dp调整为6dp
            
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp, // 从20sp调整为16sp
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StrategyScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "策略组",
            color = Color.White,
            fontSize = 24.sp
        )
    }
}

@Composable
fun ToolsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "工具",
            color = Color.White,
            fontSize = 24.sp
        )
    }
}

@Composable
fun MoreScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "更多",
            color = Color.White,
            fontSize = 24.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSection(
    isProxyRunning: Boolean,
    mainViewModel: MainViewModel?,
    onRequestVpnPermission: (() -> Unit)?,
    configViewModel: ConfigViewModel
) {
    val context = LocalContext.current
    var showConfigSheet by remember { mutableStateOf(false) }
    val activeConfig by configViewModel.activeConfig.collectAsState()
    val selectedConfig = activeConfig?.name ?: "Default"
    var tempSelectedConfig by remember { mutableStateOf(selectedConfig) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 配置选择器
        Row(
            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                    tempSelectedConfig = selectedConfig // 重置临时选择状态
                    showConfigSheet = true 
                }
        ) {
            // 信号图标
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "信号",
                tint = Color.Magenta,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedConfig,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "下拉",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // 启动/停止按钮 - 带动画效果
        AnimatedToggleButton(
            isRunning = isProxyRunning,
            onClick = { 
                if (isProxyRunning) {
                    // 停止VPN服务
                    mainViewModel?.stopVpnService(context)
                } else {
                    // 请求启动VPN服务（需要权限检查）
                    onRequestVpnPermission?.invoke()
                }
            }
        )
    }
    
    // 配置选择底部弹窗
    if (showConfigSheet) {
        ConfigSelectionBottomSheet(
            selectedConfig = tempSelectedConfig,
            onConfigSelected = { configName ->
                tempSelectedConfig = configName // 只更新临时选中状态，不关闭弹窗
            },
            onDismiss = { 
                // 在关闭弹窗时激活选中的配置
                if (tempSelectedConfig != selectedConfig) {
                    // 找到对应的配置ID并激活
                    val configs = configViewModel.configs.value
                    val selectedConfigEntity = configs.find { it.name == tempSelectedConfig }
                    selectedConfigEntity?.let {
                        configViewModel.activateConfig(it.id)
                    }
                }
                showConfigSheet = false 
            },
            configViewModel = configViewModel
        )
    }
}

@Composable
fun TabSection() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("通用", "网络", "日志")
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, tab ->
            Column(
                modifier = Modifier
                    .clickable { selectedTab = index }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = tab,
                    color = if (selectedTab == index) Blue500 else Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal
                )
                if (selectedTab == index) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(Blue500)
                    )
                }
            }
        }
    }
}

@Composable
fun OutboundModeCard() {
    var selectedMode by remember { mutableStateOf("规则模式") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4169E1), // RoyalBlue
                            Color(0xFF6495ED)  // CornflowerBlue
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = "出站模式",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "出站模式",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 模式选项
                val modes = listOf("直接连接", "全局代理", "规则模式")
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mode,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                
                if (selectedMode == "规则模式") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "使用规则系统决定如何处理请求",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { /* TODO: 代理服务器 */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "代理服务器",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        
                        Button(
                            onClick = { /* TODO: 代理规则 */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "代理规则",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50) // Green
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "资源",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "资源",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "管理 Clash 配置所需的规则集、代理组配置、GeoIP 数据库等核心资源文件",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TODO: 规则集 */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "规则集",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                
                Button(
                    onClick = { /* TODO: 资源管理 */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "资源管理",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ClashPanelCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF9800), // Orange
                            Color(0xFFFF5722)  // DeepOrange
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Clash 面板",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Clash 面板",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "通过 Web 界面远程管理和监控 Clash 代理服务，实时查看连接状态、流量统计和规则配置",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { /* TODO: 打开面板 */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "打开面板",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigSelectionBottomSheet(
    selectedConfig: String,
    onConfigSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    configViewModel: ConfigViewModel
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // 跳过半展开状态，直接全屏
    )
    
    // 全屏文本编辑器状态
    var showFullScreenEditor by remember { mutableStateOf(false) }
    var editorConfigName by remember { mutableStateOf("") }
    var editorConfigId by remember { mutableStateOf("") }
    var editorContent by remember { mutableStateOf("") }
    
    val isLoading by configViewModel.isLoading.collectAsState()
    
    // 配置列表弹窗 - 始终显示
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        windowInsets = WindowInsets(0) // 移除默认的window insets
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState) // 添加垂直滚动
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "配置列表",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "完成",
                        color = Blue500,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 配置列表
            ConfigSection(
                title = "配置",
                selectedConfig = selectedConfig,
                onConfigSelected = onConfigSelected,
                configViewModel = configViewModel
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 创建选项 - 移到编辑前面
            CreateSection(configViewModel = configViewModel)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 编辑选项 - 移到创建后面
            EditSection(
                configViewModel = configViewModel,
                onShowTextEditor = { configId, configName ->
                    configViewModel.readConfigContent(configId) { result ->
                        if (result.isSuccess) {
                            editorConfigId = configId
                            editorConfigName = configName
                            editorContent = result.getOrDefault("")
                            showFullScreenEditor = true
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // 文本编辑器弹窗 - 叠加在配置列表弹窗之上
    if (showFullScreenEditor) {
        TextEditorBottomSheet(
            configName = editorConfigName,
            initialContent = editorContent,
            onSave = { newContent ->
                configViewModel.saveConfigContent(editorConfigId, newContent) { result ->
                    if (result.isSuccess) {
                        showFullScreenEditor = false
                    }
                }
            },
            onDismiss = { showFullScreenEditor = false },
            isLoading = isLoading
        )
    }
}

@Composable
fun ConfigSection(
    title: String,
    selectedConfig: String,
    onConfigSelected: (String) -> Unit,
    configViewModel: ConfigViewModel
) {
    val configs by configViewModel.configs.collectAsState()
    val activeConfig by configViewModel.activeConfig.collectAsState()
    
    Column {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (configs.isEmpty()) {
            // 没有配置时显示提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C2C2E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "暂无配置",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "请导入或创建配置",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // 显示配置列表
            configs.forEachIndexed { index, config ->
                ConfigItem(
                    name = config.name,
                    subtitle = "更新于：${formatTimestamp(config.updatedAt)}",
                    isSelected = selectedConfig == config.name,
                    onClick = { 
                        onConfigSelected(config.name) // 只更新选中状态，不激活配置，不关闭弹窗
                    },
                    onDelete = { configViewModel.deleteConfig(config.id) },
                    canDelete = configs.size > 1 // 只有多于一个配置时才允许删除
                )
                
                if (index < configs.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorBottomSheet(
    configName: String,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    var content by remember { mutableStateOf(initialContent) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // 跳过半展开状态，直接全屏
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        windowInsets = WindowInsets(0) // 移除默认的window insets
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f) // 占用90%的高度
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "取消",
                        color = Blue500,
                        fontSize = 17.sp
                    )
                }
                
                Text(
                    text = configName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                
                TextButton(
                    onClick = { onSave(content) },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Blue500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "保存中...",
                                color = Blue500,
                                fontSize = 17.sp
                            )
                        }
                    } else {
                        Text(
                            text = "完成",
                            color = Blue500,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 代码编辑器
            CodeEditor(
                content = content,
                onContentChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}



@Composable
fun CodeEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    // 计算行数
    val lines = remember(content) {
        if (content.isEmpty()) listOf("") else content.split('\n')
    }
    val lineCount = lines.size
    val maxLineNumberWidth = remember(lineCount) {
        lineCount.toString().length.coerceAtLeast(2)
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117) // GitHub深色主题背景色
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 行号区域
            Column(
                modifier = Modifier
                    .background(Color(0xFF161B22)) // 行号区域背景
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .verticalScroll(scrollState)
            ) {
                repeat(lineCount) { index ->
                    Text(
                        text = (index + 1).toString().padStart(maxLineNumberWidth, ' '),
                        color = Color(0xFF7D8590), // 行号颜色
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier.height(18.dp)
                    )
                }
            }
            
            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF30363D))
            )
            
            // 代码编辑区域
            SelectionContainer {
                BasicTextField(
                    value = content,
                    onValueChange = onContentChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.Transparent, // 设置为透明，只显示语法高亮
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF79C0FF)), // 设置光标颜色为蓝色
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .verticalScroll(scrollState)
                        .horizontalScroll(horizontalScrollState),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (content.isEmpty()) {
                                Text(
                                    text = "# Clash配置文件\n# 在此输入YAML配置内容...",
                                    color = Color(0xFF7D8590),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            } else {
                                // 显示语法高亮的文本
                                Text(
                                    text = highlightYaml(content),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 18.sp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(0.dp)
                                )
                            }
                            // 透明的输入框，用于处理输入和光标
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RenameConfigDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    configViewModel: ConfigViewModel
) {
    var newName by remember { mutableStateOf(currentName) }
    val configs by configViewModel.configs.collectAsState()
    val isLoading by configViewModel.isLoading.collectAsState()
    val errorMessage by configViewModel.errorMessage.collectAsState()
    
    // 检查名称是否已存在
    val nameExists = configs.any { it.name == newName && it.name != currentName }
    val isNameValid = newName.isNotBlank() && !nameExists
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "重命名配置",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { 
                        Text(
                            "配置名称",
                            fontSize = 14.sp
                        ) 
                    },
                    singleLine = true,
                    isError = !isNameValid,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        disabledContainerColor = Color(0xFF2C2C2E),
                        errorContainerColor = Color(0xFF2C2C2E),
                        focusedLabelColor = Blue500,
                        unfocusedLabelColor = Color.Gray,
                        focusedIndicatorColor = Blue500,
                        unfocusedIndicatorColor = Color(0xFF48484A),
                        errorIndicatorColor = Color.Red,
                        errorLabelColor = Color.Red,
                        cursorColor = Blue500
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (nameExists) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "配置名称已存在",
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }
                }
                
                if (newName.isBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "配置名称不能为空",
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }
                }
                
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (isNameValid) {
                        onConfirm(newName)
                    }
                },
                enabled = isNameValid && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue500,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF48484A),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重命名中...")
                    }
                } else {
                    Text("确认")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Text("取消")
            }
        },
        containerColor = Color(0xFF1C1C1E),
        textContentColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 格式化时间戳为可读格式
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        else -> "${diff / (24 * 60 * 60 * 1000)}天前"
    }
}

@Composable
fun ConfigItem(
    name: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    canDelete: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Blue500,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // 更多操作按钮 - 只有在可以删除时才显示
                if (canDelete) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF2C2C2E))
                        ) {
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "删除配置", 
                                            color = Color.Red
                                        ) 
                                    },
                                    onClick = {
                                        onDelete()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSection(
    configViewModel: ConfigViewModel,
    onShowTextEditor: (String, String) -> Unit = { _, _ -> }
) {
    val activeConfig by configViewModel.activeConfig.collectAsState()
    
    // 重命名对话框状态
    var showRenameDialog by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "编辑",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ActionItem(
            icon = Icons.Default.Edit,
            iconColor = Color.Red,
            title = "重命名",
            onClick = { showRenameDialog = true }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ActionItem(
            icon = Icons.Default.Edit,
            iconColor = Blue500,
            title = "在文本模式中编辑",
            onClick = {
                activeConfig?.let { config ->
                    onShowTextEditor(config.id, config.name)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ActionItem(
            icon = Icons.Default.Link,
            iconColor = Color(0xFFFF9500),
            title = "外部资源",
            onClick = { /* TODO: 实现外部资源 */ }
        )
        
        // 重命名对话框
        if (showRenameDialog && activeConfig != null) {
            RenameConfigDialog(
                currentName = activeConfig!!.name,
                onConfirm = { newName ->
                    configViewModel.renameConfig(activeConfig!!.id, newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false },
                configViewModel = configViewModel
            )
        }
        

    }
}

@Composable
fun CreateSection(configViewModel: ConfigViewModel) {
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 导入配置文件，使用原始文件名
            configViewModel.importConfigFromLocal(it)
        }
    }
    
    // 观察状态
    val isLoading by configViewModel.isLoading.collectAsState()
    val errorMessage by configViewModel.errorMessage.collectAsState()
    
    // 显示错误消息
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: 显示Toast或Snackbar
            configViewModel.clearErrorMessage()
        }
    }
    
    Column {
        Text(
            text = "创建",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ActionItem(
            icon = Icons.Default.Folder,
            iconColor = Color(0xFF00BCD4),
            title = if (isLoading) "导入中..." else "从本地文件导入",
            onClick = {
                if (!isLoading) {
                    filePickerLauncher.launch("*/*") // 接受所有文件类型
                }
            },
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ActionItem(
            icon = Icons.Default.Download,
            iconColor = Color(0xFFFF9500),
            title = "下载配置",
            onClick = { /* TODO: 实现下载配置 */ }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ActionItem(
            icon = Icons.Default.ContentCopy,
            iconColor = Color.Red,
            title = if (isLoading) "创建副本中..." else "创建当前配置副本",
            onClick = {
                if (!isLoading) {
                    configViewModel.createConfigCopy()
                }
            },
            enabled = !isLoading
        )
    }
}



@Composable
fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) iconColor else iconColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 格式化时间显示，将秒数转换为 HH:MM:SS 格式
 */
private fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * YAML语法高亮
 */
@Composable
private fun highlightYaml(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split('\n')
        
        lines.forEachIndexed { lineIndex, line ->
            when {
                // 注释行 - 灰色
                line.trimStart().startsWith('#') -> {
                    withStyle(style = SpanStyle(color = Color(0xFF7D8590))) {
                        append(line)
                    }
                }
                // 键值对 - 键名高亮
                line.contains(':') && !line.trimStart().startsWith('-') -> {
                    val colonIndex = line.indexOf(':')
                    val beforeColon = line.substring(0, colonIndex)
                    val afterColon = line.substring(colonIndex)
                    
                    // 键名 - 蓝色
                    withStyle(style = SpanStyle(color = Color(0xFF79C0FF))) {
                        append(beforeColon)
                    }
                    // 冒号
                    withStyle(style = SpanStyle(color = Color(0xFFE6EDF3))) {
                        append(':')
                    }
                    
                    // 值部分
                    val value = afterColon.substring(1).trim()
                    if (afterColon.length > 1) {
                        append(" ")
                        when {
                            // 字符串值 - 绿色
                            value.startsWith('"') && value.endsWith('"') -> {
                                withStyle(style = SpanStyle(color = Color(0xFFA5D6FF))) {
                                    append(value)
                                }
                            }
                            value.startsWith('\'') && value.endsWith('\'') -> {
                                withStyle(style = SpanStyle(color = Color(0xFFA5D6FF))) {
                                    append(value)
                                }
                            }
                            // 数字 - 橙色
                            value.matches(Regex("^\\d+(\\.\\d+)?$")) -> {
                                withStyle(style = SpanStyle(color = Color(0xFFFF7B72))) {
                                    append(value)
                                }
                            }
                            // 布尔值 - 紫色
                            value.lowercase() in listOf("true", "false", "yes", "no", "on", "off") -> {
                                withStyle(style = SpanStyle(color = Color(0xFFD2A8FF))) {
                                    append(value)
                                }
                            }
                            // null值 - 红色
                            value.lowercase() in listOf("null", "~", "") -> {
                                withStyle(style = SpanStyle(color = Color(0xFFFF7B72))) {
                                    append(value)
                                }
                            }
                            // 普通值 - 默认颜色
                            else -> {
                                withStyle(style = SpanStyle(color = Color(0xFFE6EDF3))) {
                                    append(value)
                                }
                            }
                        }
                    }
                }
                // 列表项 - 破折号高亮
                line.trimStart().startsWith('-') -> {
                    val leadingSpaces = line.takeWhile { it == ' ' }
                    append(leadingSpaces)
                    
                    withStyle(style = SpanStyle(color = Color(0xFFFF7B72))) {
                        append("- ")
                    }
                    
                    val content = line.trimStart().substring(2)
                    withStyle(style = SpanStyle(color = Color(0xFFE6EDF3))) {
                        append(content)
                    }
                }
                // 普通文本
                else -> {
                    withStyle(style = SpanStyle(color = Color(0xFFE6EDF3))) {
                        append(line)
                    }
                }
            }
            
            // 添加换行符（除了最后一行）
            if (lineIndex < lines.size - 1) {
                append('\n')
            }
        }
    }
}

/**
 * VPN启动错误弹窗
 */
@Composable
fun VpnStartupErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "错误",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VPN启动失败",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "VPN启动过程中发生了错误，请截图以下信息并反馈给开发者：",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 错误信息展示区域
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2C2C2E)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            lineHeight = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "💡 提示：长按上方错误信息可以复制文本",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "知道了",
                    color = Blue500
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // 复制错误信息到剪贴板
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("VPN错误信息", errorMessage)
                    clipboard.setPrimaryClip(clip)
                    
                    // 显示toast提示
                    android.widget.Toast.makeText(context, "错误信息已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "复制",
                        color = Color.Gray
                    )
                }
            }
        }
    )
}