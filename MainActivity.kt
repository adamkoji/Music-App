package com.example.musicapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import com.example.musicapp.ui.theme.MusicAppTheme
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.media3.common.PlaybackException as ExoPlaybackException

class MusicPlayerActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize the player
            player = ExoPlayer.Builder(this).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: ExoPlaybackException) {
                        Log.e("MusicPlayerActivity", "Player error: ${error.message}")
                    }
                })
            }

            // Create a media item
            val mediaItem = MediaItem.fromUri("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            setContent {
                MusicAppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MusicPlayerUI(
                            player = player,
                            isPlaying = player.isPlaying,
                            onPlayPauseClick = {
                                if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerActivity", "Initialization error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@Composable
fun MusicPlayerUI(
    player: ExoPlayer? = null,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFEE0)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Image(
                painter = painterResource(id = R.drawable.img_1), // Replace with your music logo resource
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Sample Track",
                color = Color.Black,
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
                if (player != null) {
                    SeekBar(player = player)
                } else {
                    MockSeekBar()
                }
                Spacer(modifier = Modifier.height(16.dp))
                PlayPauseButton(isPlaying, onPlayPauseClick)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PlayPauseButton(isPlaying: Boolean, onPlayPauseClick: () -> Unit) {
    FloatingActionButton(
        onClick = onPlayPauseClick,
        containerColor = Color.Black,
        contentColor = Color.White,
        modifier = Modifier
            .size(75.dp)
            .padding(16.dp)
    ) {
        if (isPlaying) {
            Icon(Icons.Filled.Pause, contentDescription = "Pause")
        } else {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
        }
    }
}

@Composable
fun SeekBar(player: ExoPlayer) {
    var duration by remember { mutableFloatStateOf(player.duration.toFloat()) }
    var currentPosition by remember { mutableFloatStateOf(player.currentPosition.toFloat()) }

    LaunchedEffect(player) {
        while (true) {
            duration = player.duration.toFloat()
            currentPosition = player.currentPosition.toFloat()
            delay(1000) // Update every second
        }
    }

    if (duration > 0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildString {
                append(formatTime(currentPosition.toLong()))
                append(" / ")
                append(formatTime(duration.toLong()))
            },
            color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        println("currentPosition: $currentPosition")
        Slider(
            value = currentPosition,
            onValueChange = { newValue ->
                player.seekTo(newValue.toLong())
                currentPosition = newValue.coerceIn(0f, duration)
            },
            valueRange = 0f..duration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun MockSeekBar() {
    val duration by remember { mutableFloatStateOf(300000f) } // Mock duration of 5 minutes
    var currentPosition by remember { mutableFloatStateOf(150000f) } // Mock current position of 2.5 minutes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildString {
                append(formatTime(currentPosition.toLong()))
                append(" / ")
                append(formatTime(duration.toLong()))
            },
            color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = currentPosition,
            onValueChange = { newValue ->
                currentPosition = newValue
            },
            valueRange = 0f..duration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
            onPlayPauseClick = { /* No-op for preview */ }
        )
    }
}
