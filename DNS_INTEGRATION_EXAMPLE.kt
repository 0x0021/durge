package qialanfan.durge.doge.examples

import android.content.Context
import android.util.Log
import qialanfan.durge.doge.services.DnsMonitorService

/**
 * DNS监控服务集成示例
 * 展示如何在Durge VPN项目中使用DNS监控功能
 */
class DnsIntegrationExample(private val context: Context) {
    
    private val dnsMonitor = DnsMonitorService.getInstance(context)
    private val TAG = "DnsIntegrationExample"
    
    /**
     * 示例1: 基础DNS监控 - 在VPN连接时监控DNS变化
     */
    fun startBasicDnsMonitoring() {
        Log.d(TAG, "=== 开始基础DNS监控示例 ===")
        
        // 添加DNS变化监听器
        dnsMonitor.addDnsChangeListener("vpn_dns_monitor", object : DnsMonitorService.DnsChangeListener {
            override fun onDnsChanged(oldDnsServers: List<String>, newDnsServers: List<String>) {
                Log.d(TAG, "检测到DNS变化:")
                Log.d(TAG, "  旧DNS: ${oldDnsServers.joinToString(", ")}")
                Log.d(TAG, "  新DNS: ${newDnsServers.joinToString(", ")}")
                
                // 实际应用场景：VPN应用可能需要根据DNS变化调整配置
                handleDnsChangeForVpn(oldDnsServers, newDnsServers)
            }
            
            override fun onDnsError(error: String) {
                Log.e(TAG, "DNS监控出错: $error")
                // 处理DNS监控错误
            }
        })
        
        // 开始监控
        dnsMonitor.startMonitoring()
        
        // 获取当前网络信息
        val currentDns = dnsMonitor.getCurrentDnsServers()
        val carrierInfo = dnsMonitor.getCarrierInfo()
        
        Log.d(TAG, "当前DNS服务器: ${currentDns.joinToString(", ")}")
        Log.d(TAG, "网络类型: $carrierInfo")
    }
    
    /**
     * 示例2: VPN场景下的DNS处理
     */
    private fun handleDnsChangeForVpn(oldDns: List<String>, newDns: List<String>) {
        Log.d(TAG, "=== VPN DNS处理示例 ===")
        
        // 检查是否从WiFi切换到移动网络（或反之）
        val networkType = dnsMonitor.getCarrierInfo()
        
        when {
            networkType.contains("WiFi") -> {
                Log.d(TAG, "当前使用WiFi网络，DNS: ${newDns.joinToString(", ")}")
                // WiFi环境下可能需要特殊处理
                handleWifiDnsChange(newDns)
            }
            networkType.contains("移动数据") -> {
                Log.d(TAG, "当前使用移动数据，DNS: ${newDns.joinToString(", ")}")
                // 移动网络环境下的处理
                handleCellularDnsChange(newDns)
            }
            networkType.contains("VPN") -> {
                Log.d(TAG, "检测到VPN网络，DNS: ${newDns.joinToString(", ")}")
                // VPN环境下的处理
                handleVpnDnsChange(newDns)
            }
        }
        
        // 检查DNS是否为运营商默认DNS
        checkIfCarrierDns(newDns)
    }
    
    /**
     * WiFi网络DNS变化处理
     */
    private fun handleWifiDnsChange(dnsServers: List<String>) {
        Log.d(TAG, "处理WiFi DNS变化...")
        
        // 常见的公共DNS检查
        val publicDnsServers = setOf(
            "8.8.8.8", "8.8.4.4",           // Google DNS
            "1.1.1.1", "1.0.0.1",           // Cloudflare DNS
            "223.5.5.5", "223.6.6.6",       // 阿里DNS
            "114.114.114.114", "114.114.115.115", // 114DNS
            "119.29.29.29"                  // 腾讯DNS
        )
        
        val isPublicDns = dnsServers.any { it in publicDnsServers }
        
        if (isPublicDns) {
            Log.d(TAG, "检测到公共DNS服务器")
        } else {
            Log.d(TAG, "使用路由器或ISP提供的DNS")
        }
    }
    
    /**
     * 移动网络DNS变化处理
     */
    private fun handleCellularDnsChange(dnsServers: List<String>) {
        Log.d(TAG, "处理移动网络DNS变化...")
        
        // 检测运营商类型
        val networkInfo = dnsMonitor.getCarrierInfo()
        Log.d(TAG, "移动网络详情: $networkInfo")
    }
    
    /**
     * VPN网络DNS变化处理
     */
    private fun handleVpnDnsChange(dnsServers: List<String>) {
        Log.d(TAG, "处理VPN DNS变化...")
        Log.w(TAG, "注意：当前可能存在DNS泄漏风险")
    }
    
    /**
     * 检查是否为运营商DNS
     */
    private fun checkIfCarrierDns(dnsServers: List<String>) {
        // 分析DNS服务器IP段来判断是否为运营商DNS
        for (dns in dnsServers) {
            when {
                dns in listOf("8.8.8.8", "8.8.4.4") -> 
                    Log.d(TAG, "DNS $dns 是Google公共DNS")
                dns in listOf("1.1.1.1", "1.0.0.1") -> 
                    Log.d(TAG, "DNS $dns 是Cloudflare公共DNS")
                dns.startsWith("223.5.") || dns.startsWith("223.6.") -> 
                    Log.d(TAG, "DNS $dns 是阿里公共DNS")
                else -> 
                    Log.d(TAG, "DNS $dns 可能是运营商DNS")
            }
        }
    }
    
    /**
     * 停止DNS监控
     */
    fun stopDnsMonitoring() {
        Log.d(TAG, "停止DNS监控")
        dnsMonitor.removeDnsChangeListener("vpn_dns_monitor")
        dnsMonitor.stopMonitoring()
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopDnsMonitoring()
        dnsMonitor.cleanup()
    }
}
