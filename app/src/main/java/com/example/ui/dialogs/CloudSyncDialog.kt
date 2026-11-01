package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncDialog(
    profile: BusinessProfile,
    partiesCount: Int,
    transactionsCount: Int,
    syncStatusMessage: String?,
    onBackupNow: () -> Unit,
    onRestoreNow: () -> Unit,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = remember(profile.languageCode) { Localization.getStrings(profile.languageCode) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    var localFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = strings.backupFile,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.backupFile,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Export your ledger to a file on your device (survives uninstall) or save a quick snapshot.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "• Active Parties: $partiesCount\n• Total Entries: $transactionsCount\n• Last Saved: ${
                        profile.lastSyncedAt?.let { dateFormat.format(Date(it)) } ?: "Never"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val status = localFeedback ?: syncStatusMessage
                if (!status.isNullOrBlank()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // File Export Button (Primary document export)
                Button(
                    onClick = onExportFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = strings.exportBackup)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.exportBackup, fontWeight = FontWeight.Bold)
                }

                // File Import Button (Open file from device)
                FilledTonalButton(
                    onClick = onImportFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = strings.importBackup)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.importBackup, fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(modifier = Modifier.height(8.dp))

                // Internal Snapshot actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onBackupNow()
                            localFeedback = "Snapshot saved!"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Snapshot")
                    }

                    OutlinedButton(
                        onClick = onRestoreNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Snapshot")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
