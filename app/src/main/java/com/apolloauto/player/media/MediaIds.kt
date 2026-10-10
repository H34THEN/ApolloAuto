package com.apolloauto.player.media

object MediaIds {
    const val ROOT = "apollo_root"
    const val FOLDER_PREFIX = "folder:"
    const val TRACK_PREFIX = "track:"
    const val ALL = "all_tracks"
    const val FAVORITES = "favorites"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_PREFIX = "playlist:"
    const val ARTISTS = "artists"
    const val ARTIST_PREFIX = "artist:"
    const val ALBUMS = "albums"
    const val ALBUM_PREFIX = "album:"
    const val RECENT = "recent"
    const val MOST_PLAYED = "most_played"
    const val RECENTLY_ADDED = "recently_added"
    const val UNPLAYED = "unplayed"
    const val FOLDERS = "folders"
    const val VISUALIZERS = "visualizers"
    const val VIZ_PREFIX = "viz:"
    /** Secondary browse categories — keeps the AA root shallow on small HUDs. */
    const val MORE = "more"

    const val CMD_NEXT_VISUAL = "apollo.next_visual"
    const val CMD_TOGGLE_DRIVE = "apollo.toggle_drive"
    const val CMD_SAVE_CAR_FAV = "apollo.save_car_favorite"
}
