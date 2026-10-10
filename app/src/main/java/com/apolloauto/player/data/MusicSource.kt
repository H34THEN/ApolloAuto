package com.apolloauto.player.data

/**
 * Configurable library root. Multiple sources may be enabled at once;
 * their tracks merge into [LibraryStore].
 */
enum class MusicSourceKind {
    PHONE_MEDIASTORE,
    PHONE_FOLDER,
    USB_SSD,
    SD_CARD,
    OTHER
}

data class MusicSource(
    val id: String,
    val kind: MusicSourceKind,
    val label: String,
    /** Persistable tree URI for SAF roots; null for MediaStore phone library. */
    val treeUri: String? = null,
    val enabled: Boolean = true,
    val preferred: Boolean = false,
    val trackCount: Int = 0,
    val playlistCount: Int = 0,
    val lastScanMs: Long = 0L,
    val lastError: String? = null,
    val connected: Boolean = true
) {
    val kindLabel: String
        get() = when (kind) {
            MusicSourceKind.PHONE_MEDIASTORE -> "PHONE STORAGE"
            MusicSourceKind.PHONE_FOLDER -> "PHONE FOLDER"
            MusicSourceKind.USB_SSD -> "USB SSD"
            MusicSourceKind.SD_CARD -> "SD CARD"
            MusicSourceKind.OTHER -> "LIBRARY ROOT"
        }
}

enum class LibraryFilter {
    ALL,
    PHONE,
    USB,
    SD,
    SOURCE
}

data class LibraryFilterState(
    val mode: LibraryFilter = LibraryFilter.ALL,
    val sourceId: String? = null
)
