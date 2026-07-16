package com.rohan.mbtool.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.mbtool.data.HistoryEntry
import com.rohan.mbtool.data.HistoryRepository
import com.rohan.mbtool.data.OpType
import com.rohan.mbtool.engine.MaterialBinEngine
import com.rohan.mbtool.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class OpState {
    object Idle    : OpState()
    object Running : OpState()
    data class Success(val message: String) : OpState()
    data class Error(val message: String)   : OpState()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HistoryRepository(app)
    private val resolver = app.contentResolver

    val history = repo.historyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = repo.themeModeFlow.map { s ->
        ThemeMode.values().find { it.name == s } ?: ThemeMode.SYSTEM
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _decompileState = MutableStateFlow<OpState>(OpState.Idle)
    val decompileState = _decompileState.asStateFlow()

    private val _recompileState = MutableStateFlow<OpState>(OpState.Idle)
    val recompileState = _recompileState.asStateFlow()

    // ── File picker state ─────────────────────────────────────────────────────
    val _binFileUri = MutableStateFlow<Uri?>(null)
    val binFileUri: StateFlow<Uri?> = _binFileUri.asStateFlow()

    val _decompileOutUri = MutableStateFlow<Uri?>(null)
    val decompileOutUri: StateFlow<Uri?> = _decompileOutUri.asStateFlow()

    val _recompileInUri = MutableStateFlow<Uri?>(null)
    val recompileInUri: StateFlow<Uri?> = _recompileInUri.asStateFlow()

    val _recompileOutUri = MutableStateFlow<Uri?>(null)
    val recompileOutUri: StateFlow<Uri?> = _recompileOutUri.asStateFlow()

    // ── Decompile ─────────────────────────────────────────────────────────────
    fun decompile() {
        val binUri = _binFileUri.value ?: return
        val outUri = _decompileOutUri.value ?: return

        viewModelScope.launch {
            _decompileState.value = OpState.Running
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val inputStream = resolver.openInputStream(binUri)
                        ?: error("Cannot open .material.bin")
                    val outTree = DocumentFile.fromTreeUri(getApplication(), outUri)
                        ?: error("Cannot open output folder")

                    // Create a sub-folder named after the .bin file (without extension)
                    val binDoc = DocumentFile.fromSingleUri(getApplication(), binUri)
                    val baseName = binDoc?.name?.removeSuffix(".material.bin")?.removeSuffix(".bin") ?: "output"
                    val outDir = outTree.createDirectory(baseName)
                        ?: error("Cannot create output directory '$baseName'")

                    inputStream.use { MaterialBinEngine.decompile(it, outDir, resolver) }
                }
            }
            result.fold(
                onSuccess = { r ->
                    val msg = "✓ ${r.passCount} passes · ${r.shaderCount} shaders · ${r.glslCount} GLSL extracted → ${r.outputDirName}"
                    _decompileState.value = OpState.Success(msg)

                    val binDoc = DocumentFile.fromSingleUri(getApplication(), binUri)
                    repo.addEntry(HistoryEntry(
                        id         = System.currentTimeMillis(),
                        opType     = OpType.DECOMPILE,
                        fileName   = binDoc?.name ?: "unknown.bin",
                        outputName = r.outputDirName,
                        timestamp  = System.currentTimeMillis(),
                        success    = true,
                        detail     = "${r.passCount} passes, ${r.shaderCount} shaders",
                    ))
                },
                onFailure = { e ->
                    val msg = e.message ?: "Unknown error"
                    _decompileState.value = OpState.Error(msg)
                    repo.addEntry(HistoryEntry(
                        id         = System.currentTimeMillis(),
                        opType     = OpType.DECOMPILE,
                        fileName   = "unknown.bin",
                        outputName = "",
                        timestamp  = System.currentTimeMillis(),
                        success    = false,
                        detail     = msg,
                    ))
                }
            )
        }
    }

    // ── Recompile ─────────────────────────────────────────────────────────────
    fun recompile() {
        val inUri  = _recompileInUri.value  ?: return
        val outUri = _recompileOutUri.value ?: return

        viewModelScope.launch {
            _recompileState.value = OpState.Running
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val inDir = DocumentFile.fromTreeUri(getApplication(), inUri)
                        ?: error("Cannot open input folder")
                    val outDir = DocumentFile.fromTreeUri(getApplication(), outUri)
                        ?: error("Cannot open output folder")

                    val outName = (inDir.name ?: "output") + ".material.bin"
                    val outFile = outDir.createFile("application/octet-stream", outName)
                        ?: error("Cannot create output file")
                    val os = resolver.openOutputStream(outFile.uri)
                        ?: error("Cannot write output file")

                    os.use { MaterialBinEngine.recompile(inDir, it, resolver) }
                }
            }
            result.fold(
                onSuccess = { r ->
                    val msg = "✓ ${r.passCount} passes · ${r.shaderCount} shaders → ${r.materialName}.material.bin"
                    _recompileState.value = OpState.Success(msg)

                    val inDir = DocumentFile.fromTreeUri(getApplication(), inUri)
                    repo.addEntry(HistoryEntry(
                        id         = System.currentTimeMillis(),
                        opType     = OpType.RECOMPILE,
                        fileName   = inDir?.name ?: "folder",
                        outputName = "${r.materialName}.material.bin",
                        timestamp  = System.currentTimeMillis(),
                        success    = true,
                        detail     = "${r.passCount} passes, ${r.shaderCount} shaders",
                    ))
                },
                onFailure = { e ->
                    val msg = e.message ?: "Unknown error"
                    _recompileState.value = OpState.Error(msg)
                    repo.addEntry(HistoryEntry(
                        id         = System.currentTimeMillis(),
                        opType     = OpType.RECOMPILE,
                        fileName   = "folder",
                        outputName = "",
                        timestamp  = System.currentTimeMillis(),
                        success    = false,
                        detail     = msg,
                    ))
                }
            )
        }
    }

    // ── History ───────────────────────────────────────────────────────────────
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }

    // ── Theme ─────────────────────────────────────────────────────────────────
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode.name) }

    // ── Reset states ──────────────────────────────────────────────────────────
    fun resetDecompileState() { _decompileState.value = OpState.Idle }
    fun resetRecompileState() { _recompileState.value = OpState.Idle }
}
