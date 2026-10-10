package com.apolloauto.player.data

/**
 * Presentation-only metadata helpers. Never mutates stored Track / MediaMetadata values.
 */
object TrackMetadataFormatter {
    const val UNKNOWN_ARTIST = "Unknown Artist"
    const val UNKNOWN_ALBUM = "Unknown Album"

    fun displayTitle(title: String?): String =
        title?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown"

    fun displayArtist(artist: String?): String {
        val a = normalize(artist)
        if (a.isEmpty() || isUnknownArtist(a)) return UNKNOWN_ARTIST
        return a
    }

    fun displayAlbum(album: String?): String? {
        val a = normalize(album)
        if (a.isEmpty() || isUnknownAlbum(a)) return null
        return a
    }

    /**
     * Single subtitle line for compact rows: artist only.
     */
    @Suppress("UNUSED_PARAMETER")
    fun displaySubtitle(artist: String?, album: String? = null): String = displayArtist(artist)

    /**
     * Optional secondary line (album) when it differs from artist and is meaningful.
     */
    fun displaySecondaryMetadata(artist: String?, album: String?): String? {
        val artistLine = displayArtist(artist)
        val albumLine = displayAlbum(album) ?: return null
        if (equalsIgnoreCaseNormalized(artistLine, albumLine)) return null
        if (isUnknownArtist(artistLine) && equalsIgnoreCaseNormalized(albumLine, UNKNOWN_ALBUM)) {
            return null
        }
        return albumLine
    }

    /**
     * Lines for Now Playing style blocks: title, artist, optional album.
     * Never emits two identical consecutive metadata lines.
     */
    fun displayLines(title: String?, artist: String?, album: String?): List<String> {
        val lines = mutableListOf(displayTitle(title), displayArtist(artist))
        displaySecondaryMetadata(artist, album)?.let { lines += it }
        return lines
    }

    fun equalsIgnoreCaseNormalized(a: String?, b: String?): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        return na.equals(nb, ignoreCase = true)
    }

    private fun normalize(value: String?): String =
        value?.trim()?.replace(Regex("\\s+"), " ").orEmpty()

    private fun isUnknownArtist(value: String): Boolean {
        val v = value.lowercase()
        return v == "unknown artist" || v == "<unknown>" || v == "unknown" || v == "library"
    }

    private fun isUnknownAlbum(value: String): Boolean {
        val v = value.lowercase()
        return v == "unknown album" || v == "<unknown>" || v == "unknown"
    }
}

fun Track.displayTitle(): String = TrackMetadataFormatter.displayTitle(title)
fun Track.displayArtist(): String = TrackMetadataFormatter.displayArtist(artist)
fun Track.displaySecondary(): String? = TrackMetadataFormatter.displaySecondaryMetadata(artist, album)
fun Track.displaySubtitle(): String = TrackMetadataFormatter.displaySubtitle(artist, album)
