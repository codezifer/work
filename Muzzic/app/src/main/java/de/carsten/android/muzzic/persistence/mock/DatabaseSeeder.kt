package de.carsten.android.muzzic.persistence.mock

import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.persistence.MuzzicDatabase
import de.carsten.android.muzzic.persistence.entity.Song
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseSeeder {
    private val logger = logger()
    private val titles =
        listOf(
            "Echoes",
            "Shadows",
            "Sunlight",
            "Midnight",
            "Dancing",
            "Silence",
            "Storm",
            "Harmony",
            "Rhythm",
            "Pulse",
            "Dream",
            "Waves",
            "Neon",
            "Sky",
            "Flow",
            "Beyond",
            "Origins",
            "Visions",
            "The Last Whisper of a Dying Star",
            "Fragile Echoes in the Wind",
            "Obsidian Symphony of the Night",
            "Radiance",
            "Forgotten",
        )
    private val artists =
        listOf(
            "The Voyagers",
            "Electric Dreams",
            "Luna",
            "Solaris",
            "Beat Master",
            "Velvet Voice",
            "Crystal Clear",
            "The Rebels",
            "Audio Soul",
            "Sonic Youth",
            "Deep Bass",
            "Orchestra of the Infinite Void",
            "The Crimson Ghost Collective",
            "Starlight Explorers",
        )
    private val albums =
        listOf(
            "Horizon",
            "Neon Lights",
            "Discovery",
            "Evolution",
            "Origins",
            "Visions",
            "Atmosphere",
            "Ethereal",
            "Legacy",
            "Infinite",
            "Spectrum",
            "A Journey Through the Frozen Wastelands",
            "The Architectural Wonders of Tomorrow",
            "Echoes from the Ancient Labyrinth",
        )
    private val genres =
        listOf(
            "Rock",
            "Pop",
            "Jazz",
            "Classical",
            "Electronic",
            "Hip Hop",
            "Synthwave",
            "Lo-fi",
            "Ambient",
            "Metal",
            "Soul",
        )

    fun seed(database: MuzzicDatabase, count: Int = 50) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val songDao = database.songDao()

                // Only seed if the database is empty
                val existingAlphabet = songDao.getSongAlphabet()
                logger.info("Existing alphabet size: ${existingAlphabet.size}")
                if (existingAlphabet.isNotEmpty()) {
                    logger.info("Database already seeded, skipping.")
                    return@launch
                }

                logger.info("Seeding database with $count songs...")

                val mockSongs =
                    List(count) { index ->
                        val artist = artists.random()
                        val album = albums.random()
                        val mockPath = "https://picsum.photos/500?random=$index"
                        val seededAlbumArt = AlbumArtUri(mockPath).get()

                        Song(
                            title = if (Random.nextBoolean()) titles.random() else "${titles.random()} ${titles.random()}",
                            trackNumber = Random.nextInt(1, 13),
                            totalTracks = 12,
                            artist = artist,
                            album = album,
                            genre = genres.random(),
                            duration = Random.nextLong(120000, 360000), // 2-6 minutes
                            filePath = mockPath,
                            albumArt = seededAlbumArt,
                            albumYear = Random.nextInt(1970, 2025),
                            rating = Random.nextInt(0, 256), // WMP-style 0-255 rating
                            playCount = 0,
                            lastPlayed = Instant.now().minusSeconds(Random.nextLong(0, 2592000)),
                        )
                    }

                songDao.insertSongs(mockSongs)
                logger.info("Successfully inserted ${mockSongs.size} songs.")

                val now = Instant.now()
                val sixMonthsSeconds = 6 * 30 * 24 * 60 * 60L

                mockSongs.forEach { song ->
                    val songId = song.id
                    val individualPlayCount = Random.nextInt(5, 50) // Ensure at least some plays
                    val lastPlayed = now.minusSeconds(Random.nextLong(0, sixMonthsSeconds))

                    // Update the song's play count and last played to simulate activity
                    songDao.updatePlayCount(songId, individualPlayCount, lastPlayed.toEpochMilli())
                }

                logger.info("Database seeding completed successfully.")
            } catch (e: Exception) {
                logger.error("Error during database seeding", e)
            }
        }
    }
}
