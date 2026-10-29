package com.example.todoapp.screen.missions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupFiles(onSaved: (Boolean) -> Unit, onOpened: (String?) -> Unit): BackupFiles {
    val context = LocalContext.current
    val saved by rememberUpdatedState(onSaved)
    val opened by rememberUpdatedState(onOpened)
    var pending by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pending
        pending = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.encodeToByteArray()) }
        }.isSuccess
        saved(ok)
    }
    // Any type: a backup sent by mail or chat often comes back as octet-stream. Reading it checks it.
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        opened(runCatching {
            context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        }.getOrNull())
    }

    return remember(saveLauncher, openLauncher) {
        object : BackupFiles {
            override fun save(fileName: String, text: String) {
                pending = text
                saveLauncher.launch(fileName)
            }

            override fun open() = openLauncher.launch(arrayOf("*/*"))
        }
    }
}
