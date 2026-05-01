package pl.fitcoach.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.fitcoach.features.auth.data.db.UserCacheEntity
import pl.fitcoach.features.auth.data.db.UserCacheDao

@Database(
    entities = [
        UserCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FitCoachDatabase : RoomDatabase() {

    abstract fun userCacheDao(): UserCacheDao

    companion object {
        const val DATABASE_NAME = "fitcoach.db"
    }
}
