package qialanfan.durge.doge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import qialanfan.durge.doge.services.DurgeService
import qialanfan.durge.doge.ui.screens.MainScreen
import qialanfan.durge.doge.ui.theme.DurgeTheme
import qialanfan.durge.doge.utils.ConfigPathLogger
import qialanfan.durge.doge.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private var errorBroadcastReceiver: BroadcastReceiver? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ViewModel
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        
        // 检查初始VPN状态（处理UI进程重启）
        mainViewModel.checkInitialVpnStatus(this)
        
        // 设置VPN权限请求launcher
        setupVpnPermissionLauncher()
        
        // 注册VPN错误广播接收器
        setupErrorBroadcastReceiver()
        
        // 启用edge-to-edge显示，实现沉浸式状态栏
        enableEdgeToEdge()
        
        // 记录配置文件夹路径信息
        ConfigPathLogger.logConfigRootPath(this)
        
        setContent {
            DurgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        mainViewModel = mainViewModel,
                        onRequestVpnPermission = { requestVpnPermission() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 应用回到前台时检查VPN进程状态
        mainViewModel.onAppResumed(this)
    }
    
    private fun setupVpnPermissionLauncher() {
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // 权限获取成功，启动VPN服务
                mainViewModel.startVpnService(this)
            } else {
                // 权限被拒绝，重置状态
                mainViewModel.resetProxyState(this)
            }
        }
    }
    
    private fun setupErrorBroadcastReceiver() {
        errorBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DurgeService.ACTION_VPN_STARTUP_ERROR) {
                    val errorMessage = intent.getStringExtra(DurgeService.EXTRA_ERROR_MESSAGE)
                    if (errorMessage != null) {
                        // 直接通过ViewModel显示错误弹窗
                        mainViewModel.showVpnStartupError(errorMessage)
                        android.util.Log.d("MainActivity", "收到VPN启动错误广播: $errorMessage")
                    }
                }
            }
        }
        
        val filter = IntentFilter(DurgeService.ACTION_VPN_STARTUP_ERROR)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要明确指定接收器权限
            registerReceiver(errorBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(errorBroadcastReceiver, filter)
        }
        android.util.Log.d("MainActivity", "已注册VPN错误广播接收器")
    }
    
    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // 需要用户授权
            vpnPermissionLauncher.launch(intent)
        } else {
            // 已经有权限，直接启动服务
            mainViewModel.startVpnService(this)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        errorBroadcastReceiver?.let {
            try {
                unregisterReceiver(it)
                android.util.Log.d("MainActivity", "已注销VPN错误广播接收器")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "注销广播接收器失败", e)
            }
        }
    }
} 