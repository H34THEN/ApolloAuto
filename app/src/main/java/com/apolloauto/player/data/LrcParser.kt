package com.apolloauto.player.data

/**
 * Minimal LRC lyric parser for timed karaoke display.
 * Supports `[mm:ss.xx]text` and multi-tag lines.
 */
data class LrcLine(
    val timeMs: Long,
    val text: String
)

data class LrcDocument(
    val lines: List<LrcLine>
) {
    fun activeIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var lo = 0
        var hi = lines.lastIndex
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return best
    }
}

object LrcParser {
    private val TAG = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val META = Regex("""^\[[a-zA-Z]+:.*]$""")

    fun parse(raw: String): LrcDocument {
        val out = ArrayList<LrcLine>(64)
        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || META.matches(trimmed)) continue
            val tags = TAG.findAll(trimmed).toList()
            if (tags.isEmpty()) continue
            val text = trimmed.substring(tags.last().range.last + 1).trim()
            if (text.isEmpty()) continue
            for (tag in tags) {
                val min = tag.groupValues[1].toLongOrNull() ?: continue
                val sec = tag.groupValues[2].toLongOrNull() ?: continue
                val frac = tag.groupValues[3]
                val ms = when {
                    frac.isNullOrEmpty() -> 0L
                    frac.length == 1 -> frac.toLong() * 100L
                    frac.length == 2 -> frac.toLong() * 10L
                    else -> frac.take(3).toLong()
                }
                out += LrcLine(timeMs = min * 60_000L + sec * 1_000L + ms, text = text)
            }
        }
        out.sortBy { it.timeMs }
        return LrcDocument(out)
    }
}
