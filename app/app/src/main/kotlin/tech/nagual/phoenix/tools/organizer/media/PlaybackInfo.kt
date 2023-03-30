package tech.nagual.phoenix.tools.organizer.media

data class PlaybackInfo(
    val position: Int = 0,
    val duration: Int = 0,
    val isPlaying: Boolean = false,
    val isReleased: Boolean = false,
)
