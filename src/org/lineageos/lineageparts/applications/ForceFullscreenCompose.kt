/*
 * SPDX-FileCopyrightText: 2026 AxionOS
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.applications

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.lineageos.lineageparts.R

private sealed class ScreenState {
    object Loading : ScreenState()
    data class Empty(val hasSearchQuery: Boolean) : ScreenState()
    object Content : ScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForceFullscreenScreen() {
    val context = LocalContext.current
    val viewModel = remember {
        ForceFullscreenViewModel(context.applicationContext as Application)
    }
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = stringResource(R.string.long_screen_settings_header),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            AnimatedContent(
                targetState = when {
                    uiState.isLoading -> ScreenState.Loading
                    uiState.apps.isEmpty() -> ScreenState.Empty(uiState.searchQuery.isNotEmpty())
                    else -> ScreenState.Content
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300, easing = LinearEasing)) +
                            slideInVertically(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 4 }
                            ) togetherWith
                            fadeOut(animationSpec = tween(200, easing = LinearEasing)) +
                            slideOutVertically(
                                animationSpec = tween(200, easing = FastOutLinearInEasing),
                                targetOffsetY = { -it / 4 }
                            )
                },
                label = "screen_state_animation"
            ) { state ->
                when (state) {
                    is ScreenState.Loading -> {
                        LoadingState(modifier = Modifier.fillMaxSize())
                    }
                    is ScreenState.Empty -> {
                        EmptyState(
                            modifier = Modifier.fillMaxSize(),
                            hasSearchQuery = state.hasSearchQuery
                        )
                    }
                    is ScreenState.Content -> {
                        AppList(
                            apps = uiState.apps,
                            onToggleApp = viewModel::toggleApp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.long_screen_settings_search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun AppList(
    apps: List<AppItem>,
    onToggleApp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            AppListItem(
                app = app,
                onToggle = { enabled -> onToggleApp(app.packageName, enabled) }
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: AppItem,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.large
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingContent = {
                AppIcon(drawable = app.icon)
            },
            trailingContent = {
                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = { newValue ->
                        onToggle(newValue)
                    },
                    thumbContent = {
                        val icon = if (app.isEnabled) Icons.Filled.Check else Icons.Filled.Close
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AppIcon(
    drawable: Drawable,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) { 
        drawable.toBitmap(width = 256, height = 256).asImageBitmap() 
    }
    
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier.size(48.dp)
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    hasSearchQuery: Boolean
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasSearchQuery) {
                stringResource(R.string.long_screen_settings_no_apps_found)
            } else {
                stringResource(R.string.long_screen_settings_no_apps)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
