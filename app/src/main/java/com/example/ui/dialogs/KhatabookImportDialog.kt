package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.ImportedKhataRecord
import com.example.util.KhatabookImporter

@Composable
fun KhatabookImportDialog(
    onDismiss: () -> Unit,
    onImportRecords: (List<ImportedKhataRecord>) -> Unit
) {
    var csvText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }

    val parsedPreview = remember(csvText) {
        KhatabookImporter.parseCsvContent(csvText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Khatabook / CSV Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste CSV lines (Party Name, Phone, Amount, Type, Note) or load sample data to migrate your records into Kirio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            csvText = KhatabookImporter.getSampleCsvTemplate()
                            statusText = null
                        }
                    ) {
                        Text("Load Sample CSV")
                    }
                }

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("CSV Content") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Detected ${parsedPreview.size} valid ledger entries ready to import.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (statusText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedPreview.isEmpty()) {
                        statusText = "No valid rows found. Tap 'Load Sample CSV' to see the format."
                        return@Button
                    }
                    onImportRecords(parsedPreview)
                }
            ) {
                Text("Import (${parsedPreview.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
