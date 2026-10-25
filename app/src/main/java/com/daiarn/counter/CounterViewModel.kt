package com.daiarn.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daiarn.counter.data.CounterDatabase
import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitRepository
import com.daiarn.counter.data.VisitType
import com.daiarn.counter.sync.SyncScheduler
import android.content.SharedPreferences
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class CounterState(
    val visits: List<Visit> = emptyList(),
    val pendingCount: Int = 0,
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val syncIssue: Boolean = false
)

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VisitRepository(CounterDatabase.get(application).visits())
    private val mutableState = MutableStateFlow(CounterState())
    val state = mutableState.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val notifications = messages.receiveAsFlow()
    private val syncStatus = application.getSharedPreferences("sync_status", Application.MODE_PRIVATE)
    private val syncListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        mutableState.value = mutableState.value.copy(syncIssue = syncStatus.contains("issue"))
    }

    init {
        syncStatus.registerOnSharedPreferenceChangeListener(syncListener)
        SyncScheduler.start(application)
        load()
    }

    override fun onCleared() {
        syncStatus.unregisterOnSharedPreferenceChangeListener(syncListener)
        super.onCleared()
    }

    private fun scheduleSync() {
        // Scheduling failure must not report an already-persisted local save as failed.
        runCatching { SyncScheduler.schedule(getApplication()) }
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, loadError = false)
            combine(repository.visits, repository.pendingCount) { visits, pending ->
                CounterState(visits = visits, pendingCount = pending, loading = false,
                    syncIssue = syncStatus.contains("issue"))
            }.catch {
                mutableState.value = mutableState.value.copy(loading = false, loadError = true)
            }.collect { mutableState.value = it }
        }
    }

    fun log(type: VisitType) {
        viewModelScope.launch {
            try {
                repository.log(type)
                scheduleSync()
                messages.send("${type.label} saved")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                messages.send("Could not save this visit. Please try again.")
            }
        }
    }

    fun save(id: String?, type: VisitType, instant: Instant, note: String, completed: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.save(id, type, instant, note)
                scheduleSync()
                completed(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                completed(error.message ?: "Check the visit details.")
            } catch (_: Exception) {
                completed("Could not save this visit. Please try again.")
            }
        }
    }

    fun delete(visit: Visit, completed: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.delete(visit.id)
                scheduleSync()
                completed(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                completed("Could not delete this visit. Please try again.")
            }
        }
    }
}
