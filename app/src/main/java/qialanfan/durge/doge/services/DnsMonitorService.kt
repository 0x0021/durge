package qialanfan.durge.doge.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * DNS监控服务 - 用于获取运营商DNS并监听变化
 */
class DnsMonitorService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "DnsMonitorService"
        private const val DNS_CHECK_INTERVAL = 60000L // 60秒检查一次（节能优化）
        private const val FAST_CHECK_INTERVAL = 10000L // 网络变化后快速检查
        private const val MAX_CONSECUTIVE_CHECKS = 3 // 最多连续快速检查3次
        
        @Volatile
        private var INSTANCE: DnsMonitorService? = null
        
        fun getInstance(context: Context): DnsMonitorService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DnsMonitorService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // DNS变化回调接口
    interface DnsChangeListener {
        fun onDnsChanged(oldDnsServers: List<String>, newDnsServers: List<String>)
        fun onDnsError(error: String)
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = ConcurrentHashMap<String, DnsChangeListener>()
    private var currentDnsServers = mutableListOf<String>()
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    // 节能优化相关变量
    private var lastNetworkChangeTime = 0L
    private var consecutiveCheckCount = 0
    private var isInFastCheckMode = false
    private var lastDnsCheckTime = 0L
    private val dnsCache = mutableMapOf<String, String>() // 缓存系统属性结果
    
    // 网络状态监听器（节能优化）
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "网络连接可用")
            onNetworkChanged()
        }
        
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            Log.d(TAG, "网络链接属性变化")
            onNetworkChanged()
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "网络连接丢失")
            val oldDns = currentDnsServers.toList()
            currentDnsServers.clear()
            dnsCache.clear() // 清除缓存
            notifyDnsChanged(oldDns, emptyList())
        }
    }
    
    /**
     * 网络变化处理（节能优化）
     */
    private fun onNetworkChanged() {
        val currentTime = System.currentTimeMillis()
        lastNetworkChangeTime = currentTime
        
        // 网络变化后进入快速检查模式
        isInFastCheckMode = true
        consecutiveCheckCount = 0
        dnsCache.clear() // 网络变化时清除DNS缓存
        
        // 立即检查一次
        checkDnsServers()
    }
    
    // 网络状态广播接收器（兼容性备用方案）
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    Log.d(TAG, "网络连接状态广播")
                    checkDnsServers()
                }
            }
        }
    }
    
    /**
     * 添加DNS变化监听器
     */
    fun addDnsChangeListener(key: String, listener: DnsChangeListener) {
        listeners[key] = listener
        Log.d(TAG, "添加DNS监听器: $key")
    }
    
    /**
     * 移除DNS变化监听器
     */
    fun removeDnsChangeListener(key: String) {
        listeners.remove(key)
        Log.d(TAG, "移除DNS监听器: $key")
    }
    
    /**
     * 开始监控DNS变化
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "DNS监控已在运行")
            return
        }
        
        Log.d(TAG, "开始DNS监控")
        isMonitoring = true
        
        // 注册网络状态监听
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
                Log.d(TAG, "注册网络回调成功")
            } else {
                // Android N以下使用广播接收器
                val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                context.registerReceiver(networkReceiver, filter)
                Log.d(TAG, "注册网络广播接收器成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "注册网络监听失败: ${e.message}")
            notifyError("注册网络监听失败: ${e.message}")
        }
        
        // 启动智能定期检查任务（节能优化）
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    val currentTime = System.currentTimeMillis()
                    
                    // 决定检查间隔：网络变化后使用快速检查，否则使用正常间隔
                    val checkInterval = if (isInFastCheckMode) {
                        consecutiveCheckCount++
                        if (consecutiveCheckCount >= MAX_CONSECUTIVE_CHECKS) {
                            isInFastCheckMode = false
                            consecutiveCheckCount = 0
                            Log.d(TAG, "退出快速检查模式")
                        }
                        FAST_CHECK_INTERVAL
                    } else {
                        DNS_CHECK_INTERVAL
                    }
                    
                    // 避免过于频繁的检查
                    if (currentTime - lastDnsCheckTime >= checkInterval) {
                        checkDnsServers()
                        lastDnsCheckTime = currentTime
                    }
                    
                    delay(checkInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "DNS检查任务出错: ${e.message}")
                    notifyError("DNS检查任务出错: ${e.message}")
                    delay(DNS_CHECK_INTERVAL) // 出错时使用正常间隔
                }
            }
        }
        
        // 首次检查DNS
        checkDnsServers()
    }
    
    /**
     * 停止监控DNS变化
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            Log.d(TAG, "DNS监控未在运行")
            return
        }
        
        Log.d(TAG, "停止DNS监控")
        isMonitoring = false
        
        // 取消注册网络监听
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Log.d(TAG, "取消注册网络回调成功")
            } else {
                context.unregisterReceiver(networkReceiver)
                Log.d(TAG, "取消注册网络广播接收器成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "取消注册网络监听失败: ${e.message}")
        }
        
        // 取消定期检查任务
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * 获取当前DNS服务器列表
     */
    fun getCurrentDnsServers(): List<String> {
        return currentDnsServers.toList()
    }
    
    /**
     * 检查DNS服务器变化
     */
    private fun checkDnsServers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newDnsServers = getDnsServers()
                val oldDnsServers = currentDnsServers.toList()
                
                if (newDnsServers != oldDnsServers) {
                    Log.d(TAG, "DNS服务器发生变化")
                    Log.d(TAG, "旧DNS: $oldDnsServers")
                    Log.d(TAG, "新DNS: $newDnsServers")
                    
                    currentDnsServers.clear()
                    currentDnsServers.addAll(newDnsServers)
                    
                    notifyDnsChanged(oldDnsServers, newDnsServers)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查DNS服务器失败: ${e.message}")
                notifyError("检查DNS服务器失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取DNS服务器列表
     */
    private fun getDnsServers(): List<String> {
        val dnsServers = mutableSetOf<String>()
        
        try {
            // 方法1: 通过LinkProperties获取（Android 5.0+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getDnsFromLinkProperties()?.let { dns ->
                    dnsServers.addAll(dns)
                }
            }
            
            // 方法2: 通过系统属性获取
            getDnsFromSystemProperties()?.let { dns ->
                dnsServers.addAll(dns)
            }
            
            // 方法3: 通过网络接口获取
            getDnsFromNetworkInterface()?.let { dns ->
                dnsServers.addAll(dns)
            }
            
            Log.d(TAG, "获取到DNS服务器: $dnsServers")
            return dnsServers.toList()
            
        } catch (e: Exception) {
            Log.e(TAG, "获取DNS服务器失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 通过LinkProperties获取DNS
     */
    private fun getDnsFromLinkProperties(): List<String>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val activeNetwork = connectivityManager.activeNetwork
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                linkProperties?.dnsServers?.map { it.hostAddress ?: it.toString() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "通过LinkProperties获取DNS失败: ${e.message}")
            null
        }
    }
    
    /**
     * 通过系统属性获取DNS（参考ClashMetaForAndroid的实现）
     */
    private fun getDnsFromSystemProperties(): List<String>? {
        return try {
            val dnsServers = mutableListOf<String>()
            
            // 智能DNS属性获取（节能优化）
            val priorityDnsProperties = listOf(
                // 优先检查基础DNS属性（最常用）
                "net.dns1", "net.dns2", "net.dns3", "net.dns4"
            )
            
            val extendedDnsProperties = listOf(
                // WiFi DNS属性
                "dhcp.wlan0.dns1", "dhcp.wlan0.dns2",
                // 移动数据DNS属性  
                "dhcp.rmnet0.dns1", "dhcp.rmnet0.dns2",
                "dhcp.rmnet_data0.dns1", "dhcp.rmnet_data0.dns2",
                // 以太网DNS属性
                "dhcp.eth0.dns1", "dhcp.eth0.dns2",
                // VPN相关DNS
                "net.vpn.dns1", "net.vpn.dns2"
            )
            
            // 先检查基础DNS属性
            for (prop in priorityDnsProperties) {
                val dns = getSystemProperty(prop)
                if (!dns.isNullOrBlank() && isValidIpAddress(dns)) {
                    dnsServers.add(dns)
                }
            }
            
            // 如果基础DNS为空，再检查扩展属性
            if (dnsServers.isEmpty()) {
                for (prop in extendedDnsProperties) {
                    val dns = getSystemProperty(prop)
                    if (!dns.isNullOrBlank() && isValidIpAddress(dns)) {
                        dnsServers.add(dns)
                        // 找到有效DNS后，继续检查同接口的其他DNS
                        if (prop.contains("dns1")) {
                            val dns2Prop = prop.replace("dns1", "dns2")
                            val dns2 = getSystemProperty(dns2Prop)
                            if (!dns2.isNullOrBlank() && isValidIpAddress(dns2)) {
                                dnsServers.add(dns2)
                            }
                        }
                        break // 找到一个接口的DNS后就停止，避免过多查询
                    }
                }
            }
            
            // 去重并排序
            val uniqueDnsServers = dnsServers.distinct().sorted()
            Log.d(TAG, "系统属性DNS结果: $uniqueDnsServers")
            
            uniqueDnsServers.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "通过系统属性获取DNS失败: ${e.message}")
            null
        }
    }
    
    /**
     * 通过网络接口获取DNS
     */
    private fun getDnsFromNetworkInterface(): List<String>? {
        return try {
            val dnsServers = mutableListOf<String>()
            
            // 这是一个备用方法，实际效果有限
            // 主要作为兜底方案
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    // 这里可以添加更多网络接口相关的DNS获取逻辑
                    Log.d(TAG, "检查网络接口: ${networkInterface.name}")
                }
            }
            
            dnsServers.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "通过网络接口获取DNS失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取系统属性（节能优化版本）
     */
    private fun getSystemProperty(key: String): String? {
        // 先检查缓存
        dnsCache[key]?.let { cachedValue ->
            return if (cachedValue == "EMPTY") null else cachedValue
        }
        
        return try {
            // 方法1: 使用getprop命令（推荐）
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            val result = process.inputStream.bufferedReader().use { it.readLine()?.trim() }
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && !result.isNullOrBlank()) {
                dnsCache[key] = result // 缓存结果
                return result
            }
            
            // 方法2: 使用反射获取SystemProperties（备用方案）
            try {
                val systemPropertiesClass = Class.forName("android.os.SystemProperties")
                val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
                val reflectionResult = getMethod.invoke(null, key) as? String
                
                if (!reflectionResult.isNullOrBlank()) {
                    dnsCache[key] = reflectionResult // 缓存结果
                    return reflectionResult
                }
            } catch (e: Exception) {
                // 静默处理反射错误，避免日志spam
            }
            
            // 缓存空结果，避免重复查询
            dnsCache[key] = "EMPTY"
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取系统属性 $key 失败: ${e.message}")
            dnsCache[key] = "EMPTY" // 缓存失败结果
            null
        }
    }
    
    /**
     * 验证IP地址格式
     */
    private fun isValidIpAddress(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 通知DNS变化
     */
    private fun notifyDnsChanged(oldDns: List<String>, newDns: List<String>) {
        CoroutineScope(Dispatchers.Main).launch {
            listeners.values.forEach { listener ->
                try {
                    listener.onDnsChanged(oldDns, newDns)
                } catch (e: Exception) {
                    Log.e(TAG, "通知DNS变化失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 通知错误
     */
    private fun notifyError(error: String) {
        CoroutineScope(Dispatchers.Main).launch {
            listeners.values.forEach { listener ->
                try {
                    listener.onDnsError(error)
                } catch (e: Exception) {
                    Log.e(TAG, "通知DNS错误失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 获取详细的网络和运营商信息（参考ClashMeta的实现）
     */
    fun getCarrierInfo(): String {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            
            val networkType = when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                    val wifiInfo = getWifiInfo()
                    "WiFi网络${if (wifiInfo.isNotEmpty()) " ($wifiInfo)" else ""}"
                }
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                    val carrierInfo = getCellularCarrierInfo()
                    "移动数据网络${if (carrierInfo.isNotEmpty()) " ($carrierInfo)" else ""}"
                }
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> {
                    "以太网"
                }
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> {
                    "VPN网络"
                }
                else -> "未知网络类型"
            }
            
            // 添加接口名称信息
            val interfaceName = linkProperties?.interfaceName
            val fullInfo = if (interfaceName != null) {
                "$networkType [接口: $interfaceName]"
            } else {
                networkType
            }
            
            Log.d(TAG, "网络信息: $fullInfo")
            fullInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "获取运营商信息失败: ${e.message}")
            "获取失败"
        }
    }
    
    /**
     * 获取WiFi详细信息
     */
    private fun getWifiInfo(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            
            if (connectionInfo != null) {
                val ssid = connectionInfo.ssid?.replace("\"", "") ?: "未知"
                val bssid = connectionInfo.bssid ?: "未知"
                "SSID: $ssid"
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取WiFi信息失败: ${e.message}")
            ""
        }
    }
    
    /**
     * 获取移动网络运营商信息
     */
    private fun getCellularCarrierInfo(): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            
            if (telephonyManager != null) {
                val carrierName = telephonyManager.networkOperatorName ?: "未知运营商"
                val networkType = getNetworkTypeString(telephonyManager)
                "$carrierName $networkType"
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取移动网络信息失败: ${e.message}")
            ""
        }
    }
    
    /**
     * 获取网络类型字符串
     */
    private fun getNetworkTypeString(telephonyManager: android.telephony.TelephonyManager): String {
        return try {
            when (telephonyManager.networkType) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                android.telephony.TelephonyManager.NETWORK_TYPE_EDGE,
                android.telephony.TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 设置节能模式
     * @param enabled true为启用节能模式，false为正常模式
     */
    fun setPowerSavingMode(enabled: Boolean) {
        if (enabled) {
            Log.d(TAG, "启用节能模式 - 减少DNS检查频率")
            isInFastCheckMode = false
            consecutiveCheckCount = 0
        } else {
            Log.d(TAG, "关闭节能模式 - 恢复正常检查频率")
        }
    }
    
    /**
     * 手动刷新DNS（立即检查一次）
     */
    fun refreshDns() {
        Log.d(TAG, "手动刷新DNS")
        dnsCache.clear() // 清除缓存强制重新获取
        checkDnsServers()
    }
    
    /**
     * 获取监控状态信息
     */
    fun getMonitoringStatus(): String {
        return if (isMonitoring) {
            val mode = if (isInFastCheckMode) "快速检查模式" else "正常模式"
            val cacheSize = dnsCache.size
            "运行中 ($mode, 缓存:$cacheSize)"
        } else {
            "已停止"
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopMonitoring()
        listeners.clear()
        currentDnsServers.clear()
        dnsCache.clear()
        INSTANCE = null
        Log.d(TAG, "DNS监控服务已清理")
    }
}
