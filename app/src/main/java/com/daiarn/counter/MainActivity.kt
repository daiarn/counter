package com.daiarn.counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daiarn.counter.ui.CounterApp
import com.daiarn.counter.ui.theme.CounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CounterTheme {
                val model: CounterViewModel = viewModel()
                val state by model.state.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                var signingIn by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(model) {
                    model.notifications.collect { snackbar.showSnackbar(it) }
                }
                CounterApp(state, snackbar, model::log, model::load, model::save, model::delete,
                    signingIn = signingIn, onRestore = model::restoreFromCloud,
                    onSignIn = {
                        if (!signingIn && !state.accountBusy) scope.launch {
                            signingIn = true
                            try {
                                val resource = resources.getIdentifier("default_web_client_id", "string", packageName)
                                check(resource != 0) { "Google sign-in needs Firebase configuration. Contact the project administrator." }
                                val option = GetSignInWithGoogleOption.Builder(getString(resource)).build()
                                val result = CredentialManager.create(this@MainActivity).getCredential(
                                    this@MainActivity, GetCredentialRequest.Builder().addCredentialOption(option).build())
                                val credential = result.credential as? CustomCredential
                                check(credential?.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    "Google did not return a sign-in credential. Please try again."
                                }
                                model.signInWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                            } catch (_: GetCredentialCancellationException) {
                                model.accountMessage("Sign-in cancelled. Your visits are unchanged.")
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: IllegalStateException) {
                                model.accountMessage(error.message ?: "Google sign-in is not configured.")
                            } catch (_: Exception) {
                                model.accountMessage("Google sign-in could not finish. Check your connection and Google Play services, then try again.")
                            } finally {
                                signingIn = false
                            }
                        }
                    })
            }
        }
    }
}
