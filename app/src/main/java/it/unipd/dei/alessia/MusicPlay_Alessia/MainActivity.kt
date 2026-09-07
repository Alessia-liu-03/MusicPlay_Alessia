package it.unipd.dei.alessia.MusicPlay_Alessia

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //gestione dei permessi per le notifiche (Android 13+)
        checkNotificationPermission()

        // Se siamo su cellulare (dove abbiamo solo un fragment_container),
        // dobbiamo caricare manualmente il primo Fragment (la lista).
        if (savedInstanceState == null) {
            val smartphoneContainer = findViewById<View>(R.id.fragment_container)
            if (smartphoneContainer != null) {
                 
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SongListFragment())
                    .commit()
            }else{
            //se siamo su tablet, FragmentContainerView nel layout-sw720dp lo fa già da solo.
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val REQUEST_CODE = 21983
    }
}