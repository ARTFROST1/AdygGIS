package com.adygyes.app.presentation.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adygyes.app.R
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.viewmodel.SettingsViewModel
import com.adygyes.app.presentation.viewmodel.AuthViewModel
import com.adygyes.app.presentation.viewmodel.AuthEvent
import com.adygyes.app.presentation.viewmodel.AppSettingsViewModel
import com.adygyes.app.presentation.ui.components.auth.AuthModal
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.presentation.viewmodel.PasswordStrength
import com.adygyes.app.domain.model.currentUser
import com.adygyes.app.presentation.ui.util.ShareUtils
import android.widget.Toast
import com.adygyes.app.presentation.ui.util.EasterEggManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings screen for app configuration and preferences
 * 
 * Uses AppSettingsViewModel for dynamic contact information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    appSettingsViewModel: AppSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val appSettings by appSettingsViewModel.settings.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAuthModal by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    // Easter egg: 7 taps on title
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    // Protection against double-click on back button - prevents multiple popBackStack calls
    var isNavigating by remember { mutableStateOf(false) }
    
    // Handle auth events
    LaunchedEffect(Unit) {
        authViewModel.events.collect { event ->
            when (event) {
                is AuthEvent.SignInSuccess -> {
                    showAuthModal = false
                    Toast.makeText(
                        context,
                        "Добро пожаловать, ${event.user.displayName ?: event.user.email}!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is AuthEvent.SignUpSuccess -> {
                    showAuthModal = false
                    Toast.makeText(context, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                }
                is AuthEvent.SignUpNeedsConfirmation -> {
                    showAuthModal = false
                    Toast.makeText(
                        context,
                        "Письмо для подтверждения отправлено на ${event.email}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthEvent.SignOutSuccess -> {
                    Toast.makeText(context, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                }
                is AuthEvent.PasswordResetSent -> {
                    showAuthModal = false
                    Toast.makeText(
                        context,
                        "Письмо для сброса пароля отправлено на ${event.email}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        // Edge-to-edge: use system bars insets
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 1500) tapCount = 0
                            lastTapTime = now
                            tapCount += 1
                            if (tapCount >= 7) {
                                tapCount = 0
                                EasterEggManager.activate()
                                Toast.makeText(
                                    context,
                                    "Пасхалка активирована: фон карты заменён на фото до перезапуска",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                onNavigateBack()
                                coroutineScope.launch {
                                    delay(500)
                                    isNavigating = false
                                }
                            }
                        },
                        enabled = !isNavigating
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_back),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (!isNavigating) 1f else 0.5f
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = Dimensions.PaddingSmall)
        ) {
            // Account Section
            item {
                SettingsSectionHeader(title = "Аккаунт")
            }
            
            item {
                when (authState) {
                    is AuthState.Authenticated -> {
                        val user = authState.currentUser
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = user?.displayName ?: user?.email ?: "Пользователь",
                            subtitle = if (user?.displayName != null) user.email else "Нажмите, чтобы выйти",
                            onClick = { showSignOutConfirm = true }
                        )
                    }
                    is AuthState.Loading, is AuthState.Unknown -> {
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Загрузка...",
                            subtitle = "Проверка сессии",
                            onClick = { }
                        )
                    }
                    else -> {
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.Login,
                            title = "Войти",
                            subtitle = "Авторизуйтесь, чтобы оставлять отзывы",
                            onClick = { showAuthModal = true }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            }
            
            // Appearance Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_appearance))
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (uiState.theme) {
                        SettingsViewModel.Theme.LIGHT -> stringResource(R.string.settings_theme_light)
                        SettingsViewModel.Theme.DARK -> stringResource(R.string.settings_theme_dark)
                        SettingsViewModel.Theme.SYSTEM -> stringResource(R.string.settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = when (uiState.language) {
                        SettingsViewModel.Language.RUSSIAN -> stringResource(R.string.settings_language_ru)
                        SettingsViewModel.Language.ENGLISH -> stringResource(R.string.settings_language_en)
                    },
                    onClick = { showLanguageDialog = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            }
            
            // Map Settings Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_map))
            }
            
            item {
                SettingsItemSwitch(
                    icon = Icons.Default.MyLocation,
                    title = stringResource(R.string.settings_show_location),
                    subtitle = stringResource(R.string.settings_show_location_desc),
                    checked = uiState.showUserLocation,
                    onCheckedChange = { enabled -> viewModel.setShowUserLocation(enabled) }
                )
            }
            
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            }
            
            // Notifications Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_notifications))
            }
            
            item {
                SettingsItemSwitch(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_push_notifications),
                    subtitle = stringResource(R.string.settings_push_notifications_desc),
                    checked = uiState.pushNotifications,
                    onCheckedChange = { enabled -> viewModel.setPushNotifications(enabled) }
                )
            }
            
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            }
            
            // Data & Storage Section
            item {
                SettingsSectionHeader(title = "Данные и хранилище")
            }
            
            item {
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
                
                val syncSubtitle = when {
                    isSyncing -> "Синхронизация..."
                    lastSyncTimestamp != null -> {
                        try {
                            val instant = java.time.Instant.parse(lastSyncTimestamp)
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .withZone(java.time.ZoneId.systemDefault())
                            "Последняя: ${formatter.format(instant)}"
                        } catch (e: Exception) {
                            "Последняя: не выполнялась"
                        }
                    }
                    else -> "Последняя: не выполнялась"
                }
                
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Синхронизировать данные",
                    subtitle = syncSubtitle,
                    onClick = {
                        if (!isSyncing) {
                            viewModel.performSync()
                            Toast.makeText(context, "Синхронизация запущена", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSyncing && uiState.isOnline
                )
            }
            
            item {
                val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
                var cacheSize by remember { mutableStateOf("...") }
                
                LaunchedEffect(Unit) {
                    try {
                        cacheSize = viewModel.getCacheSize()
                    } catch (e: Exception) {
                        cacheSize = "0.0 МБ"
                    }
                }

                LaunchedEffect(isClearing) {
                    if (!isClearing) {
                        try {
                            cacheSize = viewModel.getCacheSize()
                        } catch (e: Exception) {
                            cacheSize = "0.0 МБ"
                        }
                    }
                }
                
                SettingsItem(
                    icon = Icons.Default.DeleteOutline,
                    title = "Очистить кэш",
                    subtitle = if (isClearing) "Очистка..." else "Занято: $cacheSize",
                    onClick = {
                        if (!isClearing) {
                            showClearCacheConfirm = true
                        }
                    },
                    enabled = !isClearing && uiState.isOnline
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            }
            
            // Information Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_information))
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about_app),
                    subtitle = "${stringResource(R.string.settings_version)} ${appSettings.appVersion}",
                    onClick = {
                        viewModel.onVersionClick()
                        onNavigateToAbout()
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            }
            
            // Rate & Share Section
            item {
                val rateTitle = stringResource(R.string.settings_rate_us)
                val rateDesc = stringResource(R.string.settings_rate_us_desc)
                SettingsSectionHeader(title = stringResource(R.string.settings_section_rate_share))
            }
            
            item {
                val rateTitle = stringResource(R.string.settings_rate_us)
                val rateDesc = stringResource(R.string.settings_rate_us_desc)
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = rateTitle,
                    subtitle = rateDesc,
                    onClick = {
                        val success = ShareUtils.rateApp(context)
                        if (!success) {
                            Toast.makeText(
                                context,
                                rateDesc,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = stringResource(R.string.settings_share_app),
                    subtitle = stringResource(R.string.settings_share_app_desc),
                    onClick = {
                        ShareUtils.shareApp(
                            context = context,
                            websiteUrl = appSettings.websiteUrl,
                            googlePlayUrl = appSettings.googlePlayUrl
                        )
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingLarge))
            }
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Очистить кэш?") },
            text = {
                Text(
                    "Это удалит все загруженные изображения и данные. " +
                        "Для загрузки данных нажмите \"Синхронизировать\" или перезапустите приложение. Продолжить?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheConfirm = false
                        viewModel.clearCache()
                        Toast.makeText(
                            context,
                            "Кэш очищен. Нажмите \"Синхронизировать\" для загрузки данных.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Отмена") }
            }
        )
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.language,
            onLanguageSelected = { language ->
                viewModel.setLanguage(language)
                showLanguageDialog = false
                // Delay to allow data to save, then recreate activity
                coroutineScope.launch {
                    delay(200)
                    (context as? android.app.Activity)?.recreate()
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.theme,
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Auth Modal
    var currentPasswordStrength by remember { mutableStateOf(PasswordStrength.NONE) }
    
    AuthModal(
        visible = showAuthModal,
        onClose = { 
            showAuthModal = false 
            authViewModel.clearError()
            currentPasswordStrength = PasswordStrength.NONE
        },
        onSignIn = { email, password -> 
            authViewModel.signIn(email, password) 
        },
        onSignUp = { email, password, displayName -> 
            authViewModel.signUp(email, password, displayName) 
        },
        onForgotPassword = { email -> 
            authViewModel.resetPassword(email) 
        },
        isLoading = authUiState.isLoading,
        errorMessage = authUiState.error,
        onClearError = { authViewModel.clearError() },
        passwordStrength = currentPasswordStrength,
        onPasswordChange = { password ->
            currentPasswordStrength = authViewModel.calculatePasswordStrength(password)
        }
    )
    
    // Sign Out Confirmation Dialog
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Выход") },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        authViewModel.signOut()
                    }
                ) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: SettingsViewModel.Language,
    onLanguageSelected: (SettingsViewModel.Language) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_select_language)) },
        text = {
            Column {
                SettingsViewModel.Language.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (language) {
                                SettingsViewModel.Language.RUSSIAN -> stringResource(R.string.settings_language_ru)
                                SettingsViewModel.Language.ENGLISH -> stringResource(R.string.settings_language_en)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: SettingsViewModel.Theme,
    onThemeSelected: (SettingsViewModel.Theme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_select_theme)) },
        text = {
            Column {
                SettingsViewModel.Theme.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (theme) {
                                    SettingsViewModel.Theme.LIGHT -> stringResource(R.string.settings_theme_light)
                                    SettingsViewModel.Theme.DARK -> stringResource(R.string.settings_theme_dark)
                                    SettingsViewModel.Theme.SYSTEM -> stringResource(R.string.settings_theme_system)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (theme) {
                                    SettingsViewModel.Theme.LIGHT -> stringResource(R.string.settings_theme_light_desc)
                                    SettingsViewModel.Theme.DARK -> stringResource(R.string.settings_theme_dark_desc)
                                    SettingsViewModel.Theme.SYSTEM -> stringResource(R.string.settings_theme_system_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
