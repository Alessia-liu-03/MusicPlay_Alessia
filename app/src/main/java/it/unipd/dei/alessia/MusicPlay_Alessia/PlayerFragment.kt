package it.unipd.dei.alessia.MusicPlay_Alessia

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class PlayerFragment : Fragment() {

    private lateinit var buPlayStop: ImageButton
    private lateinit var buPrecedente: ImageButton
    private lateinit var buSuccessivo: ImageButton
    private lateinit var buBack: ImageButton
    private lateinit var buMode: ImageButton
    private lateinit var buMenu: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var txtTime: TextView
    private lateinit var title: TextView
    private var isPlaying = false
    private var myPlayer: PlayerService? = null
    private var isBound = false
    private val handler = Handler(Looper.getMainLooper())
    private val viewModel: SongViewModel by activityViewModels()
    private lateinit var ivAlbumArt: ImageView//immagine al centro del fragment
    private var rotationAnimator: ObjectAnimator? = null //immagine che ruota

    //gestisce il problema di clliccare piu' volte sul timer,resetta  timer precedenti
    private var timerRunnable: Runnable? = null

    //flag per prevenire race conditions tra il cambio brano manuale (Fragment)
    //e il segnale automatico di fine brano inviato dal Service.
    private var isChangingSongManually = false

    //Connessione al Servizio
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.LocalBinder
            myPlayer = binder.getService()
            isBound = true

            //sincronizziamo il modo attuale con il servizio appena connesso
            val list = viewModel.allSongs.value ?: emptyList()
            val current = viewModel.selectedSong.value
            myPlayer?.setPlaylist(list, current)
            myPlayer?.setMode(viewModel.repeatMode.value ?: 0)

            //se il servizio sta già suonando, agggiorna l'interfaccia
            if (myPlayer?.isPlaying == true) {
                isPlaying = true
                buPlayStop.setImageResource(R.drawable.pause)
                handler.post(updateSeekBar)
                handleRotation(true)
            }
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            myPlayer = null
        }
    }

    private val songStartedReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_NEW_SONG_STARTED") {
                val songId = intent.getIntExtra("SONG_ID", -1)
                val songList = viewModel.allSongs.value

                //cerca il brano nella lista e aggiorna il ViewModel
                val nextSong = songList?.find { it.id == songId }
                if (nextSong != null && nextSong != viewModel.selectedSong.value) {
                    viewModel.updateCurrentSong(nextSong)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)

        //senza questa funzione, nell'UI di player il titolo e il pulsante home viene sotto la actionbar
        view.setOnApplyWindowInsetsListener { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        return view
    }

    //avvia il binding quando il fragment è visibile
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        Intent(requireContext(), PlayerService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        //registrazione del Receiver quando il Fragment parte
        val filter = IntentFilter("ACTION_NEW_SONG_STARTED")
        requireContext().registerReceiver(songStartedReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
        //rimozione del Receiver per evitare memory leak
        try {
            context?.unregisterReceiver(songFinishedReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("PLAYER_DEBUG", "Receiver già rimosso o mai registrato")
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateSeekBar)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(updateSeekBar)
        handler.post(updateSeekBar)
    }

    private val updateSeekBar = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            if (isBound && myPlayer != null) {
                val pos = myPlayer!!.getCurrentPosition()
                val duration = myPlayer!!.getDuration()

                if (duration > 0) {
                    seekBar.max = duration
                    seekBar.progress = pos
                    txtTime.text = "${formatTime(pos)} / ${formatTime(duration)}"
                }
            }
            //ri-schedula l'esecuzione ogni secondo
            handler.postDelayed(this, 1000)
        }
    }

    //riceve azioni da viewModel: quando arriva un nuovo brano, deve fermare il vecchio e far partirei il nuovo
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seekBar = view.findViewById(R.id.seekBar)
        txtTime = view.findViewById(R.id.txtTime)
        title = view.findViewById(R.id.song_title)
        buPlayStop = view.findViewById(R.id.PlayStopButton)
        buPrecedente = view.findViewById(R.id.buPrecedente)
        buSuccessivo = view.findViewById(R.id.buSuccessivo)
        buBack = view.findViewById(R.id.buBackToList) //bu per toranre alla lista dei brani nei dispositivi mobili
        buMode = view.findViewById(R.id.buMode)
        buMenu = view.findViewById(R.id.buMenu)
        //osservare direttamente il brano selezionato nel ViewModel.
        val viewModel: SongViewModel by activityViewModels()

        //crea immagine
        ivAlbumArt = view.findViewById(R.id.ivAlbumArtPlayer)
        rotationAnimator = ObjectAnimator.ofFloat(ivAlbumArt, "rotation", 0f, 360f).apply {
            duration = 10000 // 5 secondi per un giro completo
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }//dati per la rotazione

        val isTablet = resources.getBoolean(R.bool.is_tablet)//ritorna se il dispositivo e' tablet o no
        if (isTablet) {
            //se e' true, abbiamo un tablet, quindi non visualizzare il pulsante home
            buBack.visibility = View.GONE
        }

        handler.post(updateSeekBar)

        //logica pulsante SUCCESSIVO
        buSuccessivo.setOnClickListener {
            //chiede al Service di passare alla prossima
            myPlayer?.playPrevNext(1,true)
        }

        //logica pulsante PRECEDENTE
        buPrecedente.setOnClickListener {
            val currentPos = myPlayer?.getCurrentPosition() ?: 0
            myPlayer?.playPrevNext(-1, true)

            /*se la canzone sta suonando da più di 3 secondi, falla ricominciare
            if (currentPos > 3000) {
                myPlayer?.seekTo(0)
            } else {
                //altrimenti vai a quella precedente

            }*/
        }

        buPlayStop.setOnClickListener {
            val intent = Intent(requireContext(), PlayerService::class.java)
            val currentSong = viewModel.selectedSong.value // Recupero la canzone selezionata
            val resId = currentSong?.resourceId

            if (!isPlaying) {
                //se il player siste gia' ed e' solo in pausa, riprendi
                if(myPlayer!=null && (myPlayer?.getDuration()?:0)>0){
                    intent.action = "ACTION_RESUME" //diciamo al service di continuare
                }else{
                    //e' la prima volta che premiamo play, allora carichiamo il brano
                    intent.putExtra(PlayerService.PLAY_START, true)
                    // passo l'ID attuale o l'URI per assicurare che il servizio sappia cosa suonare
                    // Se resourceId è 0, il servizio userà l'uriString
                    if (resId != null) {
                        intent.putExtra("SONG_RES_ID", resId.toInt())
                    }
                    intent.putExtra("SONG_URI", currentSong?.uriString)
                }

                requireContext().startService(intent)
                isPlaying = true
                handler.post(updateSeekBar)
                buPlayStop.setImageResource(R.drawable.pause)
            } else {
                //azione: pausa
                intent.action = "ACTION_STOP"
                requireContext().startService(intent)
                isPlaying = false
                handler.removeCallbacks(updateSeekBar)
                buPlayStop.setImageResource(R.drawable.play)
            }
            handleRotation(isPlaying)//ruotare immagine
        }

        buBack.setOnClickListener {
            //questo comando simula il tasto "Indietro" del telefono
            //chiude il PlayerFragment e mostra quello che c'era sotto (la lista)
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        //gestione del menu
        buMenu.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.player_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.opt_rename_title -> {
                        showRenameDialog("Titolo")//cambia titolo
                        true
                    }
                    R.id.opt_rename_artist -> {
                        showRenameDialog("Artista")//cambia artista
                        true
                    }
                    R.id.opt_timer -> {
                        showTimerDialog()//imposta un timer, alla fine del timer il brano viene stoppato automaticamente
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        buMode.setOnClickListener {
            //ciclo: 0 -> 1 -> 2 -> 0
            viewModel.toggleRepeatMode()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) myPlayer?.seekTo(p)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        }
        )

        viewModel.selectedSong.observe(viewLifecycleOwner) { song ->
            //se song è null, il player fragment e' vuota.
            if (song == null) {

                if (!isTablet) {
                    //logica cellulare, ritorna alla songlist
                    if (isAdded) {
                        view.post {
                            parentFragmentManager.popBackStack()
                        }
                    }
                }else{
                    resetUIForNewSong()
                    title.text = "Scegli un brano"
                    ivAlbumArt.setImageDrawable(null)
                    buPlayStop.setImageResource(R.drawable.play)

                    //disabilitare i tasti per evitare che l'utente clicchi sul vuoto
                    seekBar.isEnabled = false
                    buPlayStop.isEnabled = false
                    buPrecedente.isEnabled = false
                    buSuccessivo.isEnabled = false
                    buMode.isEnabled = false
                }
                return@observe
            }

            title.text = song.title//aggiorna i testi nel layout
            //caricamente immagine
            if(!song.imagePath.isNullOrEmpty()){
                val bitmap = BitmapFactory.decodeFile(song.imagePath)//carichiano il file salvato nella memoria interna
                ivAlbumArt.setImageBitmap(bitmap)
            }else{
                ivAlbumArt.setImageResource(R.drawable.image)//se non c'e' immagine nel database
            }

            //sincronizza il Service se non lo è gia'
            if (isBound && (myPlayer?.isPlaying == false)) {
                val intent = Intent(requireContext(), PlayerService::class.java).apply {
                    putExtra(PlayerService.PLAY_START, true)
                    putExtra("SONG_RES_ID", song.resourceId)
                    putExtra("SONG_TITLE", song.title)
                    putExtra("SONG_ARTIST", song.artist)
                    putExtra("SONG_URI", song.uriString)
                }
                requireContext().startService(intent)
                isPlaying = true
                buPlayStop.setImageResource(R.drawable.pause)
            }
            resetUIForNewSong()
        }

        viewModel.repeatMode.observe(viewLifecycleOwner) { mode ->
            //quando il modo cambia nel ViewModel, aggiornare l'interfaccia e il Service
            updateModeIcon(mode)
            myPlayer?.setMode(mode)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(ms: Int): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    //funzione per resettare  l"UI quando cambia il brano
    private fun resetUIForNewSong() {
        seekBar.progress = 0
        txtTime.text = "--:-- / --:--"
        isPlaying = true
        buPlayStop.setImageResource(R.drawable.pause)
        resetAlbumArtRotation()
        handleRotation(true)
    }

    //funzione per ruotare immagine
    private fun handleRotation(shouldRotate: Boolean) {
        if (shouldRotate) {
            if (rotationAnimator?.isStarted == false) {
                rotationAnimator?.start()
            } else {
                rotationAnimator?.resume()
            }
        } else {
            rotationAnimator?.pause()
        }
    }

    //fa il reset della posizione dell'immagine che ruota ogni volta che viene cambiato il brano
    private fun resetAlbumArtRotation() {
        //ferma l'animazione attuale se è in corso
        rotationAnimator?.cancel()

        //riporta la rotazione della View a 0 gradi
        ivAlbumArt.rotation = 0f
    }

    //riceve un messaggio quando il brano e' terminato
    private val songFinishedReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
             
            if (intent?.action == "ACTION_SONG_FINISHED") {
                val songId = intent.getIntExtra("SONG_ID", -1)
                val songList = viewModel.allSongs.value

                val nextSong = songList?.find { it.id == songId }
                if (nextSong != null) {
                    viewModel.updateCurrentSong(nextSong)
                }
            }
        }
    }

    private fun updateModeIcon(mode: Int){
        //controllo se la variabile è stata inizializzata per evitare crash
        if (!::buMode.isInitialized) return

        when (mode) {
            0 -> buMode.setImageResource(R.drawable.repeat) //sequenziale
            1 -> buMode.setImageResource(R.drawable.repeat_one) //ripeti questo
            2 -> buMode.setImageResource(R.drawable.shuffle)    //casuale
        }
    }

    //funzione per rinomnare titolo/arrtista
    private fun showRenameDialog(type: String) {
        val currentSong = viewModel.selectedSong.value ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null)
        val txtTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val editInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dialog_input)

        //configurazione testi
        txtTitle.text = "Rinomina $type"
        val initialText = if (type == "Titolo") currentSong.title else currentSong.artist
        editInput.setText(initialText)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val newText = editInput.text.toString()
                if (newText.isNotEmpty()) {
                    viewModel.updateSongInfo(currentSong.id, type, newText)
                    if (type == "Titolo") title.text = newText
                }
            }
            .setNegativeButton("Annulla", null)
            .create()
        dialog.show()
    }

    //funzione per impostare il timer, alla fine del timer il brano viene stoppato
    private fun showTimerDialog() {
        val options = arrayOf("10 secondi", "5 minuti", "15 minuti", "30 minuti", "Disattiva")
        AlertDialog.Builder(requireContext())
            .setTitle("Spegni tra...")
            .setItems(options) { _, which ->

                //prima di far partire il timer, cancella quello vecchio
                timerRunnable?.let { handler.removeCallbacks(it) }

                val minutes = when (which) {
                    0 -> 10 * 1000L
                    1 -> 5 * 60 * 1000L
                    2 -> 15 * 60 * 1000L
                    3 -> 30 * 60 * 1000L
                    else -> 0L
                }

                if (minutes > 0) {

                    timerRunnable = Runnable {
                        //ferma la musica dopo x minuti
                        val intent = Intent(requireContext(), PlayerService::class.java)
                        intent.action = "ACTION_STOP"
                        requireContext().startService(intent)

                        //aggiorna l'interfaccia
                        isPlaying = false
                        buPlayStop.setImageResource(R.drawable.play)//torna l'icona play
                        handleRotation(false) //ferma la rotazione dell'immagine
                        handler.removeCallbacks(updateSeekBar) //ferma l'avanzamento della SeekBar

                        //feedback visivo quando scatta il timer
                        if (isAdded) { // evita crash se il fragment non è più visibile
                            android.widget.Toast.makeText(context, "Timer: Musica fermata", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    //avvio del timer
                    handler.postDelayed(timerRunnable!!, minutes)

                    //conferma all'utente che il timer è partito
                    val label = options[which]
                    android.widget.Toast.makeText(context, "Timer impostato: $label", android.widget.Toast.LENGTH_SHORT).show()
                }else{
                    timerRunnable = null
                    android.widget.Toast.makeText(context, "Timer rimosso", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
                .show()
    }
}