package qialanfan.durge.doge.viewmodel

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.IBinder
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import qialanfan.durge.doge.services.DurgeService
import qialanfan.durge.doge.services.ClashApiService
import qialanfan.durge.doge.data.ClashStats
import kotlinx.coroutines.flow.catch

class MainViewModel : ViewModel() {
    
    // VPN 服务连接状态
    private val _isVpnConnected = MutableStateFlow(false)
    val isVpnConnected: StateFlow<Boolean> = _isVpnConnected.asStateFlow()
    
    // 代理状态
    private val _isProxyActive = MutableStateFlow(false)
    val isProxyActive: StateFlow<Boolean> = _isProxyActive.asStateFlow()
    
    // 代理运行状态（兼容旧代码）
    val isProxyRunning: StateFlow<Boolean> = _isProxyActive.asStateFlow()
    
    // VPN 启动时间戳
    private var vpnStartTimestamp: Long = 0L
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // VPN启动详细错误（用于弹窗显示）
    private val _vpnStartupError = MutableStateFlow<String?>(null)
    val vpnStartupError: StateFlow<String?> = _vpnStartupError.asStateFlow()
    
    // Clash统计数据
    private val _clashStats = MutableStateFlow(ClashStats())
    val clashStats: StateFlow<ClashStats> = _clashStats.asStateFlow()
    
    // 服务连接实例
    private var durgeService: DurgeService? = null
    private var isServiceBound = false
    private val clashApiService = ClashApiService.getInstance()
    

    
    // 移除init中的检查，改为在有Context时主动调用
    
    // 服务连接回调
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // DurgeService 没有 LocalBinder，使用静态方法获取实例
            durgeService = DurgeService.getCurrentInstance()
            isServiceBound = true
            updateVpnStatus()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            durgeService = null
            isServiceBound = false
            _isVpnConnected.value = false
            _isProxyActive.value = false
        }
    }
    
    /**
     * 启动 VPN 服务
     */
    fun startVpnService(context: Context) {
        viewModelScope.launch {
            try {
                val intent = Intent(context, DurgeService::class.java)
                context.startService(intent)
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                
                _isVpnConnected.value = true
                _isProxyActive.value = true
                vpnStartTimestamp = System.currentTimeMillis()
                
                // 保存启动时间到持久化存储
                saveVpnStartTime(context, vpnStartTimestamp)
                
                // 开始收集Clash统计数据
                startClashStatsCollection()
                
                _errorMessage.value = null
                
            } catch (e: Exception) {
                _errorMessage.value = "启动 VPN 服务失败: ${e.message}"
                resetProxyState(context)
            }
        }
    }
    

    

    
    /**
     * 停止 VPN 服务
     */
    fun stopVpnService(context: Context) {
        // 杀死独立的VPN服务进程 ":durgeservice"
        killDurgeServiceProcess(context)
        
        // 立即重置状态
        resetProxyState(context)
        _errorMessage.value = null
        
        // 停止收集Clash统计数据
        stopClashStatsCollection()
        
        // 清除VPN启动错误
        clearVpnStartupError()
    }
    
    /**
     * 杀死独立的VPN服务进程
     */
    private fun killDurgeServiceProcess(context: Context) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = activityManager.runningAppProcesses
            
            val packageName = context.packageName
            val targetProcessName = "$packageName:durgeservice"
            
            runningApps?.forEach { processInfo ->
                if (processInfo.processName == targetProcessName) {
                    // 找到了独立的VPN服务进程，直接杀死
                    Process.killProcess(processInfo.pid)
                    return
                }
            }
            
            // 如果没找到进程，使用备用方案
            val intent = Intent(context, DurgeService::class.java)
            context.stopService(intent)
            
        } catch (e: Exception) {
            // 出错时使用备用方案
            val intent = Intent(context, DurgeService::class.java)
            context.stopService(intent)
        }
    }
    
    /**
     * 切换代理状态
     */
    fun toggleProxy(context: Context) {
        if (_isProxyActive.value) {
            stopVpnService(context)
        } else {
            // 这里应该请求 VPN 权限，通过 Activity 的回调处理
            // startVpnService 会在权限获取后被调用
        }
    }
    
    /**
     * 重置代理状态
     */
    fun resetProxyState(context: Context? = null) {
        _isVpnConnected.value = false
        _isProxyActive.value = false
        vpnStartTimestamp = 0L
        
        // 清除持久化存储的启动时间
        context?.let { clearVpnStartTime(it) }
    }
    
    /**
     * 应用恢复前台时检查状态
     */
    fun onAppResumed(context: Context) {
        // 应用恢复前台时强制更新VPN状态，直接检查进程
        viewModelScope.launch {
            val isVpnRunning = isVpnProcessRunning(context)
            _isVpnConnected.value = isVpnRunning
            _isProxyActive.value = isVpnRunning
            
            // 如果VPN正在运行但内存中时间戳为0，从持久化存储恢复
            if (isVpnRunning && vpnStartTimestamp == 0L) {
                val savedTimestamp = loadVpnStartTime(context)
                vpnStartTimestamp = if (savedTimestamp > 0) {
                    savedTimestamp
                } else {
                    // 如果没有保存的时间戳，使用当前时间并保存
                    val currentTime = System.currentTimeMillis()
                    saveVpnStartTime(context, currentTime)
                    currentTime
                }
            }
        }
    }
    
    /**
     * 更新 VPN 状态
     */
    private fun updateVpnStatus() {
        viewModelScope.launch {
            try {
                // 简单直接：只判断VPN进程是否存在
                val isActive = DurgeService.isRunning
                _isVpnConnected.value = isActive
                _isProxyActive.value = isActive
                
                // 如果VPN正在运行但时间戳为0，说明是从后台恢复，更新时间戳
                if (isActive && vpnStartTimestamp == 0L) {
                    vpnStartTimestamp = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                _errorMessage.value = "更新 VPN 状态失败: ${e.message}"
            }
        }
    }
    
    /**
     * 保存VPN启动时间到持久化存储
     */
    private fun saveVpnStartTime(context: Context, timestamp: Long) {
        try {
            val prefs = context.getSharedPreferences("vpn_state", Context.MODE_PRIVATE)
            prefs.edit().putLong("vpn_start_timestamp", timestamp).apply()
            android.util.Log.d("MainViewModel", "保存VPN启动时间: $timestamp")
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "保存VPN启动时间失败", e)
        }
    }
    
    /**
     * 从持久化存储读取VPN启动时间
     */
    private fun loadVpnStartTime(context: Context): Long {
        return try {
            val prefs = context.getSharedPreferences("vpn_state", Context.MODE_PRIVATE)
            val timestamp = prefs.getLong("vpn_start_timestamp", 0L)
            android.util.Log.d("MainViewModel", "读取VPN启动时间: $timestamp")
            timestamp
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "读取VPN启动时间失败", e)
            0L
        }
    }
    
    /**
     * 清除持久化存储的VPN启动时间
     */
    private fun clearVpnStartTime(context: Context) {
        try {
            val prefs = context.getSharedPreferences("vpn_state", Context.MODE_PRIVATE)
            prefs.edit().remove("vpn_start_timestamp").apply()
            android.util.Log.d("MainViewModel", "清除VPN启动时间")
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "清除VPN启动时间失败", e)
        }
    }
    
    /**
     * 获取 VPN 启动时间戳
     */
    fun getVpnStartTimestamp(): Long {
        return vpnStartTimestamp
    }
    
    /**
     * 检查初始VPN状态
     * 处理UI进程重启后的状态恢复
     */
    fun checkInitialVpnStatus(context: Context) {
        viewModelScope.launch {
            try {
                // 直接检查VPN进程是否存在，而不依赖UI进程的静态变量
                val isVpnRunning = isVpnProcessRunning(context)
                
                if (isVpnRunning) {
                    // VPN正在运行，恢复状态
                    _isVpnConnected.value = true
                    _isProxyActive.value = true
                    
                    // 从持久化存储读取真实的启动时间
                    val savedTimestamp = loadVpnStartTime(context)
                    vpnStartTimestamp = if (savedTimestamp > 0) {
                        savedTimestamp
                    } else {
                        // 如果没有保存的时间戳，使用当前时间
                        val currentTime = System.currentTimeMillis()
                        saveVpnStartTime(context, currentTime)
                        currentTime
                    }
                    
                    // 恢复统计数据收集
                    startClashStatsCollection()
                    
                    android.util.Log.d("MainViewModel", "检测到VPN进程正在运行，已恢复连接状态，启动时间: $vpnStartTimestamp")
                } else {
                    // VPN未运行，确保状态正确
                    _isVpnConnected.value = false
                    _isProxyActive.value = false
                    vpnStartTimestamp = 0L
                    
                    android.util.Log.d("MainViewModel", "VPN进程未运行，状态已重置")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "检查初始VPN状态失败", e)
                // 出错时默认为未连接状态
                _isVpnConnected.value = false
                _isProxyActive.value = false
                vpnStartTimestamp = 0L
            }
        }
    }
    
    /**
     * 检查VPN进程是否真的在运行
     * 直接检查系统进程，不依赖静态变量
     */
    private fun isVpnProcessRunning(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = activityManager.runningAppProcesses
            
            val packageName = context.packageName
            val targetProcessName = "$packageName:durgeservice"
            
            val found = runningApps?.any { it.processName == targetProcessName } ?: false
            android.util.Log.d("MainViewModel", "检查VPN进程: $targetProcessName, 找到: $found")
            
            found
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "检查VPN进程失败", e)
            false
        }
    }
    

    
    /**
     * 显示VPN启动错误（从MainActivity调用）
     */
    fun showVpnStartupError(errorMessage: String) {
        _vpnStartupError.value = errorMessage
        // 启动失败，重置状态
        _isVpnConnected.value = false
        _isProxyActive.value = false
        vpnStartTimestamp = 0L
    }
    
    /**
     * 清除VPN启动错误弹窗
     */
    fun clearVpnStartupError() {
        _vpnStartupError.value = null
    }
    

    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 开始收集Clash统计数据
     */
    private fun startClashStatsCollection() {
        viewModelScope.launch {
            // 等待VPN服务完全启动
            kotlinx.coroutines.delay(3000) // 等待3秒让服务完全启动
            
            clashApiService.getStatsFlow()
                .catch { e ->
                    android.util.Log.e("MainViewModel", "Clash统计数据流错误", e)
                    // 发送默认数据，避免UI显示空白
                    emit(ClashStats())
                }
                .collect { stats ->
                    _clashStats.value = stats
                }
        }
    }
    
    /**
     * 停止收集Clash统计数据
     */
    private fun stopClashStatsCollection() {
        // 重置统计数据
        _clashStats.value = ClashStats()
        clashApiService.cleanup()
    }
    
    override fun onCleared() {
        super.onCleared()
        // 清理资源
        durgeService = null
        clashApiService.cleanup()
        // 注意：这里无法获取context，需要在Activity/Fragment中主动调用unregisterErrorBroadcastReceiver
    }
}

/**
 * MainViewModel 工厂类
 */
class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 