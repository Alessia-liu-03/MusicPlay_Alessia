package it.unipd.dei.alessia.MusicPlay_Alessia

import android.content.Context
import androidx.fragment.app.FragmentManager

//gestisce la navigazione verso il player

class Navigator(
    private val fragmentManager: FragmentManager,
    private val context: Context
) {

    fun navigateToPlayer() {
        val isTablet = context.resources.getBoolean(R.bool.is_tablet)
        val playerFragment = PlayerFragment()

        val transaction = fragmentManager.beginTransaction()

        if (isTablet) {
            //gestione tablet, nessun pulsante home, nessuna navigazione
            transaction.replace(R.id.player_container, playerFragment)
        } else {
            //gestione smartphone con il pulsante "home"
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            transaction.replace(R.id.fragment_container, playerFragment)
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }
}