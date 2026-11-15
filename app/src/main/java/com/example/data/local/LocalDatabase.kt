package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_levels")
data class CachedLevelEntity(
    @PrimaryKey val levelNumber: Int,
    val worldId: Int,
    val stars: Int,
    val highScore: Long,
    val bestHeight: Int,
    val completed: Boolean
)

@Entity(tableName = "mission_progress")
data class MissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val rewardCoins: Int,
    val rewardGems: Int,
    val claimed: Boolean
)

@Entity(tableName = "local_mode_bests")
data class ModeBestEntity(
    @PrimaryKey val modeKey: String,
    val bestHeight: Int,
    val bestScore: Long
)

@Dao
interface GameCacheDao {
    @Query("SELECT * FROM cached_levels ORDER BY levelNumber ASC")
    fun observeAllLevels(): Flow<List<CachedLevelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevels(levels: List<CachedLevelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevel(level: CachedLevelEntity)

    @Query("DELETE FROM cached_levels")
    suspend fun clearLevels()

    @Query("SELECT * FROM mission_progress")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT COUNT(*) FROM mission_progress")
    suspend fun getMissionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMissions(missions: List<MissionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(mission: MissionEntity)

    @Query("SELECT * FROM local_mode_bests")
    fun observeModeBests(): Flow<List<ModeBestEntity>>

    @Query("SELECT * FROM local_mode_bests WHERE modeKey = :modeKey LIMIT 1")
    suspend fun getModeBest(modeKey: String): ModeBestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModeBest(modeBest: ModeBestEntity)

    @Query("DELETE FROM local_mode_bests")
    suspend fun clearModeBests()
}

@Database(
    entities = [CachedLevelEntity::class, MissionEntity::class, ModeBestEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalGameDatabase : RoomDatabase() {
    abstract fun gameCacheDao(): GameCacheDao

    companion object {
        @Volatile
        private var INSTANCE: LocalGameDatabase? = null

        fun getInstance(context: Context): LocalGameDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalGameDatabase::class.java,
                    "tumble_tower_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
