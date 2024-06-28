package com.example.musicapp

import android.content.Context
import android.net.Uri
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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

class MusicPlayerController(private val context: Context) {
    private var player: ExoPlayer? = null

    private fun ensurePlayerInitialized() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
    }

    fun setMediaItem(uri: String) {
        ensurePlayerInitialized()
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun prepare() {
        player?.prepare()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun play() {
        Log.d("MusicPlayerController", "${player == null} play")
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        player?.release()
        player = null
    }

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0L

    fun getDuration(): Long = player?.duration ?: 0L

    fun isPlaying(): Boolean = player?.isPlaying ?: false
}

data class Song(val id: Int, val name: String, val url: String)

class SharedViewModel : ViewModel() {
    private val _playlist = mutableStateListOf<Song>()
    val playlist: List<Song> = _playlist

    fun setPlaylist(songs: List<Song>) {
        _playlist.clear()
        _playlist.addAll(songs)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sharedViewModel: SharedViewModel = viewModel()
            MusicAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicAppNavHost(context = this, sharedViewModel = sharedViewModel)
                }
            }
        }
    }
}

@Composable
fun MusicAppNavHost(context: Context, sharedViewModel: SharedViewModel) {
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
                onSongClick = { song ->
                    navController.navigate("player/${song.id}")
                },
                sharedViewModel = sharedViewModel
            )
        }
        composable(
            "player/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.IntType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getInt("songId") ?: 0
            val currentSong = sharedViewModel.playlist.find { it.id == songId }
            println("current song log $currentSong")

            if (currentSong != null) {
                val currentIndex = sharedViewModel.playlist.indexOf(currentSong)
                println("current song log $currentIndex")

                MusicPlayerScreen(
                    song = currentSong,
                    currentIndex = currentIndex,
                    playlistSize = sharedViewModel.playlist.size,
                    musicPlayerController = musicPlayerController,
                    onNextSong = {
                        val nextIndex = (currentIndex + 1) % sharedViewModel.playlist.size
                        val nextSong = sharedViewModel.playlist[nextIndex]
                        navController.navigate("player/${nextSong.id}")
                    },
                    onPreviousSong = {
                        val previousIndex = (currentIndex - 1 + sharedViewModel.playlist.size) % sharedViewModel.playlist.size
                        val previousSong = sharedViewModel.playlist[previousIndex]
                        navController.navigate("player/${previousSong.id}")
                    }
                )
            } else {
                Text("Song not found")
            }
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
fun SongsPage(playlistId: Int, onSongClick: (Song) -> Unit, sharedViewModel: SharedViewModel) {
    val songs = listOf(
        Song(1, "Song 1", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        Song(2, "Song 2", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        Song(3, "Song 3", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
    )

    // Set the playlist in the SharedViewModel
    LaunchedEffect(songs) {
        sharedViewModel.setPlaylist(songs)
    }

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
                SongItem(song = song) {
                    onSongClick(song)
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = song.name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
@Composable
fun MusicPlayerScreen(
    song: Song,
    currentIndex: Int,
    playlistSize: Int,
    musicPlayerController: MusicPlayerController,
    onNextSong: () -> Unit,
    onPreviousSong: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    val songId = song.id


    LaunchedEffect(songId) {
        println("song id on Music player screen $songId")

        musicPlayerController.release()
        try {
            Log.d("MusicPlayerScreen", "Setting up media item for song: ${song.name}")
            musicPlayerController.setMediaItem(song.url)
            musicPlayerController.prepare()
            musicPlayerController.play()

            delay(100)
        } catch (e: Exception) {
            Log.e("MusicPlayerScreen", "Error setting up media: ${e.message}", e)
        }

        while (true) {

            isPlaying = musicPlayerController.isPlaying()
//            if (!isPlaying) {
//                Log.e("MusicPlayerScreen", "within if block ${song.name}")
//
//                musicPlayerController.play()
//                delay(100)
//            }
            currentPosition = musicPlayerController.getCurrentPosition()
            duration = musicPlayerController.getDuration()
            delay(100)
        }
    }

//    DisposableEffect(Unit) {
//        onDispose {
//            Log.d("MusicPlayerScreen", "Disposing MusicPlayerController")
//            musicPlayerController.release()
//        }
//    }

    MusicPlayerUI(
        songName = song.name,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        currentIndex = currentIndex,
        playlistSize = playlistSize,
        onPlayPauseClick = {
            if (isPlaying) musicPlayerController.pause() else musicPlayerController.play()
        },
        onSeek = { newPosition ->
            musicPlayerController.seekTo(newPosition)
        },
        onNextSong = {
            Log.d("MusicPlayerScreen", "Next song clicked")
            onNextSong()
        },
        onPreviousSong = {
            Log.d("MusicPlayerScreen", "Previous song clicked")
            onPreviousSong()
        }
    )
}

@Composable
fun MusicPlayerUI(
    songName: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    currentIndex: Int,
    playlistSize: Int,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onNextSong: () -> Unit,
    onPreviousSong: () -> Unit
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
            Text(
                text = "Song ${currentIndex + 1} of $playlistSize",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
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
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onPreviousSong) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White
                        )
                    }
                    PlayPauseButton(isPlaying, onPlayPauseClick)
                    IconButton(onClick = onNextSong) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
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
                .size(90.dp)
                .padding(16.dp)
        ) {
            if (playing) {
                Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(45.dp))
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(45.dp))
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
            currentIndex = 0,
            playlistSize = 5,
            onPlayPauseClick = { },
            onSeek = { },
            onNextSong = { },
            onPreviousSong = { }
        )
    }
}
