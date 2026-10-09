package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _treeUri = MutableStateFlow(prefs.getString(KEY_TREE, null)?.let(Uri::parse))
    val treeUri: StateFlow<Uri?> = _treeUri.asStateFlow()

    private val _source = MutableStateFlow(
        runCatching { LibrarySource.valueOf(prefs.getString(KEY_SOURCE, LibrarySource.PHONE.name)!!) }
            .getOrDefault(LibrarySource.PHONE)
    )
    val source: StateFlow<LibrarySource> = _source.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _status = MutableStateFlow("Ready — scan phone music to start")
    val status: StateFlow<String> = _status.asStateFlow()

    fun setTreeUri(uri: Uri?) {
        _treeUri.value = uri
        prefs.edit().apply {
            if (uri == null) remove(KEY_TREE) else putString(KEY_TREE, uri.toString())
        }.apply()
    }

    fun setSource(source: LibrarySource) {
        _source.value = source
        prefs.edit().putString(KEY_SOURCE, source.name).apply()
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setTracks(tracks: List<Track>) {
        _tracks.value = tracks
    }

    fun setScanning(value: Boolean) {
        _scanning.value = value
    }

    fun setStatus(message: String) {
        _status.value = message
    }

    fun filteredTracks(): List<Track> {
        val q = _query.value.trim()
        if (q.isEmpty()) return _tracks.value
        val needle = q.lowercase()
        return _tracks.value.filter {
            it.title.lowercase().contains(needle) ||
                it.artist.lowercase().contains(needle) ||
                it.album.lowercase().contains(needle) ||
                it.folder.lowercase().contains(needle)
        }
    }

    fun folders(): List<String> =
        _tracks.value.map { it.folder }.distinct().sorted()

    fun tracksInFolder(folder: String): List<Track> =
        _tracks.value.filter { it.folder == folder }

    companion object {
        private const val PREFS = "apollo_library"
        private const val KEY_TREE = "usb_tree_uri"
        private const val KEY_SOURCE = "library_source"
    }
}
