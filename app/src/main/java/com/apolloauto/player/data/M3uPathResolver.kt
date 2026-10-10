package com.apolloauto.player.data

import java.util.Locale

enum class M3uResolveStatus {
    RESOLVED,
    MISSING,
    AMBIGUOUS
}

data class M3uCatalogItem(
    val id: String,
    val relativePath: String,
    val title: String,
    val artist: String = ""
)

data class M3uResolvedEntry(
    val original: M3uEntry,
    val status: M3uResolveStatus,
    val trackId: String? = null,
    val track: Track? = null,
    val candidateIds: List<String> = emptyList(),
    val candidates: List<Track> = emptyList()
)

data class M3uResolveResult(
    val entries: List<M3uResolvedEntry>,
    val resolvedCount: Int,
    val missingCount: Int,
    val ambiguousCount: Int
)

object M3uPathResolver {
    fun resolve(
        playlist: M3uPlaylist,
        catalog: List<Track>,
        playlistRelativeDir: String = ""
    ): M3uResolveResult {
        val items = catalog.map {
            M3uCatalogItem(it.id, it.relativePath, it.title, it.artist)
        }
        val byId = catalog.associateBy { it.id }
        val core = resolveItems(playlist, items, playlistRelativeDir)
        return core.copy(
            entries = core.entries.map { e ->
                e.copy(
                    track = e.trackId?.let { byId[it] },
                    candidates = e.candidateIds.mapNotNull { byId[it] }
                )
            }
        )
    }

    /**
     * Ranked resolution against an authorized catalog. Never invents a pick when ambiguous.
     * JVM-safe (no Android Uri) for unit tests.
     */
    fun resolveItems(
        playlist: M3uPlaylist,
        catalog: List<M3uCatalogItem>,
        playlistRelativeDir: String = ""
    ): M3uResolveResult {
        val byRel = catalog
            .filter { it.relativePath.isNotBlank() }
            .groupBy { normalizePath(it.relativePath) }
        val byFile = catalog.groupBy { fileName(it.relativePath.ifBlank { it.title }) }
        val baseDir = normalizePath(playlistRelativeDir)

        val out = playlist.entries.map { entry ->
            val normalized = normalizeEntry(entry.location)
            val fromPlaylistDir = if (baseDir.isNotBlank()) {
                normalizePath(join(baseDir, normalized))
            } else normalized
            exact(byRel, fromPlaylistDir)?.let {
                return@map M3uResolvedEntry(entry, M3uResolveStatus.RESOLVED, trackId = it.id)
            }
            exact(byRel, normalized)?.let {
                return@map M3uResolvedEntry(entry, M3uResolveStatus.RESOLVED, trackId = it.id)
            }
            val suffix = stripDrive(normalized)
            val suffixHits = catalog.filter {
                val rel = normalizePath(it.relativePath)
                rel.isNotBlank() && (rel == suffix || rel.endsWith("/$suffix") || suffix.endsWith("/$rel"))
            }
            when (suffixHits.size) {
                1 -> return@map M3uResolvedEntry(
                    entry, M3uResolveStatus.RESOLVED, trackId = suffixHits[0].id
                )
                in 2..Int.MAX_VALUE -> return@map M3uResolvedEntry(
                    entry,
                    M3uResolveStatus.AMBIGUOUS,
                    candidateIds = suffixHits.map { it.id }
                )
            }
            val name = fileName(suffix)
            val nameHits = byFile[name].orEmpty()
            when {
                nameHits.size == 1 -> M3uResolvedEntry(
                    entry, M3uResolveStatus.RESOLVED, trackId = nameHits[0].id
                )
                nameHits.size > 1 -> {
                    val titled = entry.title?.let { t ->
                        nameHits.filter {
                            it.title.equals(t, true) ||
                                "${it.artist} - ${it.title}".equals(t, true)
                        }
                    }.orEmpty()
                    when (titled.size) {
                        1 -> M3uResolvedEntry(
                            entry, M3uResolveStatus.RESOLVED, trackId = titled[0].id
                        )
                        else -> M3uResolvedEntry(
                            entry,
                            M3uResolveStatus.AMBIGUOUS,
                            candidateIds = nameHits.map { it.id }
                        )
                    }
                }
                else -> M3uResolvedEntry(entry, M3uResolveStatus.MISSING)
            }
        }
        return M3uResolveResult(
            entries = out,
            resolvedCount = out.count { it.status == M3uResolveStatus.RESOLVED },
            missingCount = out.count { it.status == M3uResolveStatus.MISSING },
            ambiguousCount = out.count { it.status == M3uResolveStatus.AMBIGUOUS }
        )
    }

    fun normalizeEntry(raw: String): String {
        var s = raw.trim().trim('"')
        s = s.replace('\\', '/')
        // file:/// or content URIs left as-is for exact uri match elsewhere
        if (s.startsWith("file:", ignoreCase = true)) {
            s = s.removePrefix("file://").removePrefix("file:")
        }
        return normalizePath(s)
    }

    fun normalizePath(path: String): String {
        val parts = ArrayList<String>()
        for (seg in path.replace('\\', '/').split('/')) {
            when {
                seg.isEmpty() || seg == "." -> Unit
                seg == ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts += seg
            }
        }
        return parts.joinToString("/").lowercase(Locale.US)
    }

    private fun stripDrive(path: String): String {
        var s = path
        // D:/Music/... or /D:/Music
        if (s.length >= 2 && s[1] == ':') s = s.substring(2)
        while (s.startsWith("/")) s = s.removePrefix("/")
        return normalizePath(s)
    }

    private fun join(base: String, rel: String): String {
        if (rel.startsWith("/")) return rel.removePrefix("/")
        return normalizePath("$base/$rel")
    }

    private fun fileName(path: String): String =
        path.replace('\\', '/').substringAfterLast('/').lowercase(Locale.US)

    private fun exact(map: Map<String, List<M3uCatalogItem>>, key: String): M3uCatalogItem? {
        val hits = map[normalizePath(key)].orEmpty()
        return hits.singleOrNull()
    }
}
