package it.unipd.dei.alessia.MusicPlay_Alessia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

//uso questa classe per estrarre i metadata (artista, nome brano, e immagine) dai file .mp3

class MediaMetadataRetriever {
    fun getSongMetadataAndCopy(uri: Uri, context: Context): Song {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //estrarre i metadati (Titolo e Artista)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Brano Sconosciuto"
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Artista Sconosciuto"

        //estrarre l'immagine (i byte della copertina)
        val artBytes = retriever.embeddedPicture
        var internalImgPath: String? = null

        if (artBytes != null) {
            //se c'è una foto, la salviamo come file .jpg interno
            val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            val imgFile = File(context.filesDir, "thumb_${System.currentTimeMillis()}.jpg")
            imgFile.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            internalImgPath = imgFile.absolutePath
        }

        retriever.release()

        //copia il file audio per averlo internamente
        val audioPath = copyFileToInternal(uri, context)

        return Song(
            title = title,
            artist = artist,
            resourceId = 0, // 0 perche' è un file esterno
            uriString = audioPath,
            imageResId = 0,
            imagePath = internalImgPath
        )
    }

    private fun copyFileToInternal(uri: Uri, context: Context): String {
        val fileName = "song_${System.currentTimeMillis()}.mp3"
        val file = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

}