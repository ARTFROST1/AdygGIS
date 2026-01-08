package com.adygyes.app.presentation.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.data.repository.AuthRepository
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.domain.model.User
import com.adygyes.app.domain.model.currentUser
import com.adygyes.app.domain.model.isAuthenticated
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication UI state and operations.
 * 
 * Improvements:
 * - Better email validation using Android Patterns
 * - Debounce protection against multiple submissions
 * - Enhanced password validation with strength indicator
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Auth state from repository
    val authState: StateFlow<AuthState> = authRepository.authState
    
    // Convenience states
    val isAuthenticated: StateFlow<Boolean> = authState.map { it.isAuthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val currentUser: StateFlow<User?> = authState.map { it.currentUser }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // UI state
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()
    
    // Debounce protection
    private var lastSubmitTime = 0L
    private companion object {
        const val DEBOUNCE_TIME_MS = 1000L
        const val MIN_PASSWORD_LENGTH = 6
    }
    
    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        if (!canSubmit()) return
        if (!validateSignInInput(email, password)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signIn(email, password)
            
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    _events.emit(AuthEvent.SignInSuccess(user))
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка входа"
                    )
                }
            )
        }
    }
    
    /**
     * Sign up with email, password, and optional display name
     */
    fun signUp(email: String, password: String, displayName: String?) {
        if (!canSubmit()) return
        if (!validateSignUpInput(email, password, displayName)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signUp(email, password, displayName)
            
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    
                    // Check if email confirmation is required
                    val currentAuthState = authState.value
                    if (currentAuthState !is AuthState.Authenticated) {
                        // Email confirmation required
                        _events.emit(AuthEvent.SignUpNeedsConfirmation(email))
                    } else {
                        _events.emit(AuthEvent.SignUpSuccess(user))
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка регистрации"
                    )
                }
            )
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            authRepository.signOut()
            
            _uiState.value = _uiState.value.copy(isLoading = false, error = null)
            _events.emit(AuthEvent.SignOutSuccess)
        }
    }
    
    /**
     * Request password reset email
     */
    fun resetPassword(email: String) {
        if (!canSubmit()) return
        
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Введите email")
            return
        }
        
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(error = "Некорректный email")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.resetPassword(email)
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    _events.emit(AuthEvent.PasswordResetSent(email))
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось отправить письмо"
                    )
                }
            )
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        authRepository.clearError()
    }
    
    /**
     * Calculate password strength for UI indicator
     */
    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.NONE
        if (password.length < MIN_PASSWORD_LENGTH) return PasswordStrength.WEAK
        
        var score = 0
        
        // Length bonus
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        
        // Character variety
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        
        return when {
            score >= 5 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
    
    // ===== Validation helpers =====
    
    /**
     * Debounce protection against multiple submissions
     */
    private fun canSubmit(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastSubmitTime < DEBOUNCE_TIME_MS) {
            return false
        }
        lastSubmitTime = now
        return true
    }
    
    private fun validateSignInInput(email: String, password: String): Boolean {
        when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Введите email")
                return false
            }
            !isValidEmail(email) -> {
                _uiState.value = _uiState.value.copy(error = "Некорректный email")
                return false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Введите пароль")
                return false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                _uiState.value = _uiState.value.copy(error = "Пароль должен быть не менее $MIN_PASSWORD_LENGTH символов")
                return false
            }
        }
        return true
    }
    
    private fun validateSignUpInput(email: String, password: String, displayName: String?): Boolean {
        if (!validateSignInInput(email, password)) return false
        
        if (!displayName.isNullOrBlank() && displayName.trim().length < 2) {
            _uiState.value = _uiState.value.copy(error = "Имя должно быть не менее 2 символов")
            return false
        }
        
        return true
    }
    
    /**
     * Validate email using Android's built-in email pattern
     * More reliable than simple @ and . check
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }
}

/**
 * Password strength levels for UI indicator
 */
enum class PasswordStrength {
    NONE,
    WEAK,
    MEDIUM,
    STRONG
}

/**
 * UI state for auth screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * One-time auth events
 */
sealed class AuthEvent {
    data class SignInSuccess(val user: User) : AuthEvent()
    data class SignUpSuccess(val user: User) : AuthEvent()
    data class SignUpNeedsConfirmation(val email: String) : AuthEvent()
    data object SignOutSuccess : AuthEvent()
    data class PasswordResetSent(val email: String) : AuthEvent()
}
