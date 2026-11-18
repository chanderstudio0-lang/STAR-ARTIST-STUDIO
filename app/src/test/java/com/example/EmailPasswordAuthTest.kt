package com.example

import com.example.ui.auth.AuthMode
import com.example.ui.auth.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailPasswordAuthTest {

    @Test
    fun emailValidation_acceptsStandardAndSubdomainFormats() {
        // Valid emails
        assertTrue(AuthValidator.isValidEmail("client@starartist.com"))
        assertTrue(AuthValidator.isValidEmail("vikram.singh@gmail.com"))
        assertTrue(AuthValidator.isValidEmail("pooja+studio@domain.co.in"))
        assertTrue(AuthValidator.isValidEmail("fashion_model123@agency.org"))

        // Invalid emails
        assertFalse(AuthValidator.isValidEmail(""))
        assertFalse(AuthValidator.isValidEmail("   "))
        assertFalse(AuthValidator.isValidEmail("plainaddress"))
        assertFalse(AuthValidator.isValidEmail("missingdomain@"))
        assertFalse(AuthValidator.isValidEmail("@missingusername.com"))
        assertFalse(AuthValidator.isValidEmail("user@domain"))
        assertFalse(AuthValidator.isValidEmail("user@.com"))
        assertFalse(AuthValidator.isValidEmail("space in@domain.com"))
    }

    @Test
    fun passwordValidation_enforcesMinimumSixCharacters() {
        // Valid passwords (>= 6 chars)
        assertTrue(AuthValidator.isValidPassword("123456"))
        assertTrue(AuthValidator.isValidPassword("StudioGold2026"))
        assertTrue(AuthValidator.isValidPassword("mypassword!"))

        // Invalid passwords (< 6 chars)
        assertFalse(AuthValidator.isValidPassword(""))
        assertFalse(AuthValidator.isValidPassword("12345"))
        assertFalse(AuthValidator.isValidPassword("abc"))
        assertFalse(AuthValidator.isValidPassword("a"))
    }

    @Test
    fun mapFirebaseError_translatesUserNotFound() {
        val ex1 = Exception("There is no user record corresponding to this identifier (auth/user-not-found)")
        val mapped1 = AuthValidator.mapFirebaseError(ex1)
        assertTrue(mapped1.contains("No studio account found", ignoreCase = true))

        val ex2 = Exception("user-not-found error returned by server")
        val mapped2 = AuthValidator.mapFirebaseError(ex2)
        assertTrue(mapped2.contains("No studio account found", ignoreCase = true))
    }

    @Test
    fun mapFirebaseError_translatesWrongOrInvalidCredentials() {
        val ex1 = Exception("The password is invalid or user does not have a password (auth/wrong-password)")
        val mapped1 = AuthValidator.mapFirebaseError(ex1)
        assertTrue(mapped1.contains("Invalid credentials", ignoreCase = true))

        val ex2 = Exception("The supplied auth credential is invalid-credential")
        val mapped2 = AuthValidator.mapFirebaseError(ex2)
        assertTrue(mapped2.contains("Invalid credentials", ignoreCase = true))

        val ex3 = Exception("The email address is badly formatted.")
        val mapped3 = AuthValidator.mapFirebaseError(ex3)
        assertTrue(mapped3.contains("Invalid credentials", ignoreCase = true))
    }

    @Test
    fun mapFirebaseError_translatesEmailAlreadyInUse() {
        val ex = Exception("The email address is already in use by another account (auth/email-already-in-use)")
        val mapped = AuthValidator.mapFirebaseError(ex)
        assertTrue(mapped.contains("already exists", ignoreCase = true))
        assertTrue(mapped.contains("Sign In", ignoreCase = true))
    }

    @Test
    fun mapFirebaseError_translatesWeakPassword() {
        val ex = Exception("Password should be at least 6 characters (auth/weak-password)")
        val mapped = AuthValidator.mapFirebaseError(ex)
        assertTrue(mapped.contains("at least 6 characters", ignoreCase = true))
    }

    @Test
    fun mapFirebaseError_passesThroughInformativeCustomErrors() {
        val ex = Exception("Network timeout connecting to studio authentication service")
        val mapped = AuthValidator.mapFirebaseError(ex)
        assertEquals("Network timeout connecting to studio authentication service", mapped)
    }

    @Test
    fun authMode_switchesBetweenSignInAndRegister() {
        var mode = AuthMode.SIGN_IN
        assertEquals(AuthMode.SIGN_IN, mode)

        // Toggle to Register
        mode = if (mode == AuthMode.SIGN_IN) AuthMode.REGISTER else AuthMode.SIGN_IN
        assertEquals(AuthMode.REGISTER, mode)

        // Toggle back to Sign In
        mode = if (mode == AuthMode.SIGN_IN) AuthMode.REGISTER else AuthMode.SIGN_IN
        assertEquals(AuthMode.SIGN_IN, mode)
    }

    @Test
    fun fallbackDisplayNameFromEmail_capitalizesGracefully() {
        fun resolveFallbackName(email: String): String {
            return email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }

        assertEquals("Pooja.sharma", resolveFallbackName("pooja.sharma@example.com"))
        assertEquals("Vikram", resolveFallbackName("vikram@gmail.com"))
        assertEquals("Star_client", resolveFallbackName("star_client@studio.com"))
    }

    @Test
    fun mapFirebaseError_translatesOperationNotAllowedTooManyRequestsNetworkAndDisabled() {
        val opNotAllowed = Exception("This operation is not allowed (auth/operation-not-allowed)")
        val mappedOp = AuthValidator.mapFirebaseError(opNotAllowed)
        assertTrue(mappedOp.contains("Firebase Console", ignoreCase = true))

        val tooMany = Exception("We have blocked all requests from this device (auth/too-many-requests)")
        val mappedTooMany = AuthValidator.mapFirebaseError(tooMany)
        assertTrue(mappedTooMany.contains("Too many", ignoreCase = true))

        val networkFailed = Exception("A network error occurred (auth/network-request-failed)")
        val mappedNet = AuthValidator.mapFirebaseError(networkFailed)
        assertTrue(mappedNet.contains("Network connection error", ignoreCase = true))

        val disabled = Exception("The user account has been disabled (auth/user-disabled)")
        val mappedDisabled = AuthValidator.mapFirebaseError(disabled)
        assertTrue(mappedDisabled.contains("disabled", ignoreCase = true))
    }
}
