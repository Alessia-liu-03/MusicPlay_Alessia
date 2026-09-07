package it.unipd.dei.alessia.MusicPlay_Alessia

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SongListFragment : Fragment() {

    private val viewModel: SongViewModel by activityViewModels()
    private lateinit var navigator: Navigator

    //trova la recyclerView nel layout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)
        navigator = Navigator(parentFragmentManager, requireContext())
        recyclerView = view.findViewById(R.id.song_recycler_view)

        adapter = SongAdapter(mutableListOf(), onClick = { song ->
            //aggiornare il brano selezionato nel viewModel
            viewModel.selectSong(song)

            //fa partire la musica immediatamente (senza aspettare il Fragment)
            //senza questo, la musica suona se clicchi: brano -> start
            //con questo, la musica suona se clicchi: brano
            val intent = Intent(requireContext(), PlayerService::class.java).apply {
                putExtra(PlayerService.PLAY_START, true)
                putExtra("SONG_RES_ID", song.resourceId)
                putExtra("SONG_URI", song.uriString)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artist)
            }
            requireContext().startService(intent)
        },
            onDeleteClick = { viewHolder, songToDelete ->
                    //controlla se il brano che stai eliminando è quello in riproduzione
                    val currentPlaying = viewModel.selectedSong.value

                    if (currentPlaying?.id == songToDelete.id) {
                        //Stop immediato del brano
                        //invia il segnale di "Stop Immediata" al Service
                        val intent = Intent(requireContext(), PlayerService::class.java).apply {
                            action = "ACTION_STOP" // Assicurati che sia lo stesso nome nel Service
                        }
                        requireContext().startService(intent)
                        //reset del ViewModel così il player (anche se nascosto) si svuota
                        viewModel.selectSong(null)
                        viewModel.doneNavigating()
                    }

                //facciamo partire la nuova animazione
                //dobbiamo passare  il viewHolder così sa quale immagine far cadere
                animateDeleteManual(viewHolder, songToDelete)
            }
        )

        //imposta il layoutManager
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        //collega l'adapter
        recyclerView.adapter = adapter

        //se siamo su cellulare, dobbiamo anche navigare verso il PlayerFragment
        viewModel.navigateToPlayer.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate == true) {
                navigator.navigateToPlayer()
                viewModel.doneNavigating()//resetta il segnale
            }
        }

        // ogni volta che la lista cambia (aggiungere un brano),
        // il codice qui dentro viene eseguito automaticamente.
        viewModel.allSongs.observe(viewLifecycleOwner) { nuovaLista ->
            // Aggiorna i dati dell'adapter
            adapter.setData(nuovaLista)
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_song).setOnClickListener {
            launcher.launch("audio/*") //filtra solo i file audio
        }

        //per android 15+: aggiungiamo il padding dinamico
        //per risolvere il problema di mostrare il titolo e il pulsante"buBackHome" sotto actionbar
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            //applichiamo il padding dinamico:
            //systemBars.top sarà l'altezza esatta della fascia viola(actionbar) su Android 15
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            windowInsets
        }
        return view
    }

    // launcher per selezionare i file
    private val launcher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris?.forEach { uri ->
                try {
                    //PER ANDROID 14: prendere il permesso persistente
                    val contentResolver = requireContext().contentResolver

                    try {
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        //questa riga dice ad Android di non revocare il permesso alla chiusura del selettore
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                    }catch (e: SecurityException){
                        Log.w("DEBUG_CRASH", "Errore nel prendere i permessi per l'URI: $uri", e)
                    }
                    //ora aggiungiamolo al viewModel
                    viewModel.addExternalSong(uri)

                } catch (e: Exception) {
                    Log.e("DEBUG_CRASH", "Errore critico durante l'aggiunta dell'URI: $uri", e)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fab: FloatingActionButton = view.findViewById(R.id.fab_add_song)
        fab.setOnClickListener {
            launcher.launch("audio/*")
        }

        //se trascino i brano del list verso sx, viene mostrato un pulsante "cestino", se lo clicco, elimina il brano
        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false //non ci interessa spostare i brani su/giù

                //se sposto il brano verso sx,visualizza il background con il pulsante delete
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    //forza il ritorno del brano in posizione se viene trascinato troppo veloce
                    val position = viewHolder.adapterPosition
                     
                    adapter.songs[position].isMenuOpen = true//il menu del brano e' aperto
                    adapter.notifyItemChanged(position)//diciamo all'adapter di ridisegnare la riga
                    //la lista si aggiornerà da sola grazie all'Observer di allSongs
                }

                //impedisce che lo swipe si completi da solo
                override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                    return 0.3f //significa che dovresti trascinare il 30% della lunghezza per completare il swipe
                }

                //impedisce che la velocità del dito causi l'eliminazione
                override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                    return Float.MAX_VALUE //la velocità del dito non farà mai scattare l'eliminazione
                }

                //descrive cosa fa se moviano il brano di 100dp verso sx
                // (mostra il background:il pulsante delete)
                //limita il movimento del dito, e non fa vedere piu' di 100dp
                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {

                    //muoviamo solo la parte superiore (foreground_view)
                    val foregroundView = (viewHolder as SongAdapter.SongViewHolder).foregroundView

                    //limitiamo lo swipe verso sinistra a massimo 100dp (la larghezza del pulsante rosso)
                    val x = if (dX < -300f) -300f else dX // -300f è circa 100dp su molti schermi

                    //se l'utente ha rilasciato il dito (isCurrentlyActive è false)
                    //e la vista è già stata spostata a sinistra
                    if (!isCurrentlyActive && dX < -150f) {
                        //forza la posizione a -300f anche se il sistema vorrebbe tornare a 0
                        foregroundView.translationX = -300f
                        return //salto il disegno standard per evitare l'effetto elastico
                    }

                    getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, x, dY, actionState, isCurrentlyActive)
                }

                //quando l'uente solleva il dito il pulsante resta cliccabile, l'utente deve poter interagire con lo sfondo.
                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    val foregroundView = (viewHolder as SongAdapter.SongViewHolder).foregroundView

                    //se la vista è stata spostata, non chiamare il clearView standard
                    //che riporterebbe translationX a 0.
                    if (foregroundView.translationX == -300f) {
                        //non facciamo nulla, lasciamo la riga dov'è
                    } else {
                        super.clearView(recyclerView, viewHolder)
                    }
                }
            }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                //se la lista si muove (l'utente trascina o scorre)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    closeAllOpenMenus()//chiude il menu delete di tutti i brani
                }
            }
        })

        //collega il gestore alla RecyclerView
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    //funzione per animare il delete di un brano
    //quando viene cliccato "delete", l'animazione parte e cancella il brano
    private fun animateDeleteManual(viewHolder: SongAdapter.SongViewHolder, song: Song) {

        val rowView = viewHolder.itemView //animazione su tutta la riga, non solo l'icona
        val root = view as? ViewGroup ?: return

        //disabilitare il click per evitare che l'utente clicchi 10 volte
        viewHolder.buDelete.isEnabled = false

        //la distanza per cadere fuori dallo schermo
        val distanceToFall = root.height.toFloat()

        //disabilita il click per evitare doppi click durante l'animazione
        viewHolder.buDelete.isEnabled = false

        rowView.animate()
            .translationY(distanceToFall)
            .alpha(0f) //svanisce mentre cade
            .setDuration(500) //mezzo secondo
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                //eliminare dal database
                viewModel.deleteSong(song)

                //resetta la vista per quando verrà riciclata
                //senza di questo, la prossima canzone che userà questo ViewHolder sarà invisibile
                rowView.translationY = 0f
                rowView.alpha = 1f
                viewHolder.buDelete.isEnabled = true
            }
            .start()
    }

    //funzione che resetta lo stato di tutti i brani
    //quando l'utente apre il menu delete, se scorre la lista, chiude il menu delete
    private fun closeAllOpenMenus() {
        adapter.songs.forEachIndexed { index, song ->
            if (song.isMenuOpen) {
                song.isMenuOpen = false
                adapter.notifyItemChanged(index) //questo riporta la riga al suo posto
            }
        }
    }
}