package qialanfan.durge.doge.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import qialanfan.durge.doge.data.ConfigDatabase
import qialanfan.durge.doge.data.ConfigEntity
import qialanfan.durge.doge.manager.ConfigManager

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = ConfigDatabase.getDatabase(application)
    private val configManager = ConfigManager(application, database.configDao())
    
    // UI状态
    private val _configs = MutableStateFlow<List<ConfigEntity>>(emptyList())
    val configs: StateFlow<List<ConfigEntity>> = _configs.asStateFlow()
    
    private val _activeConfig = MutableStateFlow<ConfigEntity?>(null)
    val activeConfig: StateFlow<ConfigEntity?> = _activeConfig.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        initializeDefaultConfig()
        loadConfigs()
        loadActiveConfig()
    }
    
    /**
     * 初始化默认配置
     */
    private fun initializeDefaultConfig() {
        viewModelScope.launch {
            try {
                configManager.createDefaultConfig()
            } catch (e: Exception) {
                _errorMessage.value = "初始化默认配置失败: ${e.message}"
            }
        }
    }
    
    /**
     * 加载所有配置
     */
    private fun loadConfigs() {
        viewModelScope.launch {
            configManager.getAllConfigs().collect { configList ->
                _configs.value = configList
            }
        }
    }
    
    /**
     * 加载当前激活的配置
     */
    private fun loadActiveConfig() {
        viewModelScope.launch {
            val activeConfig = configManager.getActiveConfig()
            _activeConfig.value = activeConfig
        }
    }
    
    /**
     * 从本地文件导入配置
     */
    fun importConfigFromLocal(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.importConfigFromLocal(uri)
                
                if (result.isSuccess) {
                    // 导入成功，刷新配置列表
                    loadConfigs()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "导入失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "导入过程中发生错误"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 删除配置
     */
    fun deleteConfig(configId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.deleteConfig(configId)
                
                if (result.isSuccess) {
                    // 删除成功，刷新配置列表
                    loadConfigs()
                    loadActiveConfig() // 如果删除的是激活配置，需要更新激活状态
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "删除失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "删除过程中发生错误"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 激活配置
     */
    fun activateConfig(configId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.activateConfig(configId)
                
                if (result.isSuccess) {
                    // 激活成功，刷新激活状态
                    loadActiveConfig()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "激活失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "激活过程中发生错误"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 获取配置文件夹路径
     */
    fun getConfigFolderPath(configId: String): String? {
        return configManager.getConfigFolderPath(configId)
    }
    
    /**
     * 重命名配置
     */
    fun renameConfig(configId: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.renameConfig(configId, newName)
                
                if (result.isSuccess) {
                    // 重命名成功，刷新配置列表
                    loadConfigs()
                    loadActiveConfig()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "重命名失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "重命名过程中发生错误"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 读取配置文件内容
     */
    fun readConfigContent(configId: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.readConfigContent(configId)
                onResult(result)
                
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "读取配置失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "读取配置过程中发生错误"
                onResult(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 保存配置文件内容
     */
    fun saveConfigContent(configId: String, content: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.saveConfigContent(configId, content)
                onResult(result)
                
                if (result.isSuccess) {
                    // 保存成功，刷新配置列表
                    loadConfigs()
                    loadActiveConfig()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "保存配置失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "保存配置过程中发生错误"
                onResult(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 获取配置根目录路径
     */
    fun getConfigsRootPath(): String {
        return configManager.getConfigsRootPath()
    }
    
    /**
     * 创建当前激活配置的副本
     */
    fun createConfigCopy() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = configManager.createConfigCopy()
                
                if (result.isSuccess) {
                    // 创建副本成功，刷新配置列表
                    loadConfigs()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "创建副本失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "创建副本过程中发生错误"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 