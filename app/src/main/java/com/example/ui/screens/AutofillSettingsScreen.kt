package com.example.ui.screens

import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autofill.AutofillHelper
import com.example.data.model.VaultCategory
import com.example.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillSettingsScreen(
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val isAutofillEnabled by viewModel.isAutofillEnabled.collectAsStateWithLifecycle()
    val allItems by viewModel.allVaultItems.collectAsStateWithLifecycle()

    // Interactive Sandbox state
    var selectedDomain by remember { mutableStateOf("accounts.google.com") }
    var testUsername by remember { mutableStateOf("") }
    var testPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshSystemStatus(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autofill Capabilities", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshSystemStatus(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("autofill_settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // System Status Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("autofill_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAutofillEnabled) Color(0xFF10B981).copy(alpha = 0.15f)
                                    else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAutofillEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isAutofillEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAutofillEnabled) "PassGuard Autofill Active" else "Autofill Not Enabled",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAutofillEnabled)
                                    "Ready to autofill usernames and passwords system-wide"
                                else
                                    "Enable in Android Settings to activate automatic filling",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            AutofillHelper.openAutofillSettings(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_autofill_settings_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAutofillEnabled) "Configure Autofill Settings" else "Enable PassGuard Autofill")
                    }
                }
            }

            // Interactive Autofill Simulator / Test Lab
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("autofill_sandbox_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Autofill Sandbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Simulate how PassGuard detects web forms and injects matching credentials securely.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target domain selector
                    Text(
                        text = "Target Web Domain",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("accounts.google.com", "github.com", "amazon.com").forEach { domain ->
                            FilterChip(
                                selected = selectedDomain == domain,
                                onClick = { selectedDomain = domain },
                                label = { Text(domain.split(".").takeLast(2).joinToString("."), fontSize = 11.sp) }
                            )
                        }
                    }

                    // Simulated form
                    OutlinedTextField(
                        value = testUsername,
                        onValueChange = { testUsername = it },
                        label = { Text("Email or Username") },
                        placeholder = { Text("Tap 'Autofill Match' below") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_username_input")
                    )

                    OutlinedTextField(
                        value = testPassword,
                        onValueChange = { testPassword = it },
                        label = { Text("Password") },
                        placeholder = { Text("Will fill decrypted password") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val match = allItems.firstOrNull { item ->
                                    item.websiteUrl.contains(selectedDomain) ||
                                    item.title.lowercase().contains(selectedDomain.split(".")[0])
                                }
                                if (match != null) {
                                    testUsername = match.username
                                    testPassword = viewModel.getDecryptedPassword(match)
                                    Toast.makeText(context, "Autofilled credentials for ${match.title}!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No vault match found for $selectedDomain", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sandbox_autofill_button")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Autofill Match")
                        }

                        OutlinedButton(
                            onClick = {
                                if (testPassword.isNotBlank()) {
                                    viewModel.saveVaultItem(
                                        title = selectedDomain.split(".")[0].replaceFirstChar { it.uppercase() },
                                        username = testUsername,
                                        plainPassword = testPassword,
                                        websiteUrl = "https://$selectedDomain",
                                        category = VaultCategory.LOGIN,
                                        notes = "Captured from Autofill Sandbox",
                                        isFavorite = false,
                                        requireBiometric = false
                                    )
                                    Toast.makeText(context, "Saved login to Vault!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a password to test save", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sandbox_save_button")
                        ) {
                            Text("Test Auto-Save")
                        }
                    }
                }
            }

            // How Autofill Works Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Security Architecture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Text(
                        text = "• PassGuard inspects node hints (AUTOFILL_HINT_USERNAME, AUTOFILL_HINT_PASSWORD) and web domains without tracking keystrokes.\n" +
                               "• Decryption occurs in isolated memory space using AES-256 GCM only when a dataset is tapped.\n" +
                               "• Protected vault items require biometric authentication before filling.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
