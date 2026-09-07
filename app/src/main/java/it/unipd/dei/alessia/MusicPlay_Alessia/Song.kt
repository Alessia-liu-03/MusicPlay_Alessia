package it.unipd.dei.alessia.MusicPlay_Alessia

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

//contiene le informazione base di ogni brano
//nel db, definisce una riga della tabella

@Entity(tableName = "songs_table")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,               //Room genererà l'ID automaticamente
    val title: String,
    val artist: String,
    val resourceId: Int,           //puntatore a res/raw (0 se esterno)
    val uriString: String? = null, //percorso del file audio interno/esterno
    val imagePath: String? = null, //salvaraggio del percorso dell'immagine
    val imageResId: Int = 0       //ID per l'immagine in res/drawable (se interna)

    ){
    @Ignore
    var isMenuOpen: Boolean = false
}
