package com.apolloauto.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans all enabled music sources without blocking playback.
 * MediaStore phone library + SAF roots (phone folders / USB / SD).
 */
class LibraryScanCoordinator(
    private val context: Context,
    private val sourceStore: MusicSourceStore,
    private val deviceScanner: DeviceMusicScanner,
    private val safScanner: SafLibraryScanner,
    private val libraryStore: LibraryStore,
    private val m3uService: M3uPlaylistService,
    private val playlistStore: PlaylistStore
) {
    suspend fun scanAllEnabled(autoImportDiscoveredM3u: Boolean = true) {
        val sources = sourceStore.sources.value.filter { it.enabled }
        libraryStore.setScanning(true)
        try {
            for (source in sources) {
                scanOne(source, autoImportDiscoveredM3u)
            }
        } finally {
            libraryStore.setScanning(false)
        }
    }

    suspend fun scanOne(source: MusicSource, autoImportDiscoveredM3u: Boolean = true) {
        when (source.kind) {
            MusicSourceKind.PHONE_MEDIASTORE -> {
                val tracks = deviceScanner.scanPhoneLibrary(publishDirectly = false)
                if (tracks.isEmpty() && sourceStore.tracksForSource(source.id).isNotEmpty()) {
                    // Permission blip / MediaStore hiccup — keep last good phone catalog.
                    sourceStore.recordScanFailure(
                        source.id,
                        "Phone scan returned empty — keeping previous library",
                        connected = true
                    )
                } else {
                    sourceStore.recordScanResult(source.id, tracks, emptyList())
                }
            }
            MusicSourceKind.PHONE_FOLDER,
            MusicSourceKind.USB_SSD,
            MusicSourceKind.SD_CARD,
            MusicSourceKind.OTHER -> {
                val outcome = safScanner.scanSource(source)
                if (autoImportDiscoveredM3u) {
                    importDiscovered(outcome.playlists)
                }
            }
        }
    }

    /**
     * User-selected phone music folder becomes the phone library.
     * Disables MediaStore (ringtones/notifications/etc.) and other phone folders
     * so the catalog only shows files under this tree.
     */
    suspend fun addPhoneFolder(treeUri: Uri, displayName: String?): MusicSource {
        val kind = MusicSourceStore.inferKindFromDisplayName(displayName)
        // Prefer PHONE_FOLDER when SAF root looks like internal storage.
        val resolvedKind = when (kind) {
            MusicSourceKind.USB_SSD, MusicSourceKind.SD_CARD -> kind
            else -> MusicSourceKind.PHONE_FOLDER
        }
        val source = sourceStore.upsertSafSource(
            treeUri = treeUri,
            kind = resolvedKind,
            label = displayName?.ifBlank { null } ?: MusicSourceStore.defaultLabel(resolvedKind),
            makePreferred = true
        )
        takePersistablePermission(treeUri)
        sourceStore.useExclusivePhoneFolder(source.id)
        scanOne(source)
        libraryStore.setStatus(
            "Phone folder active — ${source.label}. MediaStore (ringtones) off."
        )
        return source
    }

    suspend fun addUsbOrExternal(treeUri: Uri, displayName: String?): MusicSource {
        val kind = MusicSourceStore.inferKindFromDisplayName(displayName).let {
            if (it == MusicSourceKind.PHONE_FOLDER) MusicSourceKind.USB_SSD else it
        }
        val source = sourceStore.upsertSafSource(
            treeUri = treeUri,
            kind = kind,
            label = displayName?.ifBlank { null } ?: MusicSourceStore.defaultLabel(kind),
            makePreferred = false
        )
        takePersistablePermission(treeUri)
        scanOne(source)
        return source
    }

    private suspend fun importDiscovered(files: List<DiscoveredPlaylistFile>) = withContext(Dispatchers.IO) {
        val catalog = libraryStore.tracks.value
        for (file in files) {
            runCatching {
                val dir = file.relativePath.substringBeforeLast('/', missingDelimiterValue = "")
                val preview = m3uService.preview(file.uri, catalog, dir)
                m3uService.importConfirmed(preview)
            }
        }
    }

    private fun takePersistablePermission(treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
