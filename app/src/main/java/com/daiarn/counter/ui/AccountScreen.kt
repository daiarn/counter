package com.daiarn.counter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daiarn.counter.CounterState

@Composable
fun AccountScreen(state: CounterState, signingIn: Boolean, onSignIn: () -> Unit, onRestore: () -> Unit) {
    val busy = state.accountBusy || signingIn
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Account & recovery", style = MaterialTheme.typography.headlineSmall)
        Text("Connect Google to keep access to your synced history after reinstalling or changing phones.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.accountEmail ?: "Anonymous account", style = MaterialTheme.typography.titleLarge)
                if (state.accountEmail == null) {
                    Text("Connecting a new Google account keeps this installation's visits and cloud identity.")
                    Button(onClick = onSignIn, enabled = !busy) { Text("Sign in with Google") }
                } else {
                    Text("Your synced visits can be restored using this Google account.")
                    Button(onClick = onRestore, enabled = !busy) { Text("Restore from cloud") }
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Connecting and restoring…")
                }
                state.accountMessage?.let { Text(it) }
            }
        }
        Text("Recover an earlier installation", style = MaterialTheme.typography.titleLarge)
        Text("If your old visits were saved anonymously, the project administrator must first copy them to your connected account. Then tap Restore from cloud.")
        Text("Restoring keeps unsynced local edits and deletions. It does not erase your existing visits.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.accountUid?.let {
            Text("Account ID for recovery", style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
