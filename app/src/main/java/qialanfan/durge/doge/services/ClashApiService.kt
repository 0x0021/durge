package qialanfan.durge.doge.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import qialanfan.durge.doge.data.ClashConnectionsResponse
import qialanfan.durge.doge.data.ClashInfo
import qialanfan.durge.doge.data.ClashStats
import qialanfan.durge.doge.data.ClashTraffic
import qialanfan.durge.doge.data.TrafficFormatter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Clash API 服务
 * 从本地8899端口获取VPN统计数据
 */
class ClashApiService {
    companion object {
        private const val TAG = "ClashApiService"
        private const val BASE_URL = "http://127.0.0.1:8899"
        private const val TIMEOUT = 5000 // 5秒超时
        
        @Volatile
        private var INSTANCE: ClashApiService? = null
        
        fun getInstance(): ClashApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClashApiService().also { INSTANCE = it }
            }
        }
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private var lastTraffic: ClashTraffic? = null
    private var lastUpdateTime: Long = 0L
    
    /**
     * 获取基本信息
     */
    suspend fun getInfo(): Result<ClashInfo> {
        return try {
            val response = makeRequest("$BASE_URL/")
            val info = json.decodeFromString<ClashInfo>(response)
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "获取基本信息失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取流量信息
     */
    suspend fun getTraffic(): Result<ClashTraffic> {
        return try {
            val response = makeRequest("$BASE_URL/traffic")
            val traffic = json.decodeFromString<ClashTraffic>(response)
            Result.success(traffic)
        } catch (e: Exception) {
            Log.e(TAG, "获取流量信息失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取连接信息
     */
    suspend fun getConnections(): Result<ClashConnectionsResponse> {
        return try {
            val response = makeRequest("$BASE_URL/connections")
            val connections = json.decodeFromString<ClashConnectionsResponse>(response)
            Result.success(connections)
        } catch (e: Exception) {
            Log.e(TAG, "获取连接信息失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取统计数据流
     * 每秒更新一次
     */
    fun getStatsFlow(): Flow<ClashStats> = flow {
        while (true) {
            try {
                val stats = calculateStats()
                emit(stats)
                delay(1000) // 每秒更新
            } catch (e: Exception) {
                Log.e(TAG, "获取统计数据失败", e)
                // 发送默认数据，避免UI显示空白
                emit(ClashStats())
                delay(2000) // 错误时延长间隔
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 计算统计数据
     */
    private suspend fun calculateStats(): ClashStats {
        val currentTime = System.currentTimeMillis()
        
        // 获取连接信息
        val connectionsResult = getConnections()
        val connections = connectionsResult.getOrNull()
        
        // 获取当前流量
        val trafficResult = getTraffic()
        val currentTraffic = trafficResult.getOrNull()
        
        // 计算速度
        var downloadSpeed = 0L
        var uploadSpeed = 0L
        
        if (currentTraffic != null && lastTraffic != null) {
            val timeDelta = (currentTime - lastUpdateTime) / 1000.0 // 秒
            if (timeDelta > 0) {
                downloadSpeed = ((currentTraffic.down - lastTraffic!!.down) / timeDelta).toLong()
                uploadSpeed = ((currentTraffic.up - lastTraffic!!.up) / timeDelta).toLong()
            }
        }
        
        // 更新缓存
        lastTraffic = currentTraffic
        lastUpdateTime = currentTime
        
        return ClashStats(
            requestCount = connections?.connections?.size ?: 0,
            totalTraffic = TrafficFormatter.formatBytes(
                (currentTraffic?.down ?: 0L) + (currentTraffic?.up ?: 0L)
            ),
            downloadSpeed = TrafficFormatter.formatSpeed(downloadSpeed),
            uploadSpeed = TrafficFormatter.formatSpeed(uploadSpeed),
            downloadTotal = currentTraffic?.down ?: 0L,
            uploadTotal = currentTraffic?.up ?: 0L,
            activeConnections = connections?.connections?.size ?: 0
        )
    }
    
    /**
     * 发送HTTP请求
     */
    private suspend fun makeRequest(urlString: String): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Durge-Android/1.1")
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()
                response
            } else {
                connection.disconnect()
                throw Exception("HTTP错误: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "网络请求失败: $urlString", e)
            throw e
        }
    }
    
    /**
     * 检查服务是否可用
     */
    suspend fun isServiceAvailable(): Boolean {
        return try {
            makeRequest("$BASE_URL/")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Clash API服务不可用: ${e.message}")
            false
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        lastTraffic = null
        lastUpdateTime = 0L
    }
} 