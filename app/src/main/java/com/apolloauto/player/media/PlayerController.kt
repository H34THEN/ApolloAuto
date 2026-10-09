package com.apolloauto.player.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.apolloauto.player.data.Track
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatModeUi {
    OFF,
    ALL,
    ONE
}

data class PlaybackUiState(
    val connected: Boolean = false,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: RepeatModeUi = RepeatModeUi.OFF,
    val mediaId: String = "",
    val title: String = "APOLLO STANDING BY",
    val artist: String = "Scan phone music or link USB later",
    val album: String = "",
    val artworkUri: Uri? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class PlayerController(private val context: Context) {
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun connect() {
        if (controller != null) return
        val token = SessionToken(
            context,
            ComponentName(context, ApolloPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val ctrl = future.get()
            controller = ctrl
            ctrl.addListener(listener)
            publish(ctrl)
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = PlaybackUiState()
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val ctrl = controller ?: return
        if (tracks.isEmpty()) return
        val items = tracks.map { it.toMediaItem() }
        ctrl.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        ctrl.prepare()
        ctrl.play()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val ctrl = controller ?: return
        ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
        publish(ctrl)
    }

    fun cycleRepeat() {
        val ctrl = controller ?: return
        ctrl.repeatMode = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publish(ctrl)
    }

    fun pollPosition() {
        val ctrl = controller ?: return
        _state.value = _state.value.copy(
            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
            durationMs = ctrl.duration.coerceAtLeast(0L)
        )
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    private fun publish(player: Player) {
        val meta = player.mediaMetadata
        _state.value = PlaybackUiState(
            connected = true,
            playing = player.isPlaying,
            shuffle = player.shuffleModeEnabled,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatModeUi.ONE
                Player.REPEAT_MODE_ALL -> RepeatModeUi.ALL
                else -> RepeatModeUi.OFF
            },
            mediaId = player.currentMediaItem?.mediaId.orEmpty(),
            title = meta.title?.toString() ?: "APOLLO STANDING BY",
            artist = meta.artist?.toString() ?: "LIBRARY",
            album = meta.albumTitle?.toString().orEmpty(),
            artworkUri = meta.artworkUri ?: player.currentMediaItem?.mediaMetadata?.artworkUri,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L)
        )
    }

    companion object {
        fun Track.toMediaItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .setArtworkUri(artworkUri)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
    }
}
