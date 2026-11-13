package com.narcy.notesai.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.narcy.notesai.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/** Datos de la cuenta de Google conectada. */
data class CloudUser(
    val uid: String,
    val email: String?,
    val name: String?,
    val photoUrl: String?
)

sealed interface SignInResult {
    data object Success : SignInResult
    data object Cancelled : SignInResult
    data class Error(val message: String) : SignInResult
}

private fun FirebaseUser.toCloudUser() = CloudUser(
    uid = uid,
    email = email,
    name = displayName,
    photoUrl = photoUrl?.toString()
)

/**
 * Inicio y cierre de sesión con una cuenta personal de Google.
 * Usa Credential Manager para elegir la cuenta y Firebase Authentication para
 * validar el token. El ID de cliente web sale de google-services.json
 * (recurso `default_web_client_id`, que genera el plugin de Google Services).
 */
class CloudAuth(context: Context) {
    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(appContext)

    private val _user = MutableStateFlow(auth.currentUser?.toCloudUser())
    val user: StateFlow<CloudUser?> = _user

    init {
        auth.addAuthStateListener { _user.value = it.currentUser?.toCloudUser() }
    }

    /** [activityContext] debe ser una Activity: Credential Manager la usa para mostrar el selector de cuentas. */
    suspend fun signIn(activityContext: Context): SignInResult {
        return try {
            val option = GetSignInWithGoogleOption.Builder(
                appContext.getString(R.string.default_web_client_id)
            ).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentialManager.getCredential(activityContext, request).credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                SignInResult.Success
            } else {
                SignInResult.Error("Google devolvió un tipo de credencial inesperado.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Error("No se encontró ninguna cuenta de Google en este dispositivo.")
        } catch (e: GetCredentialException) {
            SignInResult.Error(
                "No se pudo iniciar sesión con Google. Comprueba que la huella SHA-1 de la app " +
                    "esté registrada en Firebase. (${e.message ?: e.type})"
            )
        } catch (e: GoogleIdTokenParsingException) {
            SignInResult.Error("No se pudo leer la respuesta de Google.")
        } catch (e: Exception) {
            SignInResult.Error("No se pudo completar el inicio de sesión: ${e.message ?: "error desconocido"}")
        }
    }

    /** Cierra la sesión de Firebase y olvida la cuenta elegida, para que el próximo inicio pregunte cuál usar. */
    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // La sesión de Firebase ya está cerrada; no pasa nada si esto falla.
        }
    }
}
