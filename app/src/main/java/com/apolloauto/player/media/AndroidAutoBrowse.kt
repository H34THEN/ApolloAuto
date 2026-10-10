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
        Node(favoritesMediaId, "Car Favorites", "$favoritesCount tracks"),
        Node(MediaIds.PLAYLISTS, "Playlists", "$playlistCount lists"),
        Node(MediaIds.ALL, "All Tracks", "$trackCount tracks"),
        Node(MediaIds.ARTISTS, "Artists"),
        Node(MediaIds.ALBUMS, "Albums"),
        Node(MediaIds.RECENTLY_ADDED, "Recently Added"),
        Node(MediaIds.RECENT, "Recently Played"),
        Node(MediaIds.MORE, "More", "Most played · folders · phone viz")
    )

    fun moreNodes(folderCount: Int): List<Node> = listOf(
        Node(MediaIds.MOST_PLAYED, "Most Played"),
        Node(MediaIds.UNPLAYED, "Unplayed"),
        Node(MediaIds.FOLDERS, "Folders", "$folderCount folders"),
        Node(MediaIds.VISUALIZERS, "Phone Visualizers", "Affects phone Immersive only")
    )

    fun isAutomotiveController(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("projection.gearhead") ||
            p.contains("android.car") ||
            p.contains("car.app") ||
            p.contains("automotive")
    }
}
