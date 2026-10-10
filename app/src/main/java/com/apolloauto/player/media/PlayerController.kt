package com.apolloauto.player.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.data.Track
import com.apolloauto.player.data.TrackMetadataFormatter
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
    val durationMs: Long = 0L,
    /** Effective playback order (matches Media3 timeline after stable shuffle). */
    val queue: List<QueueEntry> = emptyList(),
    val queueIndex: Int = 0
)

class PlayerController(private val context: Context) {
    private var controller: MediaController? = null
    private val stableQueue: StablePlaybackQueue
        get() = ApolloApp.instance.stablePlaybackQueue
    private var applyingQueue = false

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
            // Prefer our stable shuffle projection over Media3's per-window reshuffle.
            if (ctrl.shuffleModeEnabled) {
                ctrl.shuffleModeEnabled = false
            }
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
        val apply = stableQueue.replaceQueue(items, startIndex.coerceIn(0, items.lastIndex))
        applyingQueue = true
        try {
            ctrl.shuffleModeEnabled = false
            ctrl.setMediaItems(apply.items, apply.startIndex, 0L)
            ctrl.prepare()
            ctrl.play()
        } finally {
            applyingQueue = false
        }
        publish(ctrl)
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun skipNext() {
        // Advances within the existing Media3 timeline — does not rebuild the queue.
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /** Jump within the current stable timeline without rebuilding the queue. */
    fun seekToQueueIndex(index: Int) {
        val ctrl = controller ?: return
        if (index < 0 || index >= ctrl.mediaItemCount) return
        ctrl.seekTo(index, 0L)
    }

    fun toggleShuffle() {
        val ctrl = controller ?: return
        val items = currentItems(ctrl)
        if (items.isEmpty()) return
        val apply = stableQueue.toggleShuffle(items, ctrl.currentMediaItemIndex) ?: return
        applyQueueSilently(ctrl, apply)
    }

    /** Reshuffle upcoming tracks; keeps shuffle ON and does not interrupt the current track. */
    fun remixShuffle() {
        val ctrl = controller ?: return
        val items = currentItems(ctrl)
        if (items.isEmpty()) return
        val apply = stableQueue.remix(items, ctrl.currentMediaItemIndex) ?: return
        applyQueueSilently(ctrl, apply)
    }

    private fun applyQueueSilently(ctrl: MediaController, apply: StablePlaybackQueue.QueueApply) {
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        val playWhenReady = ctrl.playWhenReady
        applyingQueue = true
        try {
            ctrl.shuffleModeEnabled = false
            ctrl.setMediaItems(apply.items, apply.startIndex, pos)
            // Avoid prepare()+play() restart blip when already buffered.
            if (ctrl.playbackState == Player.STATE_IDLE) {
                ctrl.prepare()
            }
            ctrl.playWhenReady = playWhenReady
        } finally {
            applyingQueue = false
        }
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
            if (applyingQueue) {
                publish(player)
                return
            }
            // If a remote controller (AA / system) enables Media3 shuffle, convert once to stable order.
            if (player.shuffleModeEnabled) {
                val items = currentItems(player)
                val apply = stableQueue.syncFromPlayerShuffleFlag(true, items, player.currentMediaItemIndex)
                if (apply != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    applyingQueue = true
                    try {
                        player.shuffleModeEnabled = false
                        player.setMediaItems(apply.items, apply.startIndex, pos)
                    } finally {
                        applyingQueue = false
                    }
                } else {
                    player.shuffleModeEnabled = false
                }
            }
            publish(player)
        }
    }

    private fun publish(player: Player) {
        val meta = player.mediaMetadata
        val rawArtist = meta.artist?.toString()
        val rawAlbum = meta.albumTitle?.toString()
        val queue = stableQueue.snapshot(player)
        _state.value = PlaybackUiState(
            connected = true,
            playing = player.isPlaying,
            shuffle = stableQueue.shuffleEnabled,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatModeUi.ONE
                Player.REPEAT_MODE_ALL -> RepeatModeUi.ALL
                else -> RepeatModeUi.OFF
            },
            mediaId = player.currentMediaItem?.mediaId.orEmpty(),
            title = TrackMetadataFormatter.displayTitle(meta.title?.toString()),
            // Preserve raw session metadata for AA; UI uses formatter for duplicate suppression.
            artist = rawArtist ?: "LIBRARY",
            album = rawAlbum.orEmpty(),
            artworkUri = meta.artworkUri ?: player.currentMediaItem?.mediaMetadata?.artworkUri,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            queue = queue,
            queueIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        )
    }

    private fun currentItems(player: Player): List<MediaItem> {
        val count = player.mediaItemCount
        if (count <= 0) return emptyList()
        return (0 until count).map { player.getMediaItemAt(it) }
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
