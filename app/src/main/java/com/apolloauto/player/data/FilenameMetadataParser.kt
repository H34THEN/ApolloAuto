package com.apolloauto.player.data

/**
 * Parses common downloaded/ripped filename patterns:
 * `Artist - Song Title.ext`
 */
object FilenameMetadataParser {
    private val separators = listOf(" - ", " – ", " — ", " − ")

    data class Parsed(val artist: String, val title: String)

    fun parseBaseName(baseName: String): Parsed? {
        val raw = baseName.trim()
        if (raw.isEmpty()) return null
        for (sep in separators) {
            val idx = raw.indexOf(sep)
            if (idx <= 0) continue
            val artist = raw.substring(0, idx).trim()
            val title = raw.substring(idx + sep.length).trim()
            if (artist.isNotEmpty() && title.isNotEmpty()) {
                return Parsed(artist = artist, title = title)
            }
        }
        return null
    }

    /**
     * Prefer embedded tags when they look real; otherwise parse filename / combined title.
     */
    fun resolve(
        embeddedTitle: String?,
        embeddedArtist: String?,
        displayName: String?,
        folderFallback: String = "Unknown Album"
    ): Triple<String, String, String> {
        val fileBase = displayName
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
        val titleIn = embeddedTitle?.trim().orEmpty()
        val artistIn = embeddedArtist?.trim().orEmpty()
        val artistUnknown = artistIn.isEmpty() ||
            artistIn.equals("<unknown>", true) ||
            artistIn.equals("unknown", true) ||
            artistIn.equals("unknown artist", true) ||
            artistIn.equals("library", true)

        val fromFile = fileBase?.let { parseBaseName(it) }
        val fromTitle = if (titleIn.isNotEmpty()) parseBaseName(titleIn) else null

        // Strong filename pattern wins when artist tag is missing/useless or title still embeds artist.
        val parsed = when {
            fromFile != null && (artistUnknown || titleIn == fileBase || titleIn.contains(" - ")) -> fromFile
            fromTitle != null && (artistUnknown || titleIn == fromTitle.artist + " - " + fromTitle.title) -> fromTitle
            fromFile != null && artistUnknown -> fromFile
            else -> null
        }

        if (parsed != null) {
            val album = folderFallback.takeIf { it.isNotBlank() } ?: "Unknown Album"
            return Triple(parsed.title, parsed.artist, album)
        }

        val title = titleIn.ifBlank { fileBase ?: "Unknown" }
        val artist = if (artistUnknown) TrackMetadataFormatter.UNKNOWN_ARTIST else artistIn
        val album = folderFallback.takeIf { it.isNotBlank() } ?: "Unknown Album"
        return Triple(title, artist, album)
    }
}
