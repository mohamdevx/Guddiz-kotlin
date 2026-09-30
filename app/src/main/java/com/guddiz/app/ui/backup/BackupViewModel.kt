package com.guddiz.app.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guddiz.app.GuddizApplication
import com.guddiz.app.core.backup.BackupManager
import com.guddiz.app.core.reset.AppResetManager
import com.guddiz.app.core.reset.ResetResult
import com.guddiz.app.core.backup.BackupResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface BackupEvent {
    data class Message(val text: String, val isError: Boolean) : BackupEvent
    data object RestoreComplete : BackupEvent
    data object ResetComplete : BackupEvent
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val manager: BackupManager =
        (application as GuddizApplication).container.backupManager

    private val resetManager: AppResetManager =
        (application as GuddizApplication).container.resetManager

    private val _lastBackup = MutableStateFlow(manager.lastBackupTime())
    val lastBackup: StateFlow<Long> = _lastBackup.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onBackupSelected(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            when (val result = manager.createBackup(uri)) {
                is BackupResult.Success -> {
                    _lastBackup.value = manager.lastBackupTime()
                    _events.send(BackupEvent.Message("Backup created successfully.", false))
                }
                is BackupResult.Error ->
                    _events.send(BackupEvent.Message("Backup failed: ${result.message}", true))
            }
            _isBusy.value = false
        }
    }

    fun onRestoreSelected(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            when (val result = manager.restoreBackup(uri)) {
                is BackupResult.Success -> {
                    _events.send(BackupEvent.Message("Backup restored.", false))
                    _events.send(BackupEvent.RestoreComplete)
                }
                is BackupResult.Error ->
                    _events.send(BackupEvent.Message("Restore failed: ${result.message}", true))
            }
            _isBusy.value = false
        }
    }

    fun onResetEverything() {
        viewModelScope.launch {
            _isBusy.value = true
            when (val result = resetManager.resetEverything()) {
                is ResetResult.Success -> {
                    _events.send(BackupEvent.Message("All data cleared.", false))
                    _events.send(BackupEvent.ResetComplete)
                }
                is ResetResult.Error ->
                    _events.send(BackupEvent.Message("Reset failed: " + result.message, true))
            }
            _isBusy.value = false
        }
    }
}
