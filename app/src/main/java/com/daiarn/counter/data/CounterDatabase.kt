package com.daiarn.counter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
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
    @Query("SELECT COUNT(*) FROM visits")
    suspend fun storedCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRestored(visit: Visit): Long

    @Query("UPDATE visits SET type = :type, occurredAt = :occurredAt, note = :note, " +
        "deleted = :deleted, revision = :revision WHERE id = :id AND pendingSync = 0 AND revision <= :revision")
    suspend fun updateRestored(id: String, type: VisitType, occurredAt: Long, note: String, deleted: Boolean, revision: Long): Int

    @Query("UPDATE visits SET revision = :cloudRevision + 1 WHERE id = :id AND pendingSync = 1 AND revision <= :cloudRevision")
    suspend fun rebasePending(id: String, cloudRevision: Long): Int

    @Transaction
    suspend fun restore(visits: List<Visit>): Int {
        var inserted = 0
        visits.forEach { visit ->
            require(!visit.pendingSync && visit.revision in 1 until Long.MAX_VALUE) { "Invalid cloud record" }
            if (insertRestored(visit) != -1L) {
                if (!visit.deleted) inserted++
            } else {
                updateRestored(visit.id, visit.type, visit.occurredAt, visit.note, visit.deleted, visit.revision)
                rebasePending(visit.id, visit.revision)
            }
        }
        return inserted
    }

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
