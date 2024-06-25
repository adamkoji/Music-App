package com.example.musicapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicAppNavHost(context = this)
                }
            }
        }
    }
}

@Composable
fun MusicAppNavHost(context: Context) {
    val navController = rememberNavController()
    val musicPlayerController = remember { MusicPlayerController(context) }

    NavHost(navController = navController, startDestination = "playlists") {
        composable("playlists") {
            PlaylistPage { playlistId ->
                navController.navigate("songs/$playlistId")
            }
        }
        composable(
            "songs/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.IntType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
            SongsPage(
                playlistId = playlistId,
                onSongClick = { songName ->
                    navController.navigate("player/$songName")
                }
            )
        }
        composable(
            "player/{songName}",
            arguments = listOf(navArgument("songName") { type = NavType.StringType })
        ) { backStackEntry ->
            val songName = backStackEntry.arguments?.getString("songName") ?: ""

            var isPlaying by remember { mutableStateOf(false) }
            var currentPosition by remember { mutableStateOf(0L) }
            var duration by remember { mutableStateOf(0L) }

            LaunchedEffect(songName) {
                try {
                    musicPlayerController.setMediaItem("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                    musicPlayerController.prepare()
                    musicPlayerController.play()
                } catch (e: Exception) {
                    Log.e("MusicPlayerActivity", "Error setting up media: ${e.message}", e)
                }

                while (true) {
                    isPlaying = musicPlayerController.isPlaying()
                    currentPosition = musicPlayerController.getCurrentPosition()
                    duration = musicPlayerController.getDuration()
                    delay(100) // Update every 100ms
                }
            }

            MusicPlayerUI(
                songName = songName,
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

@Composable
fun PlaylistPage(onPlaylistClick: (Int) -> Unit) {
    val playlists = listOf("Playlist 1", "Playlist 2", "Playlist 3") // Example playlists

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Playlists",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            items(playlists) { playlist ->
                PlaylistItem(name = playlist) {
                    onPlaylistClick(playlists.indexOf(playlist))
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SongsPage(playlistId: Int, onSongClick: (String) -> Unit) {
    val songs = listOf("Song 1", "Song 2", "Song 3") // Example songs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Playlist $playlistId Songs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            items(songs) { song ->
                SongItem(name = song) {
                    onSongClick(song)
                }
            }
        }
    }
}

@Composable
fun SongItem(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun MusicPlayerUI(
    songName: String,
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
                text = songName,
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
            songName = "Sample Song",
            isPlaying = false,
            currentPosition = 60000L,
            duration = 180000L,
            onPlayPauseClick = { },
            onSeek = { }
        )
    }
}
