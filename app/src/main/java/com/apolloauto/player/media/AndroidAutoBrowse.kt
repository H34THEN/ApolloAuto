package com.apolloauto.player.media

/**
 * Shallow Android Auto browse tree tuned for small landscape HUDs.
 * Counts stay in subtitles so root titles don't truncate.
 */
object AndroidAutoBrowse {
    data class Node(
        val id: String,
        val title: String,
        val subtitle: String? = null
    )

    fun rootNodes(
        favoritesMediaId: String,
        favoritesCount: Int,
        playlistCount: Int,
        trackCount: Int
    ): List<Node> = listOf(
        // Short titles — AA tabs truncate; keep counts in subtitles only.
        Node(favoritesMediaId, "Favorites", "$favoritesCount tracks"),
        Node(MediaIds.PLAYLISTS, "Playlists", "$playlistCount lists"),
        Node(MediaIds.ALL, "Songs", "$trackCount tracks"),
        Node(MediaIds.ARTISTS, "Artists"),
        Node(MediaIds.ALBUMS, "Albums"),
        Node(MediaIds.RECENTLY_ADDED, "New"),
        Node(MediaIds.RECENT, "Recent"),
        Node(MediaIds.MORE, "More")
    )

    fun moreNodes(folderCount: Int): List<Node> = listOf(
        Node(MediaIds.MOST_PLAYED, "Top Played"),
        Node(MediaIds.UNPLAYED, "Unplayed"),
        Node(MediaIds.FOLDERS, "Folders", "$folderCount folders"),
        Node(MediaIds.VISUALIZERS, "Phone Viz", "Phone Immersive only")
    )

    fun isAutomotiveController(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("projection.gearhead") ||
            p.contains("android.car") ||
            p.contains("car.app") ||
            p.contains("automotive")
    }
}
