package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ItemEditDialog
import com.example.ui.viewmodel.VaultViewModel
import com.example.util.PasswordGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    var length by remember { mutableFloatStateOf(16f) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useNumbers by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(false) }
    var isPassphraseMode by remember { mutableStateOf(false) }
    var passphraseWords by remember { mutableFloatStateOf(4f) }

    var generatedPassword by remember {
        mutableStateOf(PasswordGenerator.generate(16, true, true, true, true, false))
    }

    var showCreateDialogWithPassword by remember { mutableStateOf(false) }

    fun refreshPassword() {
        generatedPassword = if (isPassphraseMode) {
            PasswordGenerator.generatePassphrase(passphraseWords.toInt(), includeNumber = true)
        } else {
            PasswordGenerator.generate(
                length = length.toInt(),
                useUpper = useUpper,
                useLower = useLower,
                useNumbers = useNumbers,
                useSymbols = useSymbols,
                avoidAmbiguous = avoidAmbiguous
            )
        }
    }

    val strength = remember(generatedPassword) {
        PasswordGenerator.evaluateStrength(generatedPassword)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator", fontWeight = FontWeight.Bold) },
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
                .testTag("generator_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Main Password Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generated_password_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = generatedPassword,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("generated_password_text")
                    )

                    // Strength indicator bar
                    LinearProgressIndicator(
                        progress = { strength.score / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(strength.colorHex),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strength.label} (${strength.score}/100)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(strength.colorHex)
                        )
                        Text(
                            text = "Crack time: ${strength.crackTimeEstimate}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { refreshPassword() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("regenerate_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regenerate")
                        }

                        Button(
                            onClick = {
                                viewModel.copyToClipboard(context, generatedPassword, "Generated Password", true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_generated_password_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            // Mode Selector: Characters vs Memorable Passphrase
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Memorable Passphrase", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("e.g. dragon-quartz-haven-42", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPassphraseMode,
                        onCheckedChange = {
                            isPassphraseMode = it
                            refreshPassword()
                        }
                    )
                }
            }

            // Options Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!isPassphraseMode) {
                        // Password Length
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Length", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("${length.toInt()} characters", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = length,
                                onValueChange = {
                                    length = it
                                    refreshPassword()
                                },
                                valueRange = 8f..48f,
                                steps = 39,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(12, 16, 20, 24, 32).forEach { preset ->
                                    FilterChip(
                                        selected = length.toInt() == preset,
                                        onClick = {
                                            length = preset.toFloat()
                                            refreshPassword()
                                        },
                                        label = { Text("$preset") }
                                    )
                                }
                            }
                        }

                        // Character toggles
                        GeneratorToggleRow("Uppercase (A-Z)", useUpper) {
                            useUpper = it
                            refreshPassword()
                        }
                        GeneratorToggleRow("Lowercase (a-z)", useLower) {
                            useLower = it
                            refreshPassword()
                        }
                        GeneratorToggleRow("Numbers (0-9)", useNumbers) {
                            useNumbers = it
                            refreshPassword()
                        }
                        GeneratorToggleRow("Special Symbols (!@#$)", useSymbols) {
                            useSymbols = it
                            refreshPassword()
                        }
                        GeneratorToggleRow("Avoid Ambiguous (1, l, 0, O)", avoidAmbiguous) {
                            avoidAmbiguous = it
                            refreshPassword()
                        }
                    } else {
                        // Passphrase Word count
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Word Count", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("${passphraseWords.toInt()} words", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = passphraseWords,
                                onValueChange = {
                                    passphraseWords = it
                                    refreshPassword()
                                },
                                valueRange = 3f..8f,
                                steps = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Quick Add to Vault
            Button(
                onClick = { showCreateDialogWithPassword = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_generated_to_vault_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Directly to Vault")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCreateDialogWithPassword) {
        val tempItem = remember(generatedPassword) {
            com.example.data.model.VaultItem(
                id = 0,
                title = "",
                username = "",
                encryptedPassword = com.example.security.CryptoEngine.encrypt(generatedPassword),
                websiteUrl = ""
            )
        }
        ItemEditDialog(
            initialItem = tempItem,
            viewModel = viewModel,
            onDismiss = { showCreateDialogWithPassword = false }
        )
    }
}

@Composable
fun GeneratorToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
