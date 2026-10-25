package com.daiarn.counter.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.daiarn.counter.data.CounterDatabase
import com.daiarn.counter.data.Visit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

class VisitSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = AccountAccess.lock.withLock {
        val dao = CounterDatabase.get(applicationContext).visits()
        if (dao.pendingVisits().isEmpty()) return@withLock Result.success()
        val status = applicationContext.getSharedPreferences("sync_status", Context.MODE_PRIVATE)
        try {
            val cloud = FirebaseVisitCloud(applicationContext)
            cloud.authenticate()
            VisitSync(object : PendingVisitStore {
                override suspend fun pending(): List<Visit> = dao.pendingVisits()
                override suspend fun acknowledge(id: String, revision: Long) {
                    dao.acknowledge(id, revision)
                }
            }, cloud).drain()
            status.edit().remove("issue").apply()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val issue = when (error) {
                is SyncIdentityChanged -> "identity"
                is CloudRevisionConflict -> "conflict"
                else -> "unavailable"
            }
            status.edit().putString("issue", issue).apply()
            // WorkManager retries with backoff; every local mutation also schedules work.
            Result.retry()
        }
    }

}

object SyncScheduler {
    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "visit-sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<VisitSyncWorker>().setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        )
    }

    fun start(context: Context) {
        schedule(context)
        // Covers process death between a database write and scheduling its upload.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "visit-sync-safety", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<VisitSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(connected).build()
        )
    }
}
