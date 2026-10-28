package com.justice.crossshare.manager

import com.justice.crossshare.model.SharedFileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager holding state for shared and received files.
 */
object SharedFileManager {

    private val _sharedFiles = MutableStateFlow<List<SharedFileItem>>(emptyList())
    val sharedFiles: StateFlow<List<SharedFileItem>> = _sharedFiles.asStateFlow()

    private val _receivedFiles = MutableStateFlow<List<SharedFileItem>>(emptyList())
    val receivedFiles: StateFlow<List<SharedFileItem>> = _receivedFiles.asStateFlow()

    private val fileMap = ConcurrentHashMap<String, SharedFileItem>()

    fun addSharedFile(item: SharedFileItem) {
        fileMap[item.id] = item
        _sharedFiles.update { current ->
            (current.filter { it.id != item.id } + item).sortedByDescending { it.addedTimestamp }
        }
    }

    fun addSharedFiles(items: List<SharedFileItem>) {
        items.forEach { fileMap[it.id] = it }
        _sharedFiles.update { current ->
            val existingIds = items.map { it.id }.toSet()
            (current.filter { it.id !in existingIds } + items).sortedByDescending { it.addedTimestamp }
        }
    }

    fun removeSharedFile(id: String) {
        fileMap.remove(id)
        _sharedFiles.update { current -> current.filter { it.id != id } }
    }

    fun addReceivedFile(item: SharedFileItem) {
        fileMap[item.id] = item
        _receivedFiles.update { current ->
            (current.filter { it.id != item.id } + item).sortedByDescending { it.addedTimestamp }
        }
    }

    fun getSharedFile(id: String): SharedFileItem? {
        return fileMap[id]
    }

    fun getAllDownloadableFiles(): List<SharedFileItem> {
        return fileMap.values.toList().sortedByDescending { it.addedTimestamp }
    }

    fun clearAll() {
        fileMap.clear()
        _sharedFiles.value = emptyList()
        _receivedFiles.value = emptyList()
    }
}
