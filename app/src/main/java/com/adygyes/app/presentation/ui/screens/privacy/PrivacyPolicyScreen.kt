package com.adygyes.app.presentation.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adygyes.app.R
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.info.ExpandableSection
import com.adygyes.app.presentation.ui.components.info.ContactLink
import com.adygyes.app.presentation.ui.components.info.InfoCard
import com.adygyes.app.presentation.ui.util.ShareUtils
import com.adygyes.app.presentation.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Privacy Policy screen with expandable sections
 * Modern design following best practices
 * 
 * Uses AppSettingsViewModel for dynamic contact information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // App settings from Admin Panel
    val settings by viewModel.settings.collectAsState()
    
    // Protection against double-click
    var isNavigating by remember { mutableStateOf(false) }
    
    // Last updated date
    val lastUpdated = "11 января 2026"
    
    Scaffold(
        // Edge-to-edge: use system bars insets
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privacy_policy_title),
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
            // Header Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.privacy_policy_full_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Last Updated
                    Text(
                        text = stringResource(R.string.privacy_last_updated, lastUpdated),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Expandable Sections
            item {
                ExpandableSection(
                    title = stringResource(R.string.privacy_introduction_title),
                    icon = Icons.Default.Info,
                    content = stringResource(R.string.privacy_introduction_content),
                    initiallyExpanded = true
                )
            }
            
            item {
                ExpandableSection(
                    title = stringResource(R.string.privacy_data_collection_title),
                    icon = Icons.Default.Storage,
                    content = stringResource(R.string.privacy_data_collection_content)
                )
            }
            
            item {
                ExpandableSection(
                    title = stringResource(R.string.privacy_data_usage_title),
                    icon = Icons.Default.Lock,
                    content = stringResource(R.string.privacy_data_usage_content)
                )
            }
            
            item {
                ExpandableSection(
                    title = stringResource(R.string.privacy_data_sharing_title),
                    icon = Icons.Default.Group,
                    content = stringResource(R.string.privacy_data_sharing_content)
                )
            }
            
            // Contact Section
            item {
                InfoCard(
                    title = stringResource(R.string.privacy_contact_title),
                    icon = Icons.Default.Email
                ) {
                    Column {
                        ContactLink(
                            icon = Icons.Default.Email,
                            label = stringResource(R.string.contact_email),
                            value = stringResource(R.string.settings_support_email_value),
                            onClick = {
                                ShareUtils.openEmail(context, ShareUtils.SUPPORT_EMAIL)
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
