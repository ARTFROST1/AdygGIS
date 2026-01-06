package com.adygyes.app.presentation.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adygyes.app.presentation.theme.Dimensions

/**
 * Authentication mode
 */
enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

/**
 * Auth Modal Component for login/register/password reset
 * Unified with RN AuthModal component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthModal(
    visible: Boolean,
    onClose: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, displayName: String?) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!visible) return
    
    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    
    // Reset state when modal opens
    LaunchedEffect(visible) {
        if (visible) {
            authMode = AuthMode.SIGN_IN
            email = ""
            password = ""
            displayName = ""
            passwordVisible = false
        }
    }
    
    // Clear error when switching modes
    LaunchedEffect(authMode) {
        onClearError()
    }
    
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Modal content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Consume click to prevent closing */ },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.PaddingLarge),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (authMode) {
                                AuthMode.SIGN_IN -> "Вход"
                                AuthMode.SIGN_UP -> "Регистрация"
                                AuthMode.FORGOT_PASSWORD -> "Восстановление пароля"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimensions.PaddingLarge)
                    ) {
                        // Description text
                        Text(
                            text = when (authMode) {
                                AuthMode.SIGN_IN -> "Войдите, чтобы оставлять отзывы и сохранять избранное"
                                AuthMode.SIGN_UP -> "Создайте аккаунт, чтобы оставлять отзывы"
                                AuthMode.FORGOT_PASSWORD -> "Введите email для восстановления пароля"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        // Display name field (sign up only)
                        AnimatedVisibility(visible = authMode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                label = { Text("Ваше имя") },
                                placeholder = { Text("Как вас называть?") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        // Email field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            label = { Text("Email") },
                            placeholder = { Text("example@mail.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = if (authMode == AuthMode.FORGOT_PASSWORD) 
                                    ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = {
                                    if (authMode == AuthMode.FORGOT_PASSWORD) {
                                        focusManager.clearFocus()
                                        onForgotPassword(email)
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        // Password field (sign in and sign up only)
                        AnimatedVisibility(visible = authMode != AuthMode.FORGOT_PASSWORD) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                label = { Text("Пароль") },
                                placeholder = { Text("Минимум 6 символов") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) 
                                                Icons.Default.VisibilityOff 
                                            else 
                                                Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) 
                                                "Скрыть пароль" 
                                            else 
                                                "Показать пароль"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        when (authMode) {
                                            AuthMode.SIGN_IN -> onSignIn(email, password)
                                            AuthMode.SIGN_UP -> onSignUp(email, password, displayName.takeIf { it.isNotBlank() })
                                            else -> {}
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        // Error message
                        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        // Forgot password link (sign in only)
                        if (authMode == AuthMode.SIGN_IN) {
                            TextButton(
                                onClick = { authMode = AuthMode.FORGOT_PASSWORD },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Забыли пароль?",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Main action button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                when (authMode) {
                                    AuthMode.SIGN_IN -> onSignIn(email, password)
                                    AuthMode.SIGN_UP -> onSignUp(email, password, displayName.takeIf { it.isNotBlank() })
                                    AuthMode.FORGOT_PASSWORD -> onForgotPassword(email)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = when (authMode) {
                                        AuthMode.SIGN_IN -> "Войти"
                                        AuthMode.SIGN_UP -> "Зарегистрироваться"
                                        AuthMode.FORGOT_PASSWORD -> "Отправить"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Toggle between sign in and sign up
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (authMode) {
                                    AuthMode.SIGN_IN -> "Нет аккаунта?"
                                    AuthMode.SIGN_UP -> "Уже есть аккаунт?"
                                    AuthMode.FORGOT_PASSWORD -> "Вспомнили пароль?"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            TextButton(
                                onClick = {
                                    authMode = when (authMode) {
                                        AuthMode.SIGN_IN -> AuthMode.SIGN_UP
                                        AuthMode.SIGN_UP -> AuthMode.SIGN_IN
                                        AuthMode.FORGOT_PASSWORD -> AuthMode.SIGN_IN
                                    }
                                }
                            ) {
                                Text(
                                    text = when (authMode) {
                                        AuthMode.SIGN_IN -> "Зарегистрироваться"
                                        AuthMode.SIGN_UP -> "Войти"
                                        AuthMode.FORGOT_PASSWORD -> "Войти"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/**
 * Small auth prompt banner that can be shown inline
 */
@Composable
fun AuthPromptBanner(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Войдите, чтобы оставить отзыв",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ваш опыт поможет другим",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = onSignInClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Войти")
            }
        }
    }
}
