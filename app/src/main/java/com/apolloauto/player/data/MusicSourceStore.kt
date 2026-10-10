package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists multiple music roots and merges their catalogs into [LibraryStore].
 * Migrates legacy single USB tree + PHONE/USB exclusive source mode.
 */
class MusicSourceStore(
    context: Context,
    private val libraryStore: LibraryStore
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val tracksBySource = linkedMapOf<String, List<Track>>()
    private val playlistFilesBySource = linkedMapOf<String, List<DiscoveredPlaylistFile>>()

    private val _sources = MutableStateFlow(loadOrMigrate())
    val sources: StateFlow<List<MusicSource>> = _sources.asStateFlow()

    private val _filter = MutableStateFlow(LibraryFilterState())
    val filter: StateFlow<LibraryFilterState> = _filter.asStateFlow()

    private val _scanErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val scanErrors: StateFlow<Map<String, String>> = _scanErrors.asStateFlow()

    fun phoneMediastore(): MusicSource =
        _sources.value.find { it.id == PHONE_MEDIASTORE_ID }
            ?: MusicSource(
                id = PHONE_MEDIASTORE_ID,
                kind = MusicSourceKind.PHONE_MEDIASTORE,
                label = "Phone library",
                preferred = true
            )

    fun setFilter(state: LibraryFilterState) {
        _filter.value = state
        publishMerged()
    }

    fun rename(id: String, label: String) {
        update(id) { it.copy(label = label.ifBlank { it.label }) }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        update(id) { it.copy(enabled = enabled) }
        if (!enabled) {
            tracksBySource.remove(id)
            publishMerged()
        }
    }

    fun setPreferred(id: String) {
        persist(
            _sources.value.map {
                it.copy(preferred = it.id == id)
            }
        )
    }

    fun removeSource(id: String) {
        if (id == PHONE_MEDIASTORE_ID) {
            // Keep MediaStore root; just clear its tracks if disabled.
            setEnabled(id, false)
            return
        }
        tracksBySource.remove(id)
        playlistFilesBySource.remove(id)
        persist(_sources.value.filterNot { it.id == id })
        publishMerged()
    }

    fun upsertSafSource(
        treeUri: Uri,
        kind: MusicSourceKind,
        label: String,
        makePreferred: Boolean = false
    ): MusicSource {
        val uriStr = treeUri.toString()
        val existing = _sources.value.find { it.treeUri == uriStr }
        if (existing != null) {
            val updated = existing.copy(
                kind = kind,
                label = label.ifBlank { existing.label },
                enabled = true,
                connected = true,
                preferred = makePreferred || existing.preferred
            )
            persist(
                _sources.value.map {
                    when {
                        it.id == existing.id -> updated
                        makePreferred -> it.copy(preferred = false)
                        else -> it
                    }
                }
            )
            return updated
        }
        val created = MusicSource(
            id = "src_${System.currentTimeMillis()}",
            kind = kind,
            label = label.ifBlank { defaultLabel(kind) },
            treeUri = uriStr,
            enabled = true,
            preferred = makePreferred,
            connected = true
        )
        persist(
            _sources.value.map { if (makePreferred) it.copy(preferred = false) else it } + created
        )
        // Legacy mirror for older UI paths
        if (kind == MusicSourceKind.USB_SSD) {
            libraryStore.setTreeUri(treeUri)
        }
        return created
    }

    fun recordScanResult(
        sourceId: String,
        tracks: List<Track>,
        playlistFiles: List<DiscoveredPlaylistFile> = emptyList(),
        error: String? = null,
        connected: Boolean = true
    ) {
        tracksBySource[sourceId] = tracks
        playlistFilesBySource[sourceId] = playlistFiles
        val errMap = _scanErrors.value.toMutableMap()
        if (error.isNullOrBlank()) errMap.remove(sourceId) else errMap[sourceId] = error
        _scanErrors.value = errMap
        update(sourceId) {
            it.copy(
                trackCount = tracks.size,
                playlistCount = playlistFiles.size,
                lastScanMs = System.currentTimeMillis(),
                lastError = error,
                connected = connected
            )
        }
        publishMerged()
    }

    fun discoveredPlaylists(): List<DiscoveredPlaylistFile> =
        playlistFilesBySource.values.flatten()

    fun tracksForSource(sourceId: String): List<Track> =
        tracksBySource[sourceId].orEmpty()

    fun markDisconnected(sourceId: String) {
        val existing = tracksBySource[sourceId].orEmpty().map { it.copy(available = false) }
        tracksBySource[sourceId] = existing
        update(sourceId) { it.copy(connected = false) }
        publishMerged()
    }

    fun filteredTracks(): List<Track> {
        val all = libraryStore.tracks.value
        val f = _filter.value
        return when (f.mode) {
            LibraryFilter.ALL -> all
            LibraryFilter.PHONE -> all.filter {
                it.sourceKind == MusicSourceKind.PHONE_MEDIASTORE ||
                    it.sourceKind == MusicSourceKind.PHONE_FOLDER
            }
            LibraryFilter.USB -> all.filter { it.sourceKind == MusicSourceKind.USB_SSD }
            LibraryFilter.SD -> all.filter { it.sourceKind == MusicSourceKind.SD_CARD }
            LibraryFilter.SOURCE -> all.filter { it.sourceId == f.sourceId }
        }
    }

    private fun publishMerged() {
        val enabledIds = _sources.value.filter { it.enabled }.map { it.id }.toSet()
        val merged = tracksBySource
            .filterKeys { it in enabledIds }
            .values
            .flatten()
            .distinctBy { it.id }
        libraryStore.setTracks(merged)
        val preferred = _sources.value.find { it.preferred && it.enabled }
            ?: _sources.value.find { it.enabled }
        when (preferred?.kind) {
            MusicSourceKind.USB_SSD -> {
                libraryStore.setSource(LibrarySource.USB)
                preferred.treeUri?.let { libraryStore.setTreeUri(Uri.parse(it)) }
            }
            else -> libraryStore.setSource(LibrarySource.PHONE)
        }
        val total = merged.size
        val online = _sources.value.count { it.enabled && it.connected }
        libraryStore.setStatus("$total tracks · $online source(s) online")
    }

    private fun update(id: String, transform: (MusicSource) -> MusicSource) {
        persist(_sources.value.map { if (it.id == id) transform(it) else it })
    }

    private fun persist(list: List<MusicSource>) {
        val normalized = normalize(list)
        _sources.value = normalized
        val arr = JSONArray()
        normalized.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("kind", s.kind.name)
                    .put("label", s.label)
                    .put("treeUri", s.treeUri)
                    .put("enabled", s.enabled)
                    .put("preferred", s.preferred)
                    .put("trackCount", s.trackCount)
                    .put("playlistCount", s.playlistCount)
                    .put("lastScanMs", s.lastScanMs)
                    .put("lastError", s.lastError)
                    .put("connected", s.connected)
            )
        }
        prefs.edit().putString(KEY_SOURCES, arr.toString()).apply()
    }

    private fun normalize(list: List<MusicSource>): List<MusicSource> {
        val hasPhone = list.any { it.id == PHONE_MEDIASTORE_ID }
        val base = if (hasPhone) list else {
            listOf(
                MusicSource(
                    id = PHONE_MEDIASTORE_ID,
                    kind = MusicSourceKind.PHONE_MEDIASTORE,
                    label = "Phone library",
                    preferred = list.none { it.preferred },
                    enabled = true
                )
            ) + list
        }
        val preferredCount = base.count { it.preferred }
        return if (preferredCount == 1) base
        else base.mapIndexed { index, s ->
            s.copy(preferred = if (preferredCount == 0) s.id == PHONE_MEDIASTORE_ID else index == base.indexOfFirst { it.preferred })
        }
    }

    private fun loadOrMigrate(): List<MusicSource> {
        val raw = prefs.getString(KEY_SOURCES, null)
        if (raw != null) {
            return normalize(parse(raw))
        }
        // Migrate legacy LibraryStore single USB tree.
        val legacyTree = libraryStore.treeUri.value?.toString()
        val legacySource = libraryStore.source.value
        val phone = MusicSource(
            id = PHONE_MEDIASTORE_ID,
            kind = MusicSourceKind.PHONE_MEDIASTORE,
            label = "Phone library",
            preferred = legacySource != LibrarySource.USB || legacyTree == null,
            enabled = true
        )
        val usb = if (legacyTree != null) {
            MusicSource(
                id = "src_legacy_usb",
                kind = MusicSourceKind.USB_SSD,
                label = "USB SSD",
                treeUri = legacyTree,
                preferred = legacySource == LibrarySource.USB,
                enabled = true,
                connected = true
            )
        } else null
        val migrated = listOfNotNull(phone, usb)
        persist(migrated)
        return migrated
    }

    private fun parse(raw: String): List<MusicSource> = runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    MusicSource(
                        id = o.getString("id"),
                        kind = runCatching { MusicSourceKind.valueOf(o.getString("kind")) }
                            .getOrDefault(MusicSourceKind.OTHER),
                        label = o.optString("label", "Library"),
                        treeUri = o.optString("treeUri").takeIf { it.isNotBlank() && it != "null" },
                        enabled = o.optBoolean("enabled", true),
                        preferred = o.optBoolean("preferred", false),
                        trackCount = o.optInt("trackCount", 0),
                        playlistCount = o.optInt("playlistCount", 0),
                        lastScanMs = o.optLong("lastScanMs", 0L),
                        lastError = o.optString("lastError").takeIf { it.isNotBlank() && it != "null" },
                        connected = o.optBoolean("connected", true)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    companion object {
        const val PHONE_MEDIASTORE_ID = "phone_mediastore"
        private const val PREFS = "apollo_music_sources"
        private const val KEY_SOURCES = "sources_json"

        fun defaultLabel(kind: MusicSourceKind): String = when (kind) {
            MusicSourceKind.PHONE_MEDIASTORE -> "Phone library"
            MusicSourceKind.PHONE_FOLDER -> "Phone folder"
            MusicSourceKind.USB_SSD -> "USB SSD"
            MusicSourceKind.SD_CARD -> "SD Card"
            MusicSourceKind.OTHER -> "Music folder"
        }

        fun inferKindFromDisplayName(name: String?): MusicSourceKind {
            val n = name?.lowercase().orEmpty()
            return when {
                n.contains("sd") || n.contains("card") -> MusicSourceKind.SD_CARD
                n.contains("usb") || n.contains("otg") || n.contains("portable") -> MusicSourceKind.USB_SSD
                else -> MusicSourceKind.PHONE_FOLDER
            }
        }
    }
}

data class DiscoveredPlaylistFile(
    val sourceId: String,
    val name: String,
    val uri: Uri,
    val relativePath: String
)
