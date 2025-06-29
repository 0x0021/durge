/**
 * Clash API 测试示例
 * 
 * 这个文件展示了如何手动测试Clash API服务
 * 可以在Android Studio的Kotlin Playground中运行
 */

import kotlinx.coroutines.runBlocking
import qialanfan.durge.doge.services.ClashApiService

fun main() {
    val apiService = ClashApiService.getInstance()
    
    runBlocking {
        println("=== Clash API 测试 ===")
        
        // 1. 测试服务可用性
        println("\n1. 检查服务可用性...")
        val isAvailable = apiService.isServiceAvailable()
        println("服务状态: ${if (isAvailable) "可用" else "不可用"}")
        
        if (!isAvailable) {
            println("❌ 请确保VPN服务已启动且8899端口可访问")
            return@runBlocking
        }
        
        // 2. 测试基本信息
        println("\n2. 获取基本信息...")
        val infoResult = apiService.getInfo()
        if (infoResult.isSuccess) {
            val info = infoResult.getOrNull()
            println("✅ 版本: ${info?.version}")
            println("✅ 模式: ${info?.mode}")
            println("✅ 端口: ${info?.port}")
            println("✅ Socks端口: ${info?.socksPort}")
        } else {
            println("❌ 获取基本信息失败: ${infoResult.exceptionOrNull()?.message}")
        }
        
        // 3. 测试流量信息
        println("\n3. 获取流量信息...")
        val trafficResult = apiService.getTraffic()
        if (trafficResult.isSuccess) {
            val traffic = trafficResult.getOrNull()
            println("✅ 下载: ${traffic?.down} bytes")
            println("✅ 上传: ${traffic?.up} bytes")
        } else {
            println("❌ 获取流量信息失败: ${trafficResult.exceptionOrNull()?.message}")
        }
        
        // 4. 测试连接信息
        println("\n4. 获取连接信息...")
        val connectionsResult = apiService.getConnections()
        if (connectionsResult.isSuccess) {
            val connections = connectionsResult.getOrNull()
            println("✅ 活跃连接数: ${connections?.connections?.size}")
            println("✅ 总下载: ${connections?.downloadTotal} bytes")
            println("✅ 总上传: ${connections?.uploadTotal} bytes")
            
            // 显示前3个连接的详细信息
            connections?.connections?.take(3)?.forEachIndexed { index, conn ->
                println("   连接${index + 1}: ${conn.metadata.destinationIP}:${conn.metadata.destinationPort}")
            }
        } else {
            println("❌ 获取连接信息失败: ${connectionsResult.exceptionOrNull()?.message}")
        }
        
        // 5. 测试统计数据流（运行5秒）
        println("\n5. 测试统计数据流（5秒）...")
        var count = 0
        try {
            apiService.getStatsFlow().collect { stats ->
                count++
                println("📊 [${count}] 连接: ${stats.requestCount}, 总流量: ${stats.totalTraffic}, ⬇️${stats.downloadSpeed}, ⬆️${stats.uploadSpeed}")
                
                if (count >= 5) {
                    println("✅ 数据流测试完成")
                    return@collect
                }
            }
        } catch (e: Exception) {
            println("❌ 数据流测试失败: ${e.message}")
        }
        
        println("\n=== 测试完成 ===")
    }
}

/**
 * 手动测试步骤（在真实设备上）：
 * 
 * 1. 确保VPN服务已启动
 * 2. 在logcat中搜索 "ClashApiService" 标签
 * 3. 启动VPN后观察日志输出
 * 4. 使用网络（浏览器、应用）产生流量
 * 5. 观察统计数据是否实时更新
 * 
 * 预期结果：
 * - 连接数应该 > 0
 * - 流量数据应该随使用增长
 * - 速度应该在有网络活动时 > 0
 */

/**
 * 常见问题排查：
 * 
 * 1. "服务不可用"
 *    - 检查VPN是否真的启动了
 *    - 确认8899端口没有被其他应用占用
 *    - 检查防火墙设置
 * 
 * 2. "获取数据失败"
 *    - 检查网络权限
 *    - 确认API响应格式
 *    - 查看详细错误日志
 * 
 * 3. "数据不更新"
 *    - 确认有网络活动
 *    - 检查数据流是否正常运行
 *    - 重启VPN服务
 */

/**
 * 性能监控：
 * 
 * 在Android Studio的Profiler中监控：
 * - CPU使用率（应该很低）
 * - 内存使用（不应该持续增长）
 * - 网络请求频率（每秒1次）
 */ 