/*
 * SPDX-FileCopyrightText: 2026 AxionOS
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.applications

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.internal.applications.LongScreen

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isEnabled: Boolean
)

data class ForceFullscreenUiState(
    val apps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class ForceFullscreenViewModel(application: Application) : AndroidViewModel(application) {
    private val packageManager = application.packageManager
    private val longScreen = LongScreen(application)

    private val _uiState = MutableStateFlow(ForceFullscreenUiState())
    val uiState: StateFlow<ForceFullscreenUiState> = _uiState.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(_allApps, _searchQuery) { apps, query ->
                ForceFullscreenUiState(
                    apps = if (query.isEmpty()) {
                        apps
                    } else {
                        apps.filter { it.label.contains(query, ignoreCase = true) }
                    },
                    searchQuery = query,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
        loadApps()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleApp(packageName: String, enable: Boolean) {
        if (enable) {
            longScreen.addApp(packageName)
        } else {
            longScreen.removeApp(packageName)
        }
        updateAppState(packageName, enable)
    }

    private fun updateAppState(packageName: String, enabled: Boolean) {
        val currentApps = _allApps.value.toMutableList()
        val index = currentApps.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            currentApps[index] = currentApps[index].copy(isEnabled = enabled)
            _allApps.value = currentApps
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                val resolveInfoList = packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                )

                resolveInfoList
                    .asSequence()
                    .mapNotNull { resolveInfo ->
                        try {
                            val packageName = resolveInfo.activityInfo.packageName
                            val appInfo = packageManager.getApplicationInfo(
                                packageName,
                                PackageManager.ApplicationInfoFlags.of(0)
                            )
                            
                            AppItem(
                                packageName = packageName,
                                label = appInfo.loadLabel(packageManager).toString(),
                                icon = appInfo.loadIcon(packageManager),
                                isEnabled = longScreen.shouldForceLongScreen(packageName)
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
                    .toList()
            }
            
            _allApps.value = apps
        }
    }
}
