package com.apolloauto.player.data

import java.nio.charset.Charset

data class M3uEntry(
    val location: String,
    val title: String? = null,
    val durationSec: Int? = null
)

data class M3uPlaylist(
    val extended: Boolean,
    val entries: List<M3uEntry>,
    val encodingName: String
)

object M3uParser {
    fun parse(bytes: ByteArray, preferUtf8: Boolean = true): M3uPlaylist {
        val text = decode(bytes, preferUtf8)
        return parseText(text.first, text.second)
    }

    fun parseText(text: String, encodingName: String = Charsets.UTF_8.name()): M3uPlaylist {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var extended = false
        var pendingTitle: String? = null
        var pendingDuration: Int? = null
        val entries = ArrayList<M3uEntry>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#")) {
                when {
                    line.equals("#EXTM3U", ignoreCase = true) -> extended = true
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        val body = line.substringAfter(':', missingDelimiterValue = "")
                        val durationPart = body.substringBefore(',')
                        val titlePart = body.substringAfter(',', missingDelimiterValue = "").trim()
                        pendingDuration = durationPart.trim().toIntOrNull()
                        pendingTitle = titlePart.takeIf { it.isNotBlank() }
                    }
                    else -> Unit // comment
                }
                continue
            }
            entries += M3uEntry(
                location = line,
                title = pendingTitle,
                durationSec = pendingDuration
            )
            pendingTitle = null
            pendingDuration = null
        }
        return M3uPlaylist(extended = extended, entries = entries, encodingName = encodingName)
    }

    private fun decode(bytes: ByteArray, preferUtf8: Boolean): Pair<String, String> {
        if (preferUtf8) {
            val utf8 = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
            if (utf8 != null && !utf8.contains('\uFFFD')) {
                return utf8 to Charsets.UTF_8.name()
            }
        }
        val fallbacks = listOf(
            Charsets.UTF_8,
            Charset.forName("ISO-8859-1"),
            Charset.forName("windows-1252")
        )
        for (cs in fallbacks) {
            val s = runCatching { String(bytes, cs) }.getOrNull() ?: continue
            if (!s.contains('\uFFFD') || cs == Charsets.UTF_8) {
                return s to cs.name()
            }
        }
        return String(bytes, Charsets.UTF_8) to Charsets.UTF_8.name()
    }
}
