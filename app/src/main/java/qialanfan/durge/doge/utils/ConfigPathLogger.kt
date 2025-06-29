package qialanfan.durge.doge.utils

import android.content.Context
import android.util.Log
import java.io.File

object ConfigPathLogger {
    private const val TAG = "ConfigPathLogger"
    
    /**
     * 记录配置根目录路径
     */
    fun logConfigRootPath(context: Context) {
        val configsRootDir = File(context.filesDir, "clash_configs")
        val absolutePath = configsRootDir.absolutePath
        
        Log.i(TAG, "=== Clash配置管理系统路径信息 ===")
        Log.i(TAG, "配置根目录绝对路径: $absolutePath")
        Log.i(TAG, "应用内部存储路径: ${context.filesDir.absolutePath}")
        Log.i(TAG, "配置根目录是否存在: ${configsRootDir.exists()}")
        
        if (configsRootDir.exists()) {
            val subDirs = configsRootDir.listFiles()?.filter { it.isDirectory }
            Log.i(TAG, "现有配置文件夹数量: ${subDirs?.size ?: 0}")
            subDirs?.forEach { dir ->
                Log.i(TAG, "配置文件夹: ${dir.name} -> ${dir.absolutePath}")
                val configFile = File(dir, "config.yaml")
                Log.i(TAG, "  配置文件存在: ${configFile.exists()}")
                if (configFile.exists()) {
                    Log.i(TAG, "  配置文件大小: ${configFile.length()} bytes")
                }
            }
        }
        
        Log.i(TAG, "=== 路径信息记录完成 ===")
    }
    
    /**
     * 获取配置根目录路径（供外部使用）
     */
    fun getConfigRootPath(context: Context): String {
        return File(context.filesDir, "clash_configs").absolutePath
    }
} 