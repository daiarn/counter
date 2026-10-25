package com.daiarn.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daiarn.counter.data.CounterDatabase
import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitRepository
import com.daiarn.counter.data.VisitType
import com.daiarn.counter.sync.SyncScheduler
import com.daiarn.counter.sync.AccountAccess
import com.daiarn.counter.sync.AccountRecovery
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.sync.withLock
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
    val syncIssue: Boolean = false,
    val accountEmail: String? = null,
    val accountUid: String? = null,
    val accountBusy: Boolean = false,
    val accountMessage: String? = null
)

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VisitRepository(CounterDatabase.get(application).visits())
    private val recovery = AccountRecovery(application, CounterDatabase.get(application).visits())
    private val auth = FirebaseAuth.getInstance()
    private val authListener = FirebaseAuth.AuthStateListener { refreshAccount() }
    private val mutableState = MutableStateFlow(CounterState())
    val state = mutableState.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val notifications = messages.receiveAsFlow()
    private val syncStatus = application.getSharedPreferences("sync_status", Application.MODE_PRIVATE)
    private val syncListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        mutableState.value = mutableState.value.copy(syncIssue = syncStatus.contains("issue"))
    }

    init {
        auth.addAuthStateListener(authListener)
        syncStatus.registerOnSharedPreferenceChangeListener(syncListener)
        SyncScheduler.start(application)
        load()
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
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
                mutableState.value.copy(visits = visits, pendingCount = pending, loading = false, loadError = false,
                    syncIssue = syncStatus.contains("issue"))
            }.catch {
                mutableState.value = mutableState.value.copy(loading = false, loadError = true)
            }.collect { mutableState.value = it }
        }
    }

    fun log(type: VisitType) {
        viewModelScope.launch {
            try {
                AccountAccess.lock.withLock { recovery.reconcileIdentity(); repository.log(type) }
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
                AccountAccess.lock.withLock { recovery.reconcileIdentity(); repository.save(id, type, instant, note) }
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
                AccountAccess.lock.withLock { recovery.reconcileIdentity(); repository.delete(visit.id) }
                scheduleSync()
                completed(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                completed("Could not delete this visit. Please try again.")
            }
        }
    }

    private fun refreshAccount() {
        val user = auth.currentUser
        val google = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        mutableState.value = mutableState.value.copy(
            accountEmail = if (google) user?.email ?: "Google account" else null,
            accountUid = user?.uid
        )
    }

    fun accountMessage(message: String) {
        mutableState.value = mutableState.value.copy(accountMessage = message)
    }

    fun signInWithGoogle(idToken: String) = accountOperation {
        recovery.signInWithGoogle(idToken)
        refreshAccount()
        val count = recovery.restore()
        "Google account connected. Restored $count additional visits."
    }

    fun restoreFromCloud() = accountOperation {
        val count = recovery.restore()
        "Restored $count additional visits. Existing unsynced changes were kept."
    }

    private fun accountOperation(action: suspend () -> String) {
        if (mutableState.value.accountBusy) return
        mutableState.value = mutableState.value.copy(accountBusy = true, accountMessage = null)
        viewModelScope.launch {
            try {
                val message = AccountAccess.lock.withLock { action() }
                mutableState.value = mutableState.value.copy(accountMessage = message)
                SyncScheduler.schedule(getApplication())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalStateException) {
                accountMessage(error.message ?: "Could not complete recovery. Your local visits are unchanged.")
            } catch (_: Exception) {
                accountMessage("Could not complete sign-in or restore. Check your connection and try again. If Google is connected, use Restore from cloud. Your saved visits are safe.")
            } finally {
                refreshAccount()
                mutableState.value = mutableState.value.copy(accountBusy = false)
            }
        }
    }
}
