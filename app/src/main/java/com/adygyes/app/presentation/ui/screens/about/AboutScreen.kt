package com.adygyes.app.presentation.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adygyes.app.R
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.info.InfoCard
import com.adygyes.app.presentation.ui.components.info.ContactLink
import com.adygyes.app.presentation.ui.util.ShareUtils
import com.adygyes.app.presentation.viewmodel.AppSettingsViewModel
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * About screen showing app information, version, developers and contact details
 * Modern design with cards and social links
 * 
 * Uses AppSettingsViewModel to display dynamic settings from Admin Panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // App settings from Admin Panel (synced via Supabase)
    val settings by viewModel.settings.collectAsState()
    
    // Protection against double-click on back button
    var isNavigating by remember { mutableStateOf(false) }
    
    // Generate initials from developer names
    fun getInitials(fullName: String): String {
        return fullName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }
    
    Scaffold(
        // Edge-to-edge: use system bars insets
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_about_app),
                        color = MaterialTheme.colorScheme.onSurface
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
                            contentDescription = stringResource(R.string.cd_back),
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
            contentPadding = PaddingValues(Dimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "AdygGIS Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // App Name
                    Text(
                        text = "AdygGIS",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // App Slogan
                    Text(
                        text = settings.appSlogan,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Version
                    Text(
                        text = stringResource(R.string.about_version_full, settings.appVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // About App Card
            item {
                InfoCard(
                    title = stringResource(R.string.detail_about),
                    icon = Icons.Default.Info
                ) {
                    Text(
                        text = settings.appDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                }
            }
            
            // Development Team Card
            item {
                InfoCard(
                    title = stringResource(R.string.about_section_team),
                    icon = Icons.Default.Code
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Developer 1
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = getInitials(settings.developer1Name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = settings.developer1Name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = settings.developer1Role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Developer 2 (clickable → telegram)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ShareUtils.openUrl(context, settings.telegramUrl2) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = getInitials(settings.developer2Name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = settings.developer2Name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = settings.developer2Role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Useful Links Card
            item {
                InfoCard(
                    title = stringResource(R.string.about_section_links),
                    icon = Icons.Default.Link
                ) {
                    Column {
                        ContactLink(
                            icon = Icons.Default.Language,
                            label = stringResource(R.string.contact_website),
                            value = settings.websiteUrl.removePrefix("https://").removePrefix("http://"),
                            onClick = {
                                ShareUtils.openUrl(context, settings.websiteUrl)
                            }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        
                        val rateLabel = stringResource(R.string.contact_google_play)
                        val rateDesc = stringResource(R.string.settings_rate_us_desc)
                        ContactLink(
                            icon = Icons.Default.Star,
                            label = rateLabel,
                            value = rateDesc,
                            onClick = {
                                if (settings.googlePlayUrl.isNotBlank()) {
                                    ShareUtils.openUrl(context, settings.googlePlayUrl)
                                } else {
                                    Toast.makeText(
                                        context,
                                        rateDesc,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        
                        ContactLink(
                            icon = Icons.Default.Email,
                            label = stringResource(R.string.contact_email),
                            value = settings.supportEmail,
                            onClick = {
                                ShareUtils.openEmail(context, settings.supportEmail)
                            }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        
                        ContactLink(
                            icon = Icons.AutoMirrored.Filled.Send,
                            label = stringResource(R.string.contact_telegram),
                            value = settings.telegramHandle,
                            onClick = {
                                ShareUtils.openUrl(context, settings.telegramUrl)
                            }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        
                        ContactLink(
                            icon = Icons.AutoMirrored.Filled.Send,
                            label = "Telegram разработчика",
                            value = settings.telegramHandle2,
                            onClick = {
                                ShareUtils.openUrl(context, settings.telegramUrl2)
                            }
                        )
                    }
                }
            }
            
            // Legal Information Card
            item {
                InfoCard(
                    title = stringResource(R.string.about_section_legal),
                    icon = Icons.Default.Gavel
                ) {
                    Column {
                        ContactLink(
                            icon = Icons.Default.PrivacyTip,
                            label = stringResource(R.string.settings_privacy),
                            value = stringResource(R.string.settings_privacy_desc),
                            onClick = onNavigateToPrivacy
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        
                        ContactLink(
                            icon = Icons.AutoMirrored.Filled.Article,
                            label = stringResource(R.string.settings_terms),
                            value = stringResource(R.string.settings_terms_desc),
                            onClick = onNavigateToTerms
                        )
                    }
                }
            }
            
            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
