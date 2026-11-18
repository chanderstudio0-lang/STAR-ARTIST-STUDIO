package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioOutline
import com.example.util.StudioContactDetails
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthScreen"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class AuthMode {
    SIGN_IN,
    REGISTER
}

object AuthValidator {
    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        return EMAIL_REGEX.matches(trimmed)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun mapFirebaseError(exception: Throwable): String {
        val errorCode = (exception as? FirebaseAuthException)?.errorCode.orEmpty()
        val msg = exception.message ?: ""
        return when {
            errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                    msg.contains("operation-not-allowed", ignoreCase = true) ||
                    msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Email/Password sign-in is not yet enabled in Firebase Console. Please enable Email/Password under Authentication -> Sign-in method, or use Sign in with Google."
            exception is FirebaseTooManyRequestsException ||
                    errorCode == "ERROR_TOO_MANY_REQUESTS" ||
                    msg.contains("too-many-requests", ignoreCase = true) ->
                "Too many sign-in attempts. Please wait a few minutes and try again."
            exception is FirebaseNetworkException ||
                    errorCode == "ERROR_NETWORK_REQUEST_FAILED" ||
                    msg.contains("network-request-failed", ignoreCase = true) ->
                "Network connection error. Please check your internet connection and try again."
            errorCode == "ERROR_USER_DISABLED" ||
                    msg.contains("user-disabled", ignoreCase = true) ->
                "This studio account has been disabled. Please contact studio support."
            exception is FirebaseAuthInvalidUserException ||
                    errorCode == "ERROR_USER_NOT_FOUND" ||
                    msg.contains("user-not-found", ignoreCase = true) ->
                "No studio account found with this email. Please check spelling or create an account."
            exception is FirebaseAuthWeakPasswordException ||
                    errorCode == "ERROR_WEAK_PASSWORD" ||
                    msg.contains("weak-password", ignoreCase = true) ->
                "Password must be at least 6 characters long."
            exception is FirebaseAuthUserCollisionException ||
                    errorCode == "ERROR_EMAIL_ALREADY_IN_USE" ||
                    msg.contains("email-already-in-use", ignoreCase = true) ->
                "An account with this email already exists. Please switch to Sign In."
            exception is FirebaseAuthInvalidCredentialsException ||
                    errorCode == "ERROR_WRONG_PASSWORD" ||
                    errorCode == "ERROR_INVALID_CREDENTIAL" ||
                    errorCode == "ERROR_INVALID_EMAIL" ||
                    msg.contains("wrong-password", ignoreCase = true) ||
                    msg.contains("invalid-credential", ignoreCase = true) ||
                    msg.contains("The email address is badly formatted", ignoreCase = true) ->
                "Invalid credentials. Please verify your email and password."
            msg.isNotBlank() -> msg
            else -> "Authentication failed. Please verify connection and try again."
        }
    }

    fun canAccessBooking(isEmailVerified: Boolean, isAnonymous: Boolean = false): Boolean {
        // Only verified accounts can access studio booking requests to protect shoot reservations
        return isEmailVerified && !isAnonymous
    }

    fun getEmailVerificationNotice(email: String): String {
        return "A verification link has been sent to $email. Please confirm your email address before accessing studio booking features."
    }

    fun resolveWebClientId(context: Context): String? {
        return try {
            context.getString(R.string.default_web_client_id).trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds an interactive Credential Manager request carrying exactly one
     * GetSignInWithGoogleOption so the bottom sheet stays responsive across API 34-36.
     */
    fun buildInteractiveGoogleSignInRequest(serverClientId: String): GetCredentialRequest {
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
    }

    /**
     * Builds a silent/auto-select Credential Manager request carrying strictly
     * one GetGoogleIdOption for returning authorized studio members.
     */
    fun buildAutoGoogleSignInRequest(serverClientId: String): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    fun mapGoogleCredentialException(exception: Throwable): String {
        val msg = exception.localizedMessage.orEmpty()
        return when (exception) {
            is NoCredentialException ->
                "No Google account found on this device. Please add a Google account in device settings or sign in with Email."
            is GetCredentialProviderConfigurationException ->
                "Google Credential provider is missing or outdated on this device. Update Google Play Services and try again."
            is GetCredentialUnsupportedException ->
                "Google Credential Manager is not supported on this device configuration."
            is GoogleIdTokenParsingException ->
                "Received an invalid Google ID token. Please try signing in again."
            is GetCredentialException -> {
                if (msg.contains("DEVELOPER_ERROR", ignoreCase = true) || msg.contains("10:")) {
                    "Google Sign-In configuration mismatch (SHA-1/OAuth). Please verify SHA-1 in Firebase Console."
                } else {
                    msg.ifBlank { "Google Sign-In failed. Please try again." }
                }
            }
            else -> mapFirebaseError(exception)
        }
    }
}

suspend fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit
) {
    val clientId = AuthValidator.resolveWebClientId(context) ?: return
    val request = AuthValidator.buildAutoGoogleSignInRequest(clientId)
    val activityContext = context.findActivity() ?: context
    try {
        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            Firebase.auth.signInWithCredential(authCredential).await()
            onAuthSuccess()
        }
    } catch (_: NoCredentialException) {
        // No saved Google account authorized yet; ignore silently
    } catch (_: GetCredentialCancellationException) {
        // User dismissed one-tap prompt; ignore silently
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "Auto Google Sign-In skipped: ${e.message}")
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = AuthValidator.resolveWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to launch Google Sign-In prompt from the current context.")
        return
    }

    val request = AuthValidator.buildInteractiveGoogleSignInRequest(clientId)

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected Google credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(AuthValidator.mapGoogleCredentialException(e))
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            withContext(NonCancellable) {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credential state", e)
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    isStudioLight: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onHoldAuthForVerification: (Boolean) -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManagerDelegate = remember(context) { lazy { CredentialManager.create(context) } }

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isEmailLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    val isBusy = isEmailLoading || isGoogleLoading
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showVerificationNotice by remember { mutableStateOf(false) }
    var registeredEmailNotice by remember { mutableStateOf("") }
    var resendStatusText by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetDialogError by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            // Let MainActivity complete its initial SurfaceSyncGroup draw transaction before invoking CredentialManager overlay
            androidx.compose.runtime.withFrameNanos { }
            delay(500L)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManagerDelegate.value,
                onAuthSuccess = {
                    onHoldAuthForVerification(false)
                    onAuthSuccess()
                }
            )
        }
    }

    val scrollState = rememberScrollState()

    val surfaceBackground = if (isStudioLight) Color(0xFFFFFFFF) else StudioCardSurface
    val primaryTextColor = if (isStudioLight) Color(0xFF1E1705) else Color(0xFFF3E5AB)
    val subtitleTextColor = if (isStudioLight) Color(0xFF4A4035) else Color(0xFFD0D0D0)
    val fieldTextColor = if (isStudioLight) Color(0xFF161202) else Color.White
    val cardBorderColor = if (isStudioLight) GoldPrimary.copy(alpha = 0.4f) else StudioOutline

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isStudioLight) Color(0xFFF5F3EC) else StudioBlack)
            .testTag("auth_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isStudioLight) listOf(
                            Color(0xFFEDE8D8),
                            Color(0xFFF5F3EC),
                            Color(0xFFE5E0D0)
                        ) else listOf(
                            Color(0xFF1E1705),
                            StudioBlack,
                            Color(0xFF0F0F10)
                        )
                    )
                )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                .testTag("auth_card"),
            colors = CardDefaults.cardColors(containerColor = surfaceBackground),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Top Theme Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("auth_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isStudioLight) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = if (isStudioLight)
                                "Switch to Black and Gold Design"
                            else
                                "Switch to High-Contrast Studio Light Mode",
                            tint = GoldPrimary
                        )
                    }
                }

                // Studio Icon Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GoldLight, GoldDark)
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isStudioLight) Color(0xFF26200A) else StudioBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Studio Star",
                            tint = GoldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = StudioContactDetails.STUDIO_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Talwara Khurd, Sirsa, Haryana",
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryTextColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Professional Modeling, Photography & 4K Cinema Video Production",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Mode Tabs (Sign In vs Create Account)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isStudioLight) Color(0xFFEBE6D6) else Color(0xFF202025))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            authMode = AuthMode.SIGN_IN
                            errorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("auth_tab_signin"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (authMode == AuthMode.SIGN_IN) GoldPrimary else Color.Transparent,
                            contentColor = if (authMode == AuthMode.SIGN_IN) Color(0xFF161202) else if (isStudioLight) Color(0xFF4A4035) else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign In",
                                fontWeight = if (authMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            authMode = AuthMode.REGISTER
                            errorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("auth_tab_register"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (authMode == AuthMode.REGISTER) GoldPrimary else Color.Transparent,
                            contentColor = if (authMode == AuthMode.REGISTER) Color(0xFF161202) else if (isStudioLight) Color(0xFF4A4035) else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Register",
                                fontWeight = if (authMode == AuthMode.REGISTER) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error Message banner
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("auth_error_message"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Success/Status Message banner (e.g., password reset sent)
                if (statusMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("auth_status_message"),
                        colors = CardDefaults.cardColors(
                            containerColor = GoldPrimary.copy(alpha = 0.18f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            color = GoldPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Email Input Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (errorMessage != null) errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    label = { Text("Studio Member Email") },
                    placeholder = { Text("client@starartist.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon",
                            tint = GoldPrimary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = if (isStudioLight) Color(0xFFC0A040) else StudioOutline,
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = if (isStudioLight) Color(0xFF6B5B2E) else GoldLight.copy(alpha = 0.7f),
                        focusedLeadingIconColor = GoldPrimary,
                        unfocusedLeadingIconColor = GoldPrimary.copy(alpha = 0.8f),
                        focusedTextColor = fieldTextColor,
                        unfocusedTextColor = fieldTextColor,
                        cursorColor = GoldPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isBusy
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (errorMessage != null) errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    label = { Text("Password (min 6 chars)") },
                    placeholder = { Text("Enter secure password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password Icon",
                            tint = GoldPrimary
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.testTag("auth_password_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = GoldPrimary
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = if (isStudioLight) Color(0xFFC0A040) else StudioOutline,
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = if (isStudioLight) Color(0xFF6B5B2E) else GoldLight.copy(alpha = 0.7f),
                        focusedLeadingIconColor = GoldPrimary,
                        unfocusedLeadingIconColor = GoldPrimary.copy(alpha = 0.8f),
                        focusedTextColor = fieldTextColor,
                        unfocusedTextColor = fieldTextColor,
                        cursorColor = GoldPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isBusy
                )

                if (authMode == AuthMode.SIGN_IN) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                resetEmailInput = email.trim()
                                resetDialogError = null
                                showForgotPasswordDialog = true
                            },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("auth_forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Email Action Button (Sign In or Register)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val trimmedEmail = email.trim()
                        if (!AuthValidator.isValidEmail(trimmedEmail)) {
                            errorMessage = "Please enter a valid email address (e.g. name@domain.com)."
                            return@Button
                        }
                        if (!AuthValidator.isValidPassword(password)) {
                            errorMessage = "Password must be at least 6 characters long."
                            return@Button
                        }

                        isEmailLoading = true
                        errorMessage = null
                        statusMessage = null

                        coroutineScope.launch {
                            try {
                                if (authMode == AuthMode.SIGN_IN) {
                                    Firebase.auth.signInWithEmailAndPassword(trimmedEmail, password).await()
                                    onHoldAuthForVerification(false)
                                    isEmailLoading = false
                                    onAuthSuccess()
                                } else {
                                    // Hold MainActivity on AuthScreen so AuthStateListener does not unmount before verification email & notice
                                    onHoldAuthForVerification(true)
                                    var verificationError: String? = null
                                    withContext(NonCancellable) {
                                        val result = Firebase.auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                                        val user = result.user
                                        val fallbackName = trimmedEmail.substringBefore("@")
                                            .ifBlank { "Star Artist Member" }
                                            .replaceFirstChar { it.uppercase() }
                                        try {
                                            user?.updateProfile(
                                                userProfileChangeRequest {
                                                    displayName = fallbackName
                                                }
                                            )?.await()
                                        } catch (profileError: Exception) {
                                            Log.w(TAG, "Profile displayName update skipped: ${profileError.message}")
                                        }

                                        // Automatically send email verification link upon registration
                                        try {
                                            user?.sendEmailVerification()?.await()
                                        } catch (verifError: Exception) {
                                            Log.w(TAG, "Failed sending initial email verification: ${verifError.message}")
                                            verificationError = "Account created, but verification email could not be sent automatically. Tap Resend Email below."
                                        }
                                    }

                                    registeredEmailNotice = trimmedEmail
                                    resendStatusText = verificationError
                                    isEmailLoading = false
                                    showVerificationNotice = true
                                }
                            } catch (e: CancellationException) {
                                onHoldAuthForVerification(false)
                                isEmailLoading = false
                                throw e
                            } catch (e: Exception) {
                                onHoldAuthForVerification(false)
                                isEmailLoading = false
                                errorMessage = AuthValidator.mapFirebaseError(e)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_email_action_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF161202)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isBusy
                ) {
                    if (isEmailLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF161202),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (authMode == AuthMode.SIGN_IN) Icons.AutoMirrored.Filled.Login else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (authMode == AuthMode.SIGN_IN) "Sign In with Email" else "Create Member Account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Switch Mode Prompt
                TextButton(
                    onClick = {
                        authMode = if (authMode == AuthMode.SIGN_IN) AuthMode.REGISTER else AuthMode.SIGN_IN
                        errorMessage = null
                    },
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("auth_mode_toggle_button")
                ) {
                    Text(
                        text = if (authMode == AuthMode.SIGN_IN)
                            "Need an account? Create Studio Account"
                        else
                            "Already a member? Sign in to your account",
                        color = GoldPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Divider with text "OR CONTINUE WITH"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = if (isStudioLight) Color(0xFFD6CEB5) else StudioOutline
                    )
                    Text(
                        text = "  OR CONTINUE WITH  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStudioLight) Color(0xFF756852) else GoldLight.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = if (isStudioLight) Color(0xFFD6CEB5) else StudioOutline
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Google Sign-In Button via Jetpack Credential Manager + Firebase Auth
                GoogleSignInButton(
                    isLoading = isGoogleLoading,
                    enabled = !isBusy,
                    isStudioLight = isStudioLight,
                    onClick = {
                        focusManager.clearFocus()
                        isGoogleLoading = true
                        errorMessage = null
                        statusMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManagerDelegate.value,
                            onAuthSuccess = {
                                onHoldAuthForVerification(false)
                                isGoogleLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isGoogleLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isGoogleLoading = false
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Member access enables shoot bookings, creative briefs, modeling contracts, and studio gallery favorites.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isStudioLight) Color(0xFF7A7060) else Color(0xFF888888),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showVerificationNotice) {
            AlertDialog(
                onDismissRequest = {
                    showVerificationNotice = false
                    onHoldAuthForVerification(false)
                    onAuthSuccess()
                },
                modifier = Modifier.testTag("email_verification_notice"),
                containerColor = if (isStudioLight) Color(0xFFFFFFFF) else StudioCardSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Email Verification Sent",
                        tint = GoldPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        text = "Verify Your Email Address",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "A verification email has been sent to:",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStudioLight) Color(0xFF4A4035) else Color(0xFFC0C0C0),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = registeredEmailNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isStudioLight) Color(0xFF161202) else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("auth_registered_email_text")
                        )
                        Text(
                            text = "Please check your inbox and verify your email to unlock shoot booking requests, lighting bay reservations, and studio agreements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStudioLight) Color(0xFF4A4035) else Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center
                        )
                        if (resendStatusText != null) {
                            Text(
                                text = resendStatusText ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    Firebase.auth.currentUser?.reload()?.await()
                                } catch (_: Exception) {
                                }
                                showVerificationNotice = false
                                onHoldAuthForVerification(false)
                                onAuthSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF161202)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("auth_verification_continue_button")
                    ) {
                        Text("Continue to Studio", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val user = Firebase.auth.currentUser
                                    user?.sendEmailVerification()?.await()
                                    resendStatusText = "Verification link resent to $registeredEmailNotice!"
                                } catch (e: Exception) {
                                    resendStatusText = AuthValidator.mapFirebaseError(e)
                                }
                            }
                        },
                        modifier = Modifier.testTag("auth_resend_verification_button")
                    ) {
                        Text("Resend Email", color = GoldPrimary)
                    }
                }
            )
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSendingReset) showForgotPasswordDialog = false
                },
                modifier = Modifier.testTag("auth_forgot_password_dialog"),
                containerColor = if (isStudioLight) Color(0xFFFFFFFF) else StudioCardSurface,
                title = {
                    Text(
                        text = "Reset Studio Password",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter your registered email address and we will send a password reset link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleTextColor
                        )
                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = {
                                resetEmailInput = it
                                resetDialogError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_reset_email_input"),
                            label = { Text("Registered Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = if (isStudioLight) Color(0xFFC0A040) else StudioOutline,
                                focusedLabelColor = GoldPrimary,
                                focusedTextColor = fieldTextColor,
                                unfocusedTextColor = fieldTextColor,
                                cursorColor = GoldPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSendingReset
                        )
                        if (resetDialogError != null) {
                            Text(
                                text = resetDialogError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanResetEmail = resetEmailInput.trim()
                            if (!AuthValidator.isValidEmail(cleanResetEmail)) {
                                resetDialogError = "Please enter a valid email address."
                                return@Button
                            }
                            isSendingReset = true
                            resetDialogError = null
                            coroutineScope.launch {
                                try {
                                    Firebase.auth.sendPasswordResetEmail(cleanResetEmail).await()
                                    isSendingReset = false
                                    showForgotPasswordDialog = false
                                    errorMessage = null
                                    statusMessage = "Password reset link sent to $cleanResetEmail. Check your inbox."
                                } catch (e: Exception) {
                                    isSendingReset = false
                                    resetDialogError = AuthValidator.mapFirebaseError(e)
                                }
                            }
                        },
                        enabled = !isSendingReset,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF161202)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("auth_reset_confirm_button")
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF161202),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Reset Link", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showForgotPasswordDialog = false },
                        enabled = !isSendingReset
                    ) {
                        Text("Cancel", color = GoldPrimary)
                    }
                }
            )
        }
    }
}
