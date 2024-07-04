package com.example.musicapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.example.musicapp.ui.theme.MusicAppTheme
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow


import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.List

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
        player?.prepare()
    }

    fun prepare() {
        player?.prepare()
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun release() {
        player?.release()
        player = null
    }

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0L

    fun getDuration(): Long = player?.duration ?: 0L

    fun isPlaying(): Boolean = player?.isPlaying ?: false
}

data class Song(
    val id: Int,
    val name: String,
    val artist: String,
    val url: String,
    val imageRes: Int
)

class SharedViewModel : ViewModel() {
    private val _playlist = mutableStateListOf<Song>()
    val playlist: List<Song> = _playlist

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableStateOf(0L)
    val currentPosition: State<Long> = _currentPosition

    private val _duration = mutableStateOf(0L)
    val duration: State<Long> = _duration

    private val _recentlyPlayed = mutableStateListOf<Song>()
    val recentlyPlayed: List<Song> = _recentlyPlayed

    fun addToRecentlyPlayed(song: Song) {
        _recentlyPlayed.remove(song)
        _recentlyPlayed.add(0, song)
        if (_recentlyPlayed.size > 10) {
            _recentlyPlayed.removeAt(_recentlyPlayed.lastIndex)
        }
    }

    fun setPlaylist(songs: List<Song>) {
        _playlist.clear()
        _playlist.addAll(songs)
    }

    fun setCurrentSong(song: Song?) {
        _currentSong.value = song
        song?.let { addToRecentlyPlayed(it) }
    }

    fun setIsPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setCurrentPosition(position: Long) {
        _currentPosition.value = position
    }

