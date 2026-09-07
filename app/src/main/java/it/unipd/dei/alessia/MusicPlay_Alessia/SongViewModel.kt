package it.unipd.dei.alessia.MusicPlay_Alessia

//ogni item soppravvive al cambiamento dell'orientamento
//questa classe e' usato per comunicare tra i fragment
//gestisce i dati

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongViewModel(application: Application) : AndroidViewModel(application) {

    //uso repostory per aggiungere brani al database
    private val repository: SongRepository
    val allSongs: LiveData<List<Song>>
    //memorizza la modalita' attuale: 0 -> sequenziale, 1 -> repeat, 2 -> random(shuffle)
    private val _repeatMode = MutableLiveData(0)
    val repeatMode: LiveData<Int> = _repeatMode
    val navigateToPlayer = MutableLiveData<Boolean>()

    //istanziamo l'helper per i metadati
    private val metadataHelper = MediaMetadataRetriever()

    init {
        //inizializzazione del databse e repository
        val songDao = AppDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao)

        // trasformo il Flow del repository in LiveData per l'interfaccia
        allSongs = repository.allSongs.asLiveData()
    }

    //variabile per gestire la selezione del brano (quello che va al Player)
    val selectedSong = MutableLiveData<Song?>()

    fun selectSong(song: Song?) {
        selectedSong.value = song
        if (song != null) {
            navigateToPlayer.value = true //naviga solo se c'e' una canzone
        }
    }

    //usato quando l'utente sceglie un file esterno e lo aggiunge alla lista
    fun addExternalSong(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            // Usiamo la funzione sopra per creare l'entità completa
            val songEntity = metadataHelper.getSongMetadataAndCopy(uri, getApplication())

            // Salviamo nel Database Room
            repository.insert(songEntity)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(song)
        }
    }

    ////cicla tra 0, 1, 2 e poi torna a 0
    fun toggleRepeatMode() {
        _repeatMode.value = ((_repeatMode.value ?:0 )+1) % 3
    }

    fun updateSongInfo(songId: Int, type: String, newText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (type == "Titolo") {
                repository.updateTitle(songId, newText)
            } else {
                repository.updateArtist(songId, newText)
            }
        }
    }

    fun doneNavigating() {
        navigateToPlayer.value = false //reset del segnale
    }

    fun updateCurrentSong(song: Song) {
        selectedSong.value = song
    }
}