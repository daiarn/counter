package com.daiarn.counter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

class VisitConverters {
    @TypeConverter fun fromType(type: VisitType): String = type.name
    @TypeConverter fun toType(value: String): VisitType = VisitType.valueOf(value)
}

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE deleted = 0 ORDER BY occurredAt DESC, id DESC")
    fun observeVisits(): Flow<List<Visit>>

    @Query("SELECT COUNT(*) FROM visits WHERE pendingSync = 1")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(visit: Visit)

    @Query("UPDATE visits SET type = :type, occurredAt = :occurredAt, note = :note, " +
        "pendingSync = 1, revision = revision + 1 WHERE id = :id AND deleted = 0")
    suspend fun edit(id: String, type: VisitType, occurredAt: Long, note: String): Int

    @Query("UPDATE visits SET deleted = 1, pendingSync = 1, revision = revision + 1 " +
        "WHERE id = :id AND deleted = 0")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM visits WHERE pendingSync = 1 ORDER BY revision, id")
    suspend fun pendingVisits(): List<Visit>

    // A stale upload must not acknowledge a more recent local edit or deletion.
    @Query("UPDATE visits SET pendingSync = 0 WHERE id = :id AND revision = :revision")
    suspend fun acknowledge(id: String, revision: Long): Int
}

@Database(entities = [Visit::class], version = 1, exportSchema = true)
@TypeConverters(VisitConverters::class)
abstract class CounterDatabase : RoomDatabase() {
    abstract fun visits(): VisitDao

    companion object {
        @Volatile private var instance: CounterDatabase? = null

        fun get(context: Context): CounterDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, CounterDatabase::class.java, "counter.db"
            ).build().also { instance = it }
        }
    }
}
