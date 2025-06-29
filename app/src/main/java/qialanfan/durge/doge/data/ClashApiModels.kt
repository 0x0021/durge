package qialanfan.durge.doge.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Clash API 数据模型
 * 参考 zashboard 项目的 API 结构
 */

@Serializable
data class ClashTraffic(
    @SerialName("up") val up: Long = 0L,
    @SerialName("down") val down: Long = 0L
)

@Serializable
data class ClashMemory(
    @SerialName("inuse") val inuse: Long = 0L,
    @SerialName("oslimit") val oslimit: Long = 0L
)

@Serializable
data class ClashInfo(
    @SerialName("version") val version: String = "",
    @SerialName("premium") val premium: Boolean = false,
    @SerialName("mode") val mode: String = "rule",
    @SerialName("port") val port: Int = 0,
    @SerialName("socks-port") val socksPort: Int = 0,
    @SerialName("redir-port") val redirPort: Int = 0,
    @SerialName("tproxy-port") val tproxyPort: Int = 0,
    @SerialName("mixed-port") val mixedPort: Int = 0,
    @SerialName("allow-lan") val allowLan: Boolean = false,
    @SerialName("bind-address") val bindAddress: String = "*",
    @SerialName("log-level") val logLevel: String = "info"
)

@Serializable
data class ClashConnection(
    @SerialName("id") val id: String,
    @SerialName("metadata") val metadata: ConnectionMetadata,
    @SerialName("upload") val upload: Long,
    @SerialName("download") val download: Long,
    @SerialName("start") val start: String,
    @SerialName("chains") val chains: List<String>,
    @SerialName("rule") val rule: String,
    @SerialName("rulePayload") val rulePayload: String? = null
)

@Serializable
data class ConnectionMetadata(
    @SerialName("network") val network: String,
    @SerialName("type") val type: String,
    @SerialName("sourceIP") val sourceIP: String,
    @SerialName("destinationIP") val destinationIP: String,
    @SerialName("sourcePort") val sourcePort: String,
    @SerialName("destinationPort") val destinationPort: String,
    @SerialName("host") val host: String? = null,
    @SerialName("dnsMode") val dnsMode: String? = null,
    @SerialName("uid") val uid: Int? = null,
    @SerialName("process") val process: String? = null,
    @SerialName("processPath") val processPath: String? = null,
    @SerialName("specialProxy") val specialProxy: String? = null,
    @SerialName("specialRules") val specialRules: String? = null,
    @SerialName("remoteDestination") val remoteDestination: String? = null,
    @SerialName("sniffHost") val sniffHost: String? = null
)

@Serializable
data class ClashConnectionsResponse(
    @SerialName("downloadTotal") val downloadTotal: Long,
    @SerialName("uploadTotal") val uploadTotal: Long,
    @SerialName("connections") val connections: List<ClashConnection>
)

/**
 * 统计数据汇总
 */
data class ClashStats(
    val requestCount: Int = 0,
    val totalTraffic: String = "0 B",
    val downloadSpeed: String = "0 B/s",
    val uploadSpeed: String = "0 B/s",
    val downloadTotal: Long = 0L,
    val uploadTotal: Long = 0L,
    val activeConnections: Int = 0
)

/**
 * 流量格式化工具
 */
object TrafficFormatter {
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
    
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }
} 