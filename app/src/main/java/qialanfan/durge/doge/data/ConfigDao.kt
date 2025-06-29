package qialanfan.durge.doge.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    
    @Query("SELECT * FROM configs ORDER BY updatedAt DESC")
    fun getAllConfigs(): Flow<List<ConfigEntity>>
    
    @Query("SELECT * FROM configs WHERE id = :id")
    suspend fun getConfigById(id: String): ConfigEntity?
    
    @Query("SELECT * FROM configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfig(): ConfigEntity?
    
    @Query("SELECT * FROM configs WHERE name = :name")
    suspend fun getConfigByName(name: String): ConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)
    
    @Update
    suspend fun updateConfig(config: ConfigEntity)
    
    @Delete
    suspend fun deleteConfig(config: ConfigEntity)
    
    @Query("DELETE FROM configs WHERE id = :id")
    suspend fun deleteConfigById(id: String)
    
    @Query("UPDATE configs SET isActive = 0")
    suspend fun deactivateAllConfigs()
    
    @Query("UPDATE configs SET isActive = 1 WHERE id = :id")
    suspend fun activateConfig(id: String)
    
    @Transaction
    suspend fun setActiveConfig(id: String) {
        deactivateAllConfigs()
        activateConfig(id)
    }
} 