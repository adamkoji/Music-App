package com.example.musicapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.ui.theme.MusicAppTheme
import kotlinx.coroutines.delay
import java.util.Locale
import android.content.Context

class MusicPlayerController(private val context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun setMediaItem(uri: String) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun prepare() {
        player.prepare()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.release()
    }

    fun getCurrentPosition(): Long {
        return player.currentPosition
    }

    fun getDuration(): Long {
        return player.duration
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }
}

class MusicPlayerActivity : ComponentActivity() {
    private lateinit var musicPlayerController: MusicPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        musicPlayerController = MusicPlayerController(context = this)

        try {
            musicPlayerController.setMediaItem("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
            musicPlayerController.prepare()
            musicPlayerController.play()
        } catch (e: Exception) {
            Log.e("MusicPlayerActivity", "Error setting up media: ${e.message}", e)
        }

        setContent {
            MusicAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isPlaying by remember { mutableStateOf(false) }
                    var currentPosition by remember { mutableStateOf(0L) }
                    var duration by remember { mutableStateOf(0L) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            isPlaying = musicPlayerController.isPlaying()
                            currentPosition = musicPlayerController.getCurrentPosition()
                            duration = musicPlayerController.getDuration()
                            delay(100) // Update every 100ms
                        }
                    }

                    MusicPlayerUI(
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        onPlayPauseClick = {
                            if (isPlaying) musicPlayerController.pause() else musicPlayerController.play()
                        },
                        onSeek = { newPosition ->
                            musicPlayerController.seekTo(newPosition)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerController.release()
    }
}

@Composable
fun MusicPlayerUI(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Image(
                painter = painterResource(id = R.drawable.img_8),
                contentDescription = null,
                modifier = Modifier
                    .size(400.dp)
                    .padding(24.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Sample Track",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SeekBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek
                )
                Spacer(modifier = Modifier.height(16.dp))
                PlayPauseButton(isPlaying, onPlayPauseClick)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PlayPauseButton(isPlaying: Boolean, onPlayPauseClick: () -> Unit) {
    Crossfade(targetState = isPlaying, animationSpec = tween(durationMillis = 300)) { playing ->
        FloatingActionButton(
            onClick = onPlayPauseClick,
            containerColor = Color.Red,
            contentColor = Color.Black,
            modifier = Modifier
                .size(75.dp)
                .padding(16.dp)
        ) {
            if (playing) {
                Icon(Icons.Filled.Pause, contentDescription = "Pause")
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
            }
        }
    }
}

@Composable
fun SeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildString {
                append(formatTime(currentPosition))
                append(" / ")
                append(formatTime(duration.coerceAtLeast(1)))
            },
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { newValue ->
                onSeek(newValue.toLong())
            },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red,
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
        )
    }
}

fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun PreviewMusicPlayerUI() {
    MusicAppTheme {
        MusicPlayerUI(
            isPlaying = false,
            currentPosition = 60000L,
            duration = 180000L,
            onPlayPauseClick = { },
            onSeek = { }
        )
    }
}
