package de.carsten.android.muzzic.persistence.mock

import de.carsten.android.muzzic.persistence.MuzzicDatabase
import de.carsten.android.muzzic.persistence.entity.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.random.Random

object DatabaseSeeder {
    private val titles = listOf("Echoes", "Shadows", "Sunlight", "Midnight", "Dancing", "Silence", "Storm", "Harmony", "Rhythm", "Pulse", "Dream", "Waves", "Neon", "Sky", "Flow", "Beyond", "Origins", "Visions")
    private val artists = listOf("The Voyagers", "Electric Dreams", "Luna", "Solaris", "Beat Master", "Velvet Voice", "Crystal Clear", "The Rebels", "Audio Soul", "Sonic Youth", "Deep Bass")
    private val albums = listOf("Horizon", "Neon Lights", "Discovery", "Evolution", "Origins", "Visions", "Atmosphere", "Ethereal", "Legacy", "Infinite", "Spectrum")
    private val genres = listOf("Rock", "Pop", "Jazz", "Classical", "Electronic", "Hip Hop", "Synthwave", "Lo-fi", "Ambient", "Metal", "Soul")

    fun seed(database: MuzzicDatabase, count: Int = 50) {
        CoroutineScope(Dispatchers.IO).launch {
            val songDao = database.songDao()

            // Only seed if the database is empty (check alphabet or any other quick query)
            if (songDao.getSongAlphabet().isNotEmpty()) return@launch

            val mockSongs = List(count) { index ->
                val artist = artists.random()
                val album = albums.random()
                Song(
                    title = "${titles.random()} ${titles.random()}",
                    trackNumber = Random.nextInt(1, 13),
                    totalTracks = 12,
                    artist = artist,
                    album = album,
                    genre = genres.random(),
                    duration = Random.nextLong(120000, 360000), // 2-6 minutes
                    filePath = "content://mock/audio_$index",
                    albumArt = "https://picsum.photos/seed/${index + Random.nextInt()}/400/400",
                    albumYear = Random.nextInt(1970, 2025),
                    rating = Random.nextInt(0, 256), // WMP-style 0-255 rating
                    playCount = Random.nextInt(0, 50),
                    lastPlayed = Instant.now().minusSeconds(Random.nextLong(0, 2592000)) // played within the last 30 days
                )
            }

            songDao.insertSongs(mockSongs)
        }
    }
}
