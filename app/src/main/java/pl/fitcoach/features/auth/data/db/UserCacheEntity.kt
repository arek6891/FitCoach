package pl.fitcoach.features.auth.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "user_cache")
data class UserCacheEntity(
    @PrimaryKey val id: String,
    val email: String,
    val role: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface UserCacheDao {
    @Query("SELECT * FROM user_cache LIMIT 1")
    suspend fun getCachedUser(): UserCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserCacheEntity)

    @Query("DELETE FROM user_cache")
    suspend fun clearCache()
}
