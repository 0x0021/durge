package qialanfan.durge.doge.manager

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import qialanfan.durge.doge.data.ConfigDao
import qialanfan.durge.doge.data.ConfigEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.ZipInputStream
import android.util.Log

class ConfigManager(
    private val context: Context,
    private val configDao: ConfigDao
) {
    
    // 配置文件存储的根目录
    private val configsRootDir = File(context.filesDir, "clash_configs")
    
    init {
        // 确保根目录存在
        if (!configsRootDir.exists()) {
            configsRootDir.mkdirs()
        }
    }
    
    /**
     * 创建默认配置
     */
    suspend fun createDefaultConfig(): Result<ConfigEntity> = withContext(Dispatchers.IO) {
        try {
            // 检查是否已存在默认配置
            val existingDefault = configDao.getConfigByName("Default")
            if (existingDefault != null) {
                return@withContext Result.success(existingDefault)
            }
            
            // 生成唯一的配置ID和文件夹名
            val configId = UUID.randomUUID().toString()
            val folderName = "config_${configId}"
            val configFolder = File(configsRootDir, folderName)
            
            // 创建配置专用文件夹
            if (!configFolder.exists()) {
                configFolder.mkdirs()
            }
            
            // 创建默认配置文件
            val configFile = File(configFolder, "config.yaml")
            val defaultConfigContent = createDefaultConfigContent()
            configFile.writeText(defaultConfigContent)
            
            val currentTime = System.currentTimeMillis()
            
            // 创建配置实体
            val configEntity = ConfigEntity(
                id = configId,
                name = "Default",
                folderPath = configFolder.absolutePath,
                configFilePath = configFile.absolutePath,
                createdAt = currentTime,
                updatedAt = currentTime,
                isActive = true, // 默认配置设为激活状态
                description = "系统默认配置"
            )
            
            // 保存到数据库
            configDao.insertConfig(configEntity)
            
            // 解压 resources.zip 到配置目录
            val extractResult = extractResourcesZipToConfigDir(configFolder)
            if (extractResult.isSuccess) {
                Log.d("ConfigManager", "成功解压资源文件到默认配置目录")
            } else {
                Log.w("ConfigManager", "解压资源文件失败: ${extractResult.exceptionOrNull()?.message}")
            }
            
            // 打印配置目录内容
            printConfigDirectoryContents(configFolder, "Default")
            
            Result.success(configEntity)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建默认配置文件内容
     */
    private fun createDefaultConfigContent(): String {
        return """
# Clash 默认配置文件
port: 7890
socks-port: 7891
allow-lan: false
mode: rule
log-level: info
external-controller: 127.0.0.1:9090

dns:
  enable: true
  listen: 0.0.0.0:53
  default-nameserver:
    - 223.5.5.5
    - 8.8.8.8
  nameserver:
    - https://doh.pub/dns-query
    - https://dns.alidns.com/dns-query

proxies:
  - name: "DIRECT"
    type: direct

proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - DIRECT

rules:
  - DOMAIN-SUFFIX,local,DIRECT
  - IP-CIDR,127.0.0.0/8,DIRECT
  - IP-CIDR,172.16.0.0/12,DIRECT
  - IP-CIDR,192.168.0.0/16,DIRECT
  - IP-CIDR,10.0.0.0/8,DIRECT
  - MATCH,PROXY
        """.trimIndent()
    }
    
    /**
     * 从本地文件导入配置
     */
    suspend fun importConfigFromLocal(
        uri: Uri,
        configName: String? = null
    ): Result<ConfigEntity> = withContext(Dispatchers.IO) {
        try {
            // 获取文件名
            val fileName = getFileNameFromUri(uri) ?: "config.yaml"
            val finalConfigName = configName ?: getConfigNameFromFileName(fileName)
            
            // 生成唯一的配置ID和文件夹名
            val configId = UUID.randomUUID().toString()
            val folderName = "config_${configId}"
            val configFolder = File(configsRootDir, folderName)
            
            // 创建配置专用文件夹
            if (!configFolder.exists()) {
                configFolder.mkdirs()
            }
            
            // 复制配置文件到专用文件夹，保持原始文件名
            val configFile = File(configFolder, fileName)
            copyUriToFile(uri, configFile)
            
            // 验证配置文件是否有效
            if (!isValidClashConfig(configFile)) {
                // 如果配置无效，清理文件夹
                configFolder.deleteRecursively()
                return@withContext Result.failure(Exception("无效的Clash配置文件"))
            }
            
            val currentTime = System.currentTimeMillis()
            
            // 创建配置实体
            val configEntity = ConfigEntity(
                id = configId,
                name = finalConfigName,
                folderPath = configFolder.absolutePath,
                configFilePath = configFile.absolutePath,
                createdAt = currentTime,
                updatedAt = currentTime,
                isActive = false,
                description = "从本地文件导入"
            )
            
            // 保存到数据库
            configDao.insertConfig(configEntity)
            
            // 解压 resources.zip 到配置目录
            val extractResult = extractResourcesZipToConfigDir(configFolder)
            if (extractResult.isSuccess) {
                Log.d("ConfigManager", "成功解压资源文件到配置目录")
            } else {
                Log.w("ConfigManager", "解压资源文件失败: ${extractResult.exceptionOrNull()?.message}")
            }
            
            // 打印配置目录内容
            printConfigDirectoryContents(configFolder, finalConfigName)
            
            // 自动激活新导入的配置
            configDao.setActiveConfig(configId)
            
            Result.success(configEntity)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除配置（包括文件夹和数据库记录）
     * 注意：不允许删除所有配置，至少保留一个
     */
    suspend fun deleteConfig(configId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 检查配置总数，如果只有一个配置则不允许删除
            val allConfigs = configDao.getAllConfigs().first()
            
            if (allConfigs.size <= 1) {
                return@withContext Result.failure(Exception("不能删除所有配置，至少需要保留一个配置"))
            }
            
            // 从数据库获取配置信息
            val config = configDao.getConfigById(configId)
                ?: return@withContext Result.failure(Exception("配置不存在"))
            
            // 如果要删除的是激活配置，需要先激活其他配置
            if (config.isActive) {
                val otherConfig = allConfigs.find { it.id != configId }
                otherConfig?.let {
                    configDao.setActiveConfig(it.id)
                }
            }
            
            // 删除配置文件夹
            val configFolder = File(config.folderPath)
            if (configFolder.exists()) {
                configFolder.deleteRecursively()
            }
            
            // 从数据库删除记录
            configDao.deleteConfigById(configId)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 激活配置
     */
    suspend fun activateConfig(configId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = configDao.getConfigById(configId)
                ?: return@withContext Result.failure(Exception("配置不存在"))
            
            // 设置为激活状态
            configDao.setActiveConfig(configId)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取当前激活的配置
     */
    suspend fun getActiveConfig(): ConfigEntity? = withContext(Dispatchers.IO) {
        configDao.getActiveConfig()
    }
    
    /**
     * 获取所有配置
     */
    fun getAllConfigs() = configDao.getAllConfigs()
    
    /**
     * 复制URI内容到文件
     */
    private fun copyUriToFile(uri: Uri, targetFile: File) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    
    /**
     * 验证是否为有效的Clash配置文件
     */
    private fun isValidClashConfig(file: File): Boolean {
        return try {
            val content = file.readText()
            isValidClashConfig(content)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 验证是否为有效的Clash配置内容
     */
    private fun isValidClashConfig(content: String): Boolean {
        return try {
            // 基本验证：检查是否包含Clash配置的关键字段
            content.contains("proxies") || 
            content.contains("proxy-groups") || 
            content.contains("rules") ||
            content.contains("port") ||
            content.contains("socks-port")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取配置文件夹的绝对路径（供外部使用）
     */
    fun getConfigFolderPath(configId: String): String? {
        return try {
            val folderName = "config_${configId}"
            File(configsRootDir, folderName).absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 重命名配置
     */
    suspend fun renameConfig(configId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 检查新名称是否已存在
            val existingConfig = configDao.getConfigByName(newName)
            if (existingConfig != null && existingConfig.id != configId) {
                return@withContext Result.failure(Exception("配置名称已存在"))
            }
            
            // 获取要重命名的配置
            val config = configDao.getConfigById(configId)
                ?: return@withContext Result.failure(Exception("配置不存在"))
            
            // 更新配置名称
            val updatedConfig = config.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )
            
            configDao.updateConfig(updatedConfig)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 读取配置文件内容
     */
    suspend fun readConfigContent(configId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = configDao.getConfigById(configId)
                ?: return@withContext Result.failure(Exception("配置不存在"))
            
            val configFile = File(config.configFilePath)
            if (!configFile.exists()) {
                return@withContext Result.failure(Exception("配置文件不存在"))
            }
            
            val content = configFile.readText()
            Result.success(content)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 保存配置文件内容
     */
    suspend fun saveConfigContent(configId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = configDao.getConfigById(configId)
                ?: return@withContext Result.failure(Exception("配置不存在"))
            
            val configFile = File(config.configFilePath)
            if (!configFile.exists()) {
                return@withContext Result.failure(Exception("配置文件不存在"))
            }
            
            // 验证YAML内容的基本格式
            if (!isValidClashConfig(content)) {
                return@withContext Result.failure(Exception("配置格式无效，请检查YAML语法和必要字段"))
            }
            
            // 保存内容
            configFile.writeText(content)
            
            // 更新配置的修改时间
            val updatedConfig = config.copy(updatedAt = System.currentTimeMillis())
            configDao.updateConfig(updatedConfig)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取配置根目录路径
     */
    fun getConfigsRootPath(): String = configsRootDir.absolutePath
    
    /**
     * 创建当前激活配置的副本
     */
    suspend fun createConfigCopy(): Result<ConfigEntity> = withContext(Dispatchers.IO) {
        try {
            // 获取当前激活的配置
            val activeConfig = configDao.getActiveConfig()
                ?: return@withContext Result.failure(Exception("没有激活的配置"))
            
            // 生成新的配置ID和文件夹名
            val newConfigId = UUID.randomUUID().toString()
            val newFolderName = "config_${newConfigId}"
            val newConfigFolder = File(configsRootDir, newFolderName)
            
            // 创建新的配置文件夹
            if (!newConfigFolder.exists()) {
                newConfigFolder.mkdirs()
            }
            
            // 复制原配置文件到新文件夹
            val originalConfigFile = File(activeConfig.configFilePath)
            val newConfigFile = File(newConfigFolder, "config.yaml")
            
            if (originalConfigFile.exists()) {
                originalConfigFile.copyTo(newConfigFile, overwrite = true)
            } else {
                return@withContext Result.failure(Exception("原配置文件不存在"))
            }
            
            // 生成副本名称，确保唯一性
            val baseName = "${activeConfig.name}_copy"
            var copyName = baseName
            var counter = 1
            
            // 检查名称是否已存在，如果存在则添加数字后缀
            while (configDao.getConfigByName(copyName) != null) {
                copyName = "${baseName}_${counter}"
                counter++
            }
            
            val currentTime = System.currentTimeMillis()
            
            // 创建新的配置实体
            val newConfigEntity = ConfigEntity(
                id = newConfigId,
                name = copyName,
                folderPath = newConfigFolder.absolutePath,
                configFilePath = newConfigFile.absolutePath,
                createdAt = currentTime,
                updatedAt = currentTime,
                isActive = false, // 副本默认不激活
                description = "从 ${activeConfig.name} 创建的副本"
            )
            
            // 保存到数据库
            configDao.insertConfig(newConfigEntity)
            
            // 解压 resources.zip 到新的配置目录
            val extractResult = extractResourcesZipToConfigDir(newConfigFolder)
            if (extractResult.isSuccess) {
                Log.d("ConfigManager", "成功解压资源文件到副本配置目录")
            } else {
                Log.w("ConfigManager", "解压资源文件失败: ${extractResult.exceptionOrNull()?.message}")
            }
            
            // 打印配置目录内容
            printConfigDirectoryContents(newConfigFolder, copyName)
            
            Result.success(newConfigEntity)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从URI获取文件名
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从文件名生成配置名称
     */
    private fun getConfigNameFromFileName(fileName: String): String {
        // 移除文件扩展名
        val nameWithoutExtension = fileName.substringBeforeLast(".")
        // 如果文件名为空或只有扩展名，使用默认名称
        return if (nameWithoutExtension.isBlank()) {
            "导入配置_${System.currentTimeMillis()}"
        } else {
            nameWithoutExtension
        }
    }
    
    /**
     * 解压assets中的resources.zip到指定配置目录
     */
    private suspend fun extractResourcesZipToConfigDir(configFolder: File): Result<List<String>> = withContext(Dispatchers.IO) {
        val TAG = "ConfigManager"
        try {
            Log.d(TAG, "开始解压 resources.zip 到配置目录: ${configFolder.absolutePath}")
            
            val extractedFiles = mutableListOf<String>()
            
            // 从assets中读取resources.zip
            context.assets.open("resources.zip").use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    
                    while (entry != null) {
                        val entryName = entry.name
                        val targetFile = File(configFolder, entryName)
                        
                        if (entry.isDirectory) {
                            // 创建目录
                            targetFile.mkdirs()
                            Log.d(TAG, "创建目录: $entryName")
                        } else {
                            // 确保父目录存在
                            targetFile.parentFile?.mkdirs()
                            
                            // 解压文件
                            FileOutputStream(targetFile).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }
                            
                            extractedFiles.add(entryName)
                            Log.d(TAG, "解压文件: $entryName (大小: ${targetFile.length()} 字节)")
                        }
                        
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }
            
            Log.d(TAG, "解压完成！共解压 ${extractedFiles.size} 个文件")
            
            // 清理 macOS 系统生成的元数据文件和文件夹
            cleanupMacOSMetadata(configFolder)
            
            Result.success(extractedFiles)
            
        } catch (e: Exception) {
            Log.e(TAG, "解压 resources.zip 失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 打印配置目录中的所有文件
     */
    private fun printConfigDirectoryContents(configFolder: File, configName: String) {
        val TAG = "ConfigManager"
        Log.d(TAG, "=== 配置 '$configName' 目录内容 ===")
        Log.d(TAG, "目录路径: ${configFolder.absolutePath}")
        
        try {
            printDirectoryRecursive(configFolder, "", TAG)
        } catch (e: Exception) {
            Log.e(TAG, "打印目录内容失败", e)
        }
        
        Log.d(TAG, "=== 目录内容打印完成 ===")
    }
    
    /**
     * 递归打印目录内容
     */
    private fun printDirectoryRecursive(dir: File, indent: String, tag: String) {
        if (!dir.exists() || !dir.isDirectory) return
        
        val files = dir.listFiles() ?: return
        
        files.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { file ->
            when {
                file.isDirectory -> {
                    Log.d(tag, "$indent📁 ${file.name}/")
                    printDirectoryRecursive(file, "$indent  ", tag)
                }
                else -> {
                    val size = formatFileSize(file.length())
                    Log.d(tag, "$indent📄 ${file.name} ($size)")
                }
            }
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "${sizeInBytes}B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024}KB"
            sizeInBytes < 1024 * 1024 * 1024 -> "${sizeInBytes / (1024 * 1024)}MB"
            else -> "${sizeInBytes / (1024 * 1024 * 1024)}GB"
        }
    }
    
    /**
     * 清理 macOS 系统生成的元数据文件和文件夹
     */
    private fun cleanupMacOSMetadata(configFolder: File) {
        val TAG = "ConfigManager"
        try {
            // 删除 __MACOSX 文件夹
            val macOSFolder = File(configFolder, "__MACOSX")
            if (macOSFolder.exists()) {
                val deleted = macOSFolder.deleteRecursively()
                if (deleted) {
                    Log.d(TAG, "已删除 __MACOSX 元数据文件夹")
                } else {
                    Log.w(TAG, "删除 __MACOSX 文件夹失败")
                }
            }
            
            // 递归删除以 ._ 开头的隐藏文件（macOS 元数据文件）
            cleanupMacOSMetadataRecursive(configFolder, TAG)
            
        } catch (e: Exception) {
            Log.e(TAG, "清理 macOS 元数据文件失败", e)
        }
    }
    
    /**
     * 递归清理 macOS 元数据文件
     */
    private fun cleanupMacOSMetadataRecursive(dir: File, tag: String) {
        if (!dir.exists() || !dir.isDirectory) return
        
        val files = dir.listFiles() ?: return
        var deletedCount = 0
        
        files.forEach { file ->
            when {
                file.isDirectory -> {
                    // 递归处理子目录
                    cleanupMacOSMetadataRecursive(file, tag)
                }
                file.name.startsWith("._") -> {
                    // 删除以 ._ 开头的 macOS 元数据文件
                    if (file.delete()) {
                        deletedCount++
                        Log.d(tag, "已删除 macOS 元数据文件: ${file.name}")
                    } else {
                        Log.w(tag, "删除 macOS 元数据文件失败: ${file.name}")
                    }
                }
            }
        }
        
        if (deletedCount > 0) {
            Log.d(tag, "在目录 ${dir.name} 中删除了 $deletedCount 个 macOS 元数据文件")
        }
    }
} 