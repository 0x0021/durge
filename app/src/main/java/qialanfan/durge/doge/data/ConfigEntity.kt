package qialanfan.durge.doge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configs")
data class ConfigEntity(
    @PrimaryKey
    val id: String, // 使用UUID作为唯一标识
    val name: String, // 配置名称
    val folderPath: String, // 配置文件夹的绝对路径
    val configFilePath: String, // 配置文件的绝对路径
    val createdAt: Long, // 创建时间戳
    val updatedAt: Long, // 更新时间戳
    val isActive: Boolean = false, // 是否为当前激活的配置
    val description: String? = null // 配置描述
) 