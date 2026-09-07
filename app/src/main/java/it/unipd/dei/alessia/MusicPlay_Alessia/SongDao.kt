package it.unipd.dei.alessia.MusicPlay_Alessia

import androidx.room.*
import kotlinx.coroutines.flow.Flow

//questa classe dice a Room come leggere e scrivere i dati
// (quali operazioni si possono fare su di essi)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs_table")
    fun getAllSongs(): Flow<List<Song>> //flow aggiorna la lista in tempo reale!

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("UPDATE songs_table SET title = :newText WHERE id = :songId")
    suspend fun updateTitle(songId: Int, newText: String)

    @Query("UPDATE songs_table SET artist = :newText WHERE id = :songId")
    suspend fun updateArtist(songId: Int, newText: String)
}