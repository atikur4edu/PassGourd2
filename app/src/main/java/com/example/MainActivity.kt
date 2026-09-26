package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AutofillSettingsScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MainVaultScreen
import com.example.ui.screens.PasswordGeneratorScreen
import com.example.ui.screens.SecurityAuditScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PassGuardTheme
import com.example.ui.viewmodel.VaultTab
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: VaultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PassGuardApplication
        val factory = VaultViewModel.Factory(app.repository, app.lockManager)
        viewModel = ViewModelProvider(this, factory)[VaultViewModel::class.java]

        setContent {
            PassGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()

                    if (isLocked) {
                        LockScreen(
                            viewModel = viewModel,
                            onUnlocked = {
                                // Handled inside viewModel.unlock()
                            }
                        )
                    } else {
                        PassGuardMainApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PassGuardMainApp(viewModel: VaultViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    BackHandler(enabled = activeTab != VaultTab.VAULT) {
        viewModel.setActiveTab(VaultTab.VAULT)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == VaultTab.VAULT,
                    onClick = { viewModel.setActiveTab(VaultTab.VAULT) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    modifier = Modifier.testTag("nav_tab_vault")
                )
                NavigationBarItem(
                    selected = activeTab == VaultTab.GENERATOR,
                    onClick = { viewModel.setActiveTab(VaultTab.GENERATOR) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generator") },
                    label = { Text("Generator") },
                    modifier = Modifier.testTag("nav_tab_generator")
                )
                NavigationBarItem(
                    selected = activeTab == VaultTab.AUDIT,
                    onClick = { viewModel.setActiveTab(VaultTab.AUDIT) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Audit") },
                    label = { Text("Audit") },
                    modifier = Modifier.testTag("nav_tab_audit")
                )
                NavigationBarItem(
                    selected = activeTab == VaultTab.AUTOFILL,
                    onClick = { viewModel.setActiveTab(VaultTab.AUTOFILL) },
                    icon = { Icon(Icons.Default.VpnKey, contentDescription = "Autofill") },
                    label = { Text("Autofill") },
                    modifier = Modifier.testTag("nav_tab_autofill")
                )
                NavigationBarItem(
                    selected = activeTab == VaultTab.SETTINGS,
                    onClick = { viewModel.setActiveTab(VaultTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                VaultTab.VAULT -> MainVaultScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tab -> viewModel.setActiveTab(tab) }
                )
                VaultTab.GENERATOR -> PasswordGeneratorScreen(viewModel = viewModel)
                VaultTab.AUDIT -> SecurityAuditScreen(viewModel = viewModel)
                VaultTab.AUTOFILL -> AutofillSettingsScreen(viewModel = viewModel)
                VaultTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
