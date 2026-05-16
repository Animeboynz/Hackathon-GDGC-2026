package com.animeboynz.kmd

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.animeboynz.kmd.nfc.NfcTagDispatcher
import com.animeboynz.kmd.preferences.GeneralPreferences
import com.animeboynz.kmd.preferences.preference.collectAsState
import com.animeboynz.kmd.presentation.components.preferences.TachiyomiTheme
import com.animeboynz.kmd.ui.home.HomeScreen
import com.animeboynz.kmd.ui.onboarding.PassportOnboardingScreen
import com.animeboynz.kmd.utils.FirebaseConfig
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private val generalPreferences: GeneralPreferences by inject()

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private val nfcTechLists = arrayOf(arrayOf(IsoDep::class.java.name))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val nfcIntent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, flags)

        handleNfcIntent(intent)

        FirebaseConfig.init(applicationContext)
        FirebaseConfig.setAnalyticsEnabled(true)
        FirebaseConfig.setCrashlyticsEnabled(true)

        setContent {
            val themeMode by appearancePreferences.themeMode.collectAsState()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(themeMode, isSystemInDarkTheme) {
                val lightStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb())
                val darkStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
                enableEdgeToEdge(
                    statusBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }

            val startScreen = if (generalPreferences.passportOnboardingCompleted.get()) {
                HomeScreen
            } else {
                PassportOnboardingScreen
            }

            TachiyomiTheme() {
                Navigator(screen = startScreen) {
                    SlideTransition(navigator = it)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED,
            -> {
                val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                }
                if (tag != null) {
                    NfcTagDispatcher.consumer?.invoke(tag)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (NfcTagDispatcher.consumer != null && nfcAdapter != null && nfcPendingIntent != null) {
            nfcAdapter?.enableForegroundDispatch(
                this,
                nfcPendingIntent,
                null,
                nfcTechLists,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
}