    fun setDuration(duration: Long) {
        _duration.value = duration
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
    var currentPlaylistId by remember { mutableStateOf(0) }
    var isPlayerPageVisible by remember { mutableStateOf(false) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(currentRoute) {
        isPlayerPageVisible = currentRoute?.startsWith("player/") == true
    }

    Scaffold(
        bottomBar = {
            if (!isPlayerPageVisible) {
                Column {
                    MiniPlayer(
                        song = sharedViewModel.currentSong.value,
                        isPlaying = sharedViewModel.isPlaying.value,
                        currentPosition = sharedViewModel.currentPosition.value,
                        duration = sharedViewModel.duration.value,
                        onPlayPauseClick = {
                            if (sharedViewModel.isPlaying.value) musicPlayerController.pause() else musicPlayerController.play()
                            sharedViewModel.setIsPlaying(!sharedViewModel.isPlaying.value)
                        },
                        onNextSong = {
                            // Implement next song logic
                        },
                        onPreviousSong = {
                            // Implement previous song logic
                        },
                        onMiniPlayerClick = {
                            sharedViewModel.currentSong.value?.let { song ->
                                navController.navigate("player/${song.id}")
                            }
                        }
                    )
                    BottomNav(navController, currentRoute)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomePage(
                    navigateToPlaylist = { playlistId ->
                        navController.navigate("songs/$playlistId")
                    },
                    navigateToSong = { song ->
                        navController.navigate("player/${song.id}")
                    },
                    sharedViewModel = sharedViewModel
                )
            }
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
                currentPlaylistId = playlistId
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

                if (currentSong != null) {
                    val currentIndex = sharedViewModel.playlist.indexOf(currentSong)

                    MusicPlayerScreen(
                        song = currentSong,
                        currentIndex = currentIndex,
                        playlistSize = sharedViewModel.playlist.size,
                        musicPlayerController = musicPlayerController,
                        sharedViewModel = sharedViewModel,
                        onNextSong = {
                            val nextIndex = (currentIndex + 1) % sharedViewModel.playlist.size
                            val nextSong = sharedViewModel.playlist[nextIndex]
                            navController.navigate("player/${nextSong.id}")
                        },
                        onPreviousSong = {
                            val previousIndex = (currentIndex - 1 + sharedViewModel.playlist.size) % sharedViewModel.playlist.size
                            val previousSong = sharedViewModel.playlist[previousIndex]
                            navController.navigate("player/${previousSong.id}")
                        },
                        onBackPress = {
                            navController.navigate("songs/$currentPlaylistId") {
                                // Pop up to the songs page, removing all player pages from the back stack
                                popUpTo("songs/$currentPlaylistId") { inclusive = false }
                            }
                            navController.popBackStack()
                        }
                    )
                } else {
                    Text("Song not found")
                }
            }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Songs") },
            label = { Text("Songs") },
            selected = currentRoute == "songs/0",
            onClick = { navController.navigate("songs/0") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library") },
            label = { Text("Library") },
            selected = currentRoute == "playlists",
            onClick = { navController.navigate("playlists") }
        )
    }
}

@Composable
fun HomePage(
    navigateToPlaylist: (Int) -> Unit,
    navigateToSong: (Song) -> Unit,
    sharedViewModel: SharedViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Good morning",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Your Playlists section
        Text(
            text = "Your Playlists",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyRow {
            items(3) { index ->
                PlaylistItem(name = "Playlist ${index + 1}") {
                    navigateToPlaylist(index)
                }
            }
        }

        // Recently Played section
        Text(
            text = "Recently Played",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyRow {
            items(sharedViewModel.recentlyPlayed) { song ->
                RecentlyPlayedItem(song = song) {
                    navigateToSong(song)
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_6), // song image
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(
            text = song.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistPage(onPlaylistClick: (Int) -> Unit) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlists by remember { mutableStateOf(listOf("Playlist 1", "Playlist 2", "Playlist 3")) }

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
        Button(
            onClick = { showCreatePlaylistDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)

        ) {
            Icon(Icons.Default.Add, contentDescription = "Create playlist")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create playlist")
        }

        LazyColumn {
            items(playlists) { playlist ->
                PlaylistItem(name = playlist) {
                    onPlaylistClick(playlists.indexOf(playlist))
                }
            }
        }
    }
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Give your playlist a name.") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            playlists = playlists + newPlaylistName
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("NEXT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
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
        Song(1, "Song 1", "x","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",R.drawable.img),
        Song(2, "Song 2", "y","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",R.drawable.img_7),
        Song(3, "Song 3", "z","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",R.drawable.img_9),
    )
    
    LaunchedEffect(songs) {
        sharedViewModel.setPlaylist(songs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Text(
            text = "Playlist $playlistId",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = song.imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MusicPlayerScreen(
    song: Song,
    currentIndex: Int,
    playlistSize: Int,
    musicPlayerController: MusicPlayerController,
    sharedViewModel: SharedViewModel,
    onNextSong: () -> Unit,
    onPreviousSong: () -> Unit,
    onBackPress: () -> Unit
) {
    LaunchedEffect(song.id) {
        if (sharedViewModel.currentSong.value?.id != song.id) {
            sharedViewModel.setCurrentSong(song)
            musicPlayerController.setMediaItem(song.url)
            musicPlayerController.prepare()
            musicPlayerController.play()
            sharedViewModel.setIsPlaying(true)
        }

        while (true) {
            sharedViewModel.setIsPlaying(musicPlayerController.isPlaying())
            sharedViewModel.setCurrentPosition(musicPlayerController.getCurrentPosition())
            sharedViewModel.setDuration(musicPlayerController.getDuration())
            delay(100)
        }
    }

    BackHandler {
        onBackPress()
    }

    MusicPlayerUI(
        songName = song.name,
        isPlaying = sharedViewModel.isPlaying.value,
        currentPosition = sharedViewModel.currentPosition.value,
        duration = sharedViewModel.duration.value,
        onBackPress = onBackPress,
        currentIndex = currentIndex,
        playlistSize = playlistSize,
        onPlayPauseClick = {
            if (sharedViewModel.isPlaying.value) musicPlayerController.pause() else musicPlayerController.play()
            sharedViewModel.setIsPlaying(!sharedViewModel.isPlaying.value)
        },
        onSeek = { newPosition ->
            musicPlayerController.seekTo(newPosition)
        },
        onNextSong = onNextSong,
        onPreviousSong = onPreviousSong
    )
}

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onNextSong: () -> Unit,
    onPreviousSong: () -> Unit,
    onMiniPlayerClick: () -> Unit
) {
    if (song != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF282828))
                .clickable(onClick = onMiniPlayerClick)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_6),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(8.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = song.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { currentPosition.toFloat() / duration.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Red,
                    )
                }
                Row {
                    IconButton(onClick = onPreviousSong) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(35.dp))
                    }
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                    IconButton(onClick = onNextSong) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(35.dp))
                    }
                }
            }
        }
    }
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
    onPreviousSong: () -> Unit,
    onBackPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBackPress,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Now Playing",
                color = Color.Gray,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.height(30.dp))
            Image(
                painter = painterResource(id = R.drawable.img_10),
                contentDescription = null,
                modifier = Modifier
                    .size(350.dp)
                    .padding(24.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = songName,
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Song ${currentIndex + 1} of $playlistSize",
                color = Color.Gray,
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onPreviousSong) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    PlayPauseButton(isPlaying, onPlayPauseClick)
                    IconButton(onClick = onNextSong) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
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
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color.White, CircleShape)
            .clickable(onClick = onPlayPauseClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
        )
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
            .padding(horizontal = 2.dp)
    ) {

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { newValue ->
                onSeek(newValue.toLong())
            },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Red,
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 14.sp
            )
        }
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
            onPreviousSong = { },
            onBackPress = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMiniPlayer() {
    MusicAppTheme {
        MiniPlayer(
            song = Song(1, "Preview Song", "Preview Artist","https://example.com/song.mp3",R.drawable.img_6),
            isPlaying = true,
            currentPosition = 60000L,
            duration = 180000L,
            onPlayPauseClick = { },
            onNextSong = { },
            onPreviousSong = { },
            onMiniPlayerClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSongsPage() {
    val sharedViewModel = SharedViewModel()
    MusicAppTheme {
        SongsPage(
            playlistId = 1,
            onSongClick = { },
            sharedViewModel = sharedViewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPlaylistPage() {
    MusicAppTheme {
        PlaylistPage(onPlaylistClick = { })
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomePage() {
    val sharedViewModel = SharedViewModel()

    // Add some sample recently played songs
    sharedViewModel.setPlaylist(listOf(
        Song(1, "Recently Played 1", "Artist 1","url1",R.drawable.img_6),
        Song(2, "Recently Played 2", "Artist 2","url2",R.drawable.img_7),
        Song(3, "Recently Played 3", "Artist 3","url3",R.drawable.img_9)
    ))
    sharedViewModel.addToRecentlyPlayed(Song(1, "Recently Played 1",  "Artist 1","url1",R.drawable.img_6))
    sharedViewModel.addToRecentlyPlayed(Song(2, "Recently Played 2",  "Artist 2","url2",R.drawable.img_7))
    sharedViewModel.addToRecentlyPlayed(Song(3, "Recently Played 3",  "Artist 3","url3",R.drawable.img_9))

    MusicAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomePage(
                navigateToPlaylist = { },
                navigateToSong = { },
                sharedViewModel = sharedViewModel
            )
        }
    }
}
