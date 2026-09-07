package it.unipd.dei.alessia.MusicPlay_Alessia

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

//questa classe prende i dati della classe Song e li collega alla vista di layout

class SongAdapter(

    var songs: List<Song>,
    private val onClick: (Song) -> Unit,
    private val onDeleteClick: (SongViewHolder, Song) -> Unit,

    ) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    //questa classe tiene i riferimenti agli  elementi grafici di ogni riga
    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvArtist)
        val ivAlbumArt: ImageView = view.findViewById(R.id.ivAlbumArt)
        val buDelete: View = itemView.findViewById(R.id.buDelete)
        val foregroundView: View = itemView.findViewById(R.id.foreground_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
         
        holder.tvArtist.text = song.artist

        //trasformo il titolo da formato html a quello normale
        val cleanTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(song.title, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(song.title).toString()
        }
        holder.tvTitle.text = cleanTitle

        //caricamento immagine con Glide
        //se imageResId=0 (file esterno), allora prendiamo l'immagine da imagePath
        //se imageResId!=0 (file interno), allora prendiamo l'immagine da imageResId
        val imageSource = if (song.imageResId != 0) song.imageResId else song.imagePath

        Glide.with(holder.itemView.context)
            .load(imageSource)
            .placeholder(R.drawable.image)//mette un/icona di default se non riesce a caricare l'immagine
            .centerCrop()
            .into(holder.ivAlbumArt)

        //se isMenuOpen è true, spostiamo la vista a sinistra
        if (song.isMenuOpen) {
            holder.foregroundView.translationX = -300f
 
        } else {
            holder.foregroundView.translationX = 0f
        }

        //click sul brano
        holder.foregroundView.setOnClickListener {
            if (holder.foregroundView.translationX < 0) {
                //se è aperta, la chiudiamo invece di far partire la musica
                holder.foregroundView.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .start()
                song.isMenuOpen = false
            } else {
                onClick(song) //suona il brano
            }
        }

        //click sul cestino
        holder.buDelete.setOnClickListener {  
            onDeleteClick(holder, song) //elimina song dalla lista (dal Database)
        }
    }

    //metodo per aggiornare la lista quando il db cambia
    @SuppressLint("NotifyDataSetChanged")
    fun setData(newList: List<Song>) {
        //puliamo la vecchia lista (se l'adapter ne ha una interna
        this.songs = newList

        // notifichiamo all'Adapter che i dati sono cambiati
        // per fargli rinfrescare la vista
        notifyDataSetChanged()
    }

    override fun getItemCount() = songs.size
}