package qialanfan.durge.doge.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Clash风格的DNS监控器 - 纯事件驱动，无轮询
 * 参考ClashMetaForAndroid的实现方式
 */
class ClashStyleDnsMonitor private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ClashStyleDnsMonitor"
        
        @Volatile
        private var INSTANCE: ClashStyleDnsMonitor? = null
        
        fun getInstance(context: Context): ClashStyleDnsMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClashStyleDnsMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // DNS变化监听接口
    interface DnsChangeListener {
        fun onDnsChanged(networkType: String, dnsServers: List<String>, linkProperties: LinkProperties)
        fun onNetworkAvailable(network: Network, networkType: String)
        fun onNetworkLost(network: Network)
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = mutableSetOf<DnsChangeListener>()
    private var currentNetwork: Network? = null
    private var currentDnsServers = listOf<String>()
    private var isMonitoring = false
    
    // 网络切换延迟检查
    private val handler = Handler(Looper.getMainLooper())
    private var networkCheckRunnable: Runnable? = null
    private val NETWORK_CHECK_DELAY = 2000L // 2秒后检查新网络
    
    // 网络回调监听器（Clash风格 - 纯事件驱动）
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            currentNetwork = network
            
            val networkType = getNetworkTypeString(network)
            Log.d(TAG, "[网络连接] 网络连接可用: $networkType")
            
            // 立即获取DNS信息
            getDnsFromNetwork(network)
            
            // 通知监听器
            listeners.forEach { listener ->
                try {
                    listener.onNetworkAvailable(network, networkType)
                } catch (e: Exception) {
                    Log.e(TAG, "通知网络可用失败", e)
                }
            }
        }
        
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            
            val networkType = getNetworkTypeString(network)
            Log.d(TAG, "[网络变化] 网络链接属性变化: $networkType")
            
            // 从LinkProperties直接获取DNS
            val newDnsServers = linkProperties.dnsServers.mapNotNull { 
                it.hostAddress 
            }.distinct()
            
            // 检查DNS是否真的变化了
            if (newDnsServers != currentDnsServers) {
                val oldDns = currentDnsServers
                currentDnsServers = newDnsServers
                
                Log.d(TAG, "[DNS变化] DNS服务器变化:")
                Log.d(TAG, "  旧DNS: $oldDns")
                Log.d(TAG, "  新DNS: $newDnsServers")
                Log.d(TAG, "  网络类型: $networkType")
                Log.d(TAG, "  接口名称: ${linkProperties.interfaceName}")
                
                // 打印详细的网络信息
                printDetailedNetworkInfo(linkProperties, networkType)
                
                // 通知所有监听器
                listeners.forEach { listener ->
                    try {
                        listener.onDnsChanged(networkType, newDnsServers, linkProperties)
                    } catch (e: Exception) {
                        Log.e(TAG, "通知DNS变化失败", e)
                    }
                }
            } else {
                Log.v(TAG, "DNS无变化，跳过通知")
            }
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            
            val networkType = getNetworkTypeString(network)
            Log.d(TAG, "[网络能力] 网络能力变化: $networkType")
            
            // 打印网络能力信息
            printNetworkCapabilities(networkCapabilities)
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            
            Log.d(TAG, "[网络丢失] 网络连接丢失")
            
            // 检查是否是当前活动网络丢失
            if (network == currentNetwork) {
                Log.d(TAG, "[网络丢失] 当前活动网络丢失，清除DNS记录")
                val oldDns = currentDnsServers
                currentDnsServers = emptyList()
                currentNetwork = null
                
                // 通知DNS变化（清空）
                if (oldDns.isNotEmpty()) {
                    listeners.forEach { listener ->
                        try {
                            listener.onDnsChanged("网络丢失", emptyList(), 
                                android.net.LinkProperties()) // 空的LinkProperties
                        } catch (e: Exception) {
                            Log.e(TAG, "通知DNS清空失败", e)
                        }
                    }
                }
                
                // 延迟检查是否有新的活动网络（处理网络切换场景）
                scheduleNetworkCheck()
            }
            
            // 通知监听器网络丢失
            listeners.forEach { listener ->
                try {
                    listener.onNetworkLost(network)
                } catch (e: Exception) {
                    Log.e(TAG, "通知网络丢失失败", e)
                }
            }
        }
    }
    
    /**
     * 开始监控（Clash风格 - 纯事件驱动）
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "DNS监控已在运行")
            return
        }
        
        Log.d(TAG, "[启动监控] 启动Clash风格DNS监控（纯事件驱动）")
        isMonitoring = true
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 注册网络回调，监听所有网络变化
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()
                
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
                Log.d(TAG, "[启动监控] 网络回调注册成功")
                
                // 获取当前活动网络的初始状态
                initializeCurrentNetwork()
                
            } else {
                Log.w(TAG, "[启动监控] Android版本过低，不支持NetworkCallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动DNS监控失败", e)
            isMonitoring = false
        }
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }
        
        Log.d(TAG, "[停止监控] 停止DNS监控")
        isMonitoring = false
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Log.d(TAG, "[停止监控] 网络回调注销成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止DNS监控失败", e)
        }
        
        currentNetwork = null
        currentDnsServers = emptyList()
    }
    
    /**
     * 添加DNS变化监听器
     */
    fun addDnsChangeListener(listener: DnsChangeListener) {
        listeners.add(listener)
        Log.d(TAG, "添加DNS监听器，当前监听器数量: ${listeners.size}")
    }
    
    /**
     * 移除DNS变化监听器
     */
    fun removeDnsChangeListener(listener: DnsChangeListener) {
        listeners.remove(listener)
        Log.d(TAG, "移除DNS监听器，当前监听器数量: ${listeners.size}")
    }
    
    /**
     * 获取当前DNS服务器
     */
    fun getCurrentDnsServers(): List<String> {
        return currentDnsServers
    }
    
    /**
     * 手动刷新当前网络的DNS信息
     */
    fun refreshCurrentNetworkDns() {
        currentNetwork?.let { network ->
            Log.d(TAG, "[手动刷新] 手动刷新当前网络DNS")
            getDnsFromNetwork(network)
        } ?: run {
            Log.w(TAG, "当前无活动网络，尝试检查新网络")
            checkForNewActiveNetwork()
        }
    }
    
    /**
     * 强制检查网络变化（用于处理网络切换延迟）
     */
    fun forceCheckNetworkChange() {
        Log.d(TAG, "[强制检查] 立即检查网络变化")
        checkForNewActiveNetwork()
    }
    
    /**
     * 初始化当前网络状态
     */
    private fun initializeCurrentNetwork() {
        try {
            // 在VPN环境下，优先查找底层网络
            val allNetworks = connectivityManager.allNetworks
            var foundUnderlyingNetwork = false
            
            for (network in allNetworks) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val linkProperties = connectivityManager.getLinkProperties(network)
                
                // 跳过VPN网络，寻找真正的底层网络
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                    continue
                }
                
                // 检查是否是有效的网络（WiFi或移动数据）
                val isValidNetwork = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                                   capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                
                if (isValidNetwork && linkProperties != null) {
                    Log.d(TAG, "[初始化] 发现底层网络（非VPN）")
                    currentNetwork = network
                    foundUnderlyingNetwork = true
                    
                    // 获取底层网络的真实DNS信息
                    getDnsFromUnderlyingNetwork(network, linkProperties)
                    break
                }
            }
            
            if (!foundUnderlyingNetwork) {
                // 如果没找到底层网络，使用activeNetwork
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    Log.d(TAG, "[初始化] 初始化当前活动网络（可能是VPN）")
                    currentNetwork = activeNetwork
                    getDnsFromNetwork(activeNetwork)
                } else {
                    Log.w(TAG, "无活动网络")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化当前网络失败", e)
        }
    }
    
    /**
     * 从指定网络获取DNS信息
     */
    private fun getDnsFromNetwork(network: Network) {
        try {
            val linkProperties = connectivityManager.getLinkProperties(network)
            if (linkProperties != null) {
                val dnsServers = linkProperties.dnsServers.mapNotNull { 
                    it.hostAddress 
                }.distinct()
                
                val networkType = getNetworkTypeString(network)
                
                Log.d(TAG, "[DNS信息] 网络DNS信息:")
                Log.d(TAG, "  网络类型: $networkType")
                Log.d(TAG, "  接口名称: ${linkProperties.interfaceName}")
                Log.d(TAG, "  DNS服务器: $dnsServers")
                
                // 更新当前DNS记录
                if (dnsServers != currentDnsServers) {
                    val oldDns = currentDnsServers
                    currentDnsServers = dnsServers
                    
                    // 通知监听器
                    listeners.forEach { listener ->
                        try {
                            listener.onDnsChanged(networkType, dnsServers, linkProperties)
                        } catch (e: Exception) {
                            Log.e(TAG, "通知DNS变化失败", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取网络DNS失败", e)
        }
    }
    
    /**
     * 从底层网络获取真实DNS信息（VPN环境下使用）
     */
    private fun getDnsFromUnderlyingNetwork(network: Network, linkProperties: LinkProperties) {
        try {
            val dnsServers = linkProperties.dnsServers.mapNotNull { 
                it.hostAddress 
            }.distinct()
            
            val networkType = getNetworkTypeString(network)
            
            Log.d(TAG, "[底层DNS] 底层网络DNS信息:")
            Log.d(TAG, "  网络类型: $networkType")
            Log.d(TAG, "  接口名称: ${linkProperties.interfaceName}")
            Log.d(TAG, "  真实DNS服务器: $dnsServers")
            
            // 检查是否是真正的运营商/路由器DNS（不是VPN DNS）
            val isRealDns = dnsServers.isNotEmpty() && !dnsServers.all { it.startsWith("192.18.") }
            
            if (isRealDns) {
                Log.d(TAG, "[底层DNS] 发现真实的底层DNS，非VPN DNS")
                
                // 更新当前DNS记录
                if (dnsServers != currentDnsServers) {
                    val oldDns = currentDnsServers
                    currentDnsServers = dnsServers
                    
                    // 通知监听器
                    listeners.forEach { listener ->
                        try {
                            listener.onDnsChanged(networkType, dnsServers, linkProperties)
                        } catch (e: Exception) {
                            Log.e(TAG, "通知DNS变化失败", e)
                        }
                    }
                }
            } else {
                Log.d(TAG, "[底层DNS] 底层网络DNS被VPN覆盖或为空，跳过更新")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取底层网络DNS失败", e)
        }
    }
    
    /**
     * 获取网络类型字符串
     */
    private fun getNetworkTypeString(network: Network): String {
        return try {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动数据"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                else -> "未知"
            }
        } catch (e: Exception) {
            "未知"
        }
    }
    
    /**
     * 打印详细的网络信息
     */
    private fun printDetailedNetworkInfo(linkProperties: LinkProperties, networkType: String) {
        Log.d(TAG, "[详细信息] 详细网络信息:")
        Log.d(TAG, "  网络类型: $networkType")
        Log.d(TAG, "  接口名称: ${linkProperties.interfaceName}")
        Log.d(TAG, "  域名: ${linkProperties.domains}")
        Log.d(TAG, "  MTU: ${linkProperties.mtu}")
        
        // 打印路由信息
        linkProperties.routes.forEachIndexed { index, route ->
            Log.d(TAG, "  路由$index: ${route.destination} -> ${route.gateway}")
        }
        
        // 打印链接地址
        linkProperties.linkAddresses.forEachIndexed { index, addr ->
            Log.d(TAG, "  地址$index: ${addr.address.hostAddress}/${addr.prefixLength}")
        }
    }
    
    /**
     * 打印网络能力信息
     */
    private fun printNetworkCapabilities(capabilities: NetworkCapabilities) {
        val transports = mutableListOf<String>()
        
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            transports.add("WiFi")
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            transports.add("移动数据")
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            transports.add("以太网")
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            transports.add("VPN")
        }
        
        Log.d(TAG, "  传输类型: ${transports.joinToString(", ")}")
        Log.d(TAG, "  下行带宽: ${capabilities.linkDownstreamBandwidthKbps} Kbps")
        Log.d(TAG, "  上行带宽: ${capabilities.linkUpstreamBandwidthKbps} Kbps")
        Log.d(TAG, "  网络已验证: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
        Log.d(TAG, "  网络可访问互联网: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
    }
    
    /**
     * 获取监控状态
     */
    fun getMonitoringStatus(): String {
        return if (isMonitoring) {
            "运行中 (事件驱动, 监听器:${listeners.size}, DNS:${currentDnsServers.size})"
        } else {
            "已停止"
        }
    }
    
    /**
     * 延迟检查新网络（处理网络切换场景）
     */
    private fun scheduleNetworkCheck() {
        // 取消之前的检查任务
        networkCheckRunnable?.let { handler.removeCallbacks(it) }
        
        // 创建新的检查任务
        networkCheckRunnable = Runnable {
            Log.d(TAG, "[延迟检查] 检查是否有新的活动网络")
            checkForNewActiveNetwork()
        }
        
        // 延迟执行检查
        handler.postDelayed(networkCheckRunnable!!, NETWORK_CHECK_DELAY)
        Log.d(TAG, "[延迟检查] 已安排${NETWORK_CHECK_DELAY}ms后检查新网络")
    }
    
    /**
     * 检查新的活动网络
     */
    private fun checkForNewActiveNetwork() {
        try {
            // 在VPN环境下，需要检查所有网络，而不只是activeNetwork
            val allNetworks = connectivityManager.allNetworks
            var foundNewNetwork = false
            
            for (network in allNetworks) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val linkProperties = connectivityManager.getLinkProperties(network)
                
                // 跳过VPN网络，寻找真正的底层网络
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                    continue
                }
                
                // 检查是否是有效的网络（WiFi或移动数据）
                val isValidNetwork = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                                   capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                
                if (isValidNetwork && network != currentNetwork && linkProperties != null) {
                    Log.d(TAG, "[延迟检查] 发现新的底层网络（非VPN）")
                    
                    // 更新当前网络
                    currentNetwork = network
                    foundNewNetwork = true
                    
                    // 获取底层网络的真实DNS信息
                    getDnsFromUnderlyingNetwork(network, linkProperties)
                    
                    // 通知网络可用
                    val networkType = getNetworkTypeString(network)
                    listeners.forEach { listener ->
                        try {
                            listener.onNetworkAvailable(network, networkType)
                        } catch (e: Exception) {
                            Log.e(TAG, "通知新网络可用失败", e)
                        }
                    }
                    break // 找到一个有效网络就停止
                }
            }
            
            if (!foundNewNetwork) {
                // 如果没找到新的底层网络，检查activeNetwork
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null && activeNetwork != currentNetwork) {
                    Log.d(TAG, "[延迟检查] 发现新的活动网络（可能是VPN）")
                    
                    // 更新当前网络
                    currentNetwork = activeNetwork
                    
                    // 获取网络的DNS信息
                    getDnsFromNetwork(activeNetwork)
                    
                    // 通知网络可用
                    val networkType = getNetworkTypeString(activeNetwork)
                    listeners.forEach { listener ->
                        try {
                            listener.onNetworkAvailable(activeNetwork, networkType)
                        } catch (e: Exception) {
                            Log.e(TAG, "通知新网络可用失败", e)
                        }
                    }
                } else if (activeNetwork == null) {
                    Log.d(TAG, "[延迟检查] 仍然没有活动网络")
                } else {
                    Log.d(TAG, "[延迟检查] 网络没有变化")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查新活动网络失败", e)
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 取消延迟检查任务
        networkCheckRunnable?.let { handler.removeCallbacks(it) }
        networkCheckRunnable = null
        
        stopMonitoring()
        listeners.clear()
        INSTANCE = null
        Log.d(TAG, "[清理] Clash风格DNS监控器已清理")
    }
} 