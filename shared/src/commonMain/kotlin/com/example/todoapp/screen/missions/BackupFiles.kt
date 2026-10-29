package com.example.todoapp.screen.missions

import androidx.compose.runtime.Composable

/** The platform's dialogs for a backup file: Android's save and open pickers, iOS's share sheet and Files picker. */
interface BackupFiles {
    fun save(fileName: String, text: String)
    fun open()
}

/** [onSaved] gets false when the file didn't write; [onOpened] gets null when the picked file couldn't be read. A cancel calls neither. */
@Composable
expect fun rememberBackupFiles(onSaved: (Boolean) -> Unit, onOpened: (String?) -> Unit): BackupFiles
