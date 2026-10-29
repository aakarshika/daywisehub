package com.example.todoapp.screen.missions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.backup.Backup
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText

/** Save everything to one file, or restore from one. The file moves between iPhone and Android. */
@Composable
fun BackupDialog(note: String?, onSave: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Done") }
        },
        title = { Text("Backup") },
        text = {
            Column {
                Text(
                    "Save all your missions and days to one file. Keep it in Files or Drive, or send it to yourself. " +
                        "It opens on iPhone and Android, so it also moves your data to a new phone.",
                    fontSize = 13.sp, color = InkGray
                )
                TextButton(onClick = onSave, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp).padding(top = 8.dp)) {
                    Text("Save a backup", fontSize = 16.sp)
                }
                TextButton(onClick = onRestore, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("Restore from a file", fontSize = 16.sp)
                }
                note?.let { Text(it, fontSize = 14.sp, color = InkGray, modifier = Modifier.padding(top = 4.dp)) }
            }
        },
        containerColor = Light_Yellowww
    )
}

/** "Replace everything?" before a restore. */
@Composable
fun RestoreConfirmDialog(backup: Backup, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    val missions = if (backup.missionCount == 1) "1 mission" else "${backup.missionCount} missions"
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Replace") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Cancel") }
        },
        title = { Text("Replace everything?") },
        text = {
            Text(
                "This backup is from ${backup.exportedOn}, with $missions. Everything in the app now is replaced by it.",
                fontSize = 14.sp, color = InkGray
            )
        },
        containerColor = Light_Yellowww
    )
}
