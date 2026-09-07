package it.unipd.dei.alessia.MusicPlay_Alessia

import kotlinx.coroutines.flow.Flow

//questa classa fa da intermediario tra il DAO e il ViewModel

class SongRepository(private val songDao: SongDao) {

    //legge tutti i brani come un flusso di dati continuo
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()

    //operazione di inserimento (deve essere 'suspend' perché Room non permette
    //operazioni di scrittura sul thread principale per non bloccare l'UI)
    suspend fun insert(song: Song) {
        songDao.insertSong(song)
    }

    suspend fun delete(song: Song) {
        songDao.deleteSong(song)
    }

    suspend fun updateTitle(songId: Int, newText: String) {
        songDao.updateTitle(songId, newText)
    }

    suspend fun updateArtist(songId: Int, newText: String) {
        songDao.updateArtist(songId, newText)
    }
}