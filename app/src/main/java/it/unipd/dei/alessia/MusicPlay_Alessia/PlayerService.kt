package it.unipd.dei.alessia.MusicPlay_Alessia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class PlayerService : Service()
{
    private var myPlayer: MediaPlayer? = null

    //variabili di stato
    var isPlaying = false
    private var isPrepared = false
    private var repeatMode = 0

    //gestione playlist
    private var songList: List<Song> = emptyList()
    private var currentSongIndex: Int = -1 //variabile per capire cosa c'e' in memoria

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        //questa funzione restituisce il servizio stesso all'Activity
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    //il playerService deve leggere sia file all'interno dell'app, sia file esterni identificati da un URI
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val playStart = intent?.getBooleanExtra(PLAY_START, false) ?: false

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent.action

        when (action) {
            "ACTION_RESUME" -> resume()
            "ACTION_STOP" -> {
                pause()
                stopForeground(STOP_FOREGROUND_DETACH)
            }
            "ACTION_DELETE_STOP" -> {
                stop() //chiama la funzione stop() che fa release e null
                currentSongIndex = -1
            }
            else -> {
                if (playStart) {
                    val resId = intent?.getIntExtra("SONG_RES_ID", 0) ?: 0
                    val uriString = intent?.getStringExtra("SONG_URI") ?: ""
                    val title = intent?.getStringExtra("SONG_TITLE") ?: "Brano"
                    val artist = intent?.getStringExtra("SONG_ARTIST") ?: "Artista"

                    updateCurrentIndex(resId, uriString)
                    play(resId, uriString, title, artist)
                }
            }
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate()
    {
        super.onCreate()

        // Create the NotificationChannel, but only on API level 26+
        val name: CharSequence = "Music Player"
        val description = "MusicPlayerChannel"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        //devo forzare il stop(), altrimenti il player continua a suonare anche quando il brano viene eliminato
        //crea un receiver interno al Service
        val deleteReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "FORCE_STOP_DELETE") {
                    stop()
                }
            }
        }
        //registrarlo in onCreate
        val filter = IntentFilter("FORCE_STOP_DELETE")
        registerReceiver(deleteReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    private fun play(resId: Int, uriString: String, title: String, artist: String) {
        //pulizia totale del player precedente
        myPlayer?.apply {
            if (isPlaying) stop()
            reset()    // Riporta il player allo stato Idle
            release()  // Libera la memoria
        }

        myPlayer = MediaPlayer()
        isPrepared = false

        try {
            myPlayer?.apply {
                //impostazione del player per non andare in pausa quando il tablet spegne lo schermo
                setWakeMode(this@PlayerService, PowerManager.PARTIAL_WAKE_LOCK)

                if (resId != 0) {
                    //caso 1: brano nelle risorse interne (res/raw)
                    val afd = resources.openRawResourceFd(resId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } else if (uriString.isNotEmpty()) {
                    //caso 2: brano scaricato sul Tablet A8 (URI)
                    setDataSource(this@PlayerService, Uri.parse(uriString))
                }

                setOnPreparedListener {
                    it.start()
                    this@PlayerService.isPlaying = true
                    isPrepared = true
                }

                //quando ha finito di suonare la musica, spetta al fragment a decidere cosa suonare dopo
                setOnCompletionListener {
                    playPrevNext(1,false)//1 per suonare il brano successivo
                }
                //prepareAsync non blocca l'app mentre carica il file (importante per file grandi)
                prepareAsync()
            }
            updateNotification(title, artist)
        } catch (e: Exception) {
            Log.e("SERVICE_DEBUG", "Errore Play: ${e.message}")
            isPlaying = false
        }
    }

    private fun stop() {
        isPlaying = false
        myPlayer?.let {
            if (it.isPlaying) it.stop()
            it.reset()
            it.release()
        }
        myPlayer = null
        stopForeground(true)
    }

    //funzine per riprendere dal punto in cui viene messo in stop
    private fun resume() {
        if (myPlayer != null && !myPlayer!!.isPlaying) {
            myPlayer?.start()
            isPlaying = true
        }
    }

    //modifica la funzione stop in pause se vuoi che mantenga la posizione
    private fun pause() {
        if (myPlayer?.isPlaying == true) {
            myPlayer?.pause()
            isPlaying = false
        }
    }

    //restituisce la durata totale in millisecondi per la SeekBar
    fun getDuration(): Int = if (isPrepared) myPlayer?.duration ?: 0 else 0

    //restituisce la posizione attuale in millisecondi per la SeekBar
    fun getCurrentPosition(): Int = if (isPrepared) myPlayer?.currentPosition ?: 0 else 0

    //permette di saltare a un punto specifico (usato quando trascini la SeekBar)
    fun seekTo(pos: Int) { myPlayer?.seekTo(pos) }

    override fun onDestroy()
    {
        stop()
        super.onDestroy()
    }

    fun setMode(mode: Int) {
        this.repeatMode = mode
    }

    fun setPlaylist(list: List<Song>, currentSong: Song?) {
        this.songList = list
        currentSong?.let {
            updateCurrentIndex(it.resourceId, it.uriString ?: "")
        }
    }

    //logica per cambiare canzone precedente(1) o successivo(-1)
    fun playPrevNext(direction: Int, isManualClick: Boolean = false) {

        if (songList.isEmpty()) {
            stop()
            return
        }

        val size = songList.size

        when (repeatMode) {
            0 -> { // Sequenziale
                currentSongIndex = (currentSongIndex + direction + size) % size
            }
            1 -> {
                if (isManualClick) currentSongIndex = (currentSongIndex + direction + size) % size
            }
            2 -> { //shuffle
                currentSongIndex = songList.indices.filter { it != currentSongIndex }.random()
            }
        }

        val nextSong = songList[currentSongIndex]
        play(nextSong.resourceId, nextSong.uriString ?: "", nextSong.title, nextSong.artist)

        //notifica il Fragment che la canzone è cambiata (per aggiornare titolo/immagine)
        val intent = Intent("ACTION_NEW_SONG_STARTED")
        intent.putExtra("SONG_ID", nextSong.id)
        sendBroadcast(intent)
    }

    //trova la posizione della canzone nella lista per sapere chi è la "prossima"
    private fun updateCurrentIndex(resId: Int, uri: String) {
        currentSongIndex = songList.indexOfFirst {
            if (resId != 0) it.resourceId == resId
            else it.uriString == uri
        }
    }

    private fun updateNotification(title: String, artist: String) {
        //aggiornamento notifica (uso del notification compat)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.music_note)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        //questo avvia il servizio in Foreground e mostra la notifica (obbligatorio su Android moderni)
        startForeground(NOTIFICATION_ID, notification)
    }

    companion object
    {
        private const val CHANNEL_ID = "musicplay_alessia"
        private const val NOTIFICATION_ID = 5786423
        const val PLAY_START = "BGPlayStart"
    }
}