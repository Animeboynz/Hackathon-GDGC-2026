package com.animeboynz.kmd

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var resumed = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val nfcConsumerStateListener: (Boolean) -> Unit = {
        updateNfcReaderMode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        NfcTagDispatcher.addConsumerStateListener(nfcConsumerStateListener)

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

            val startScreen = if (generalPreferences.digitalIdGenerated.get()) {
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
        resumed = true
        updateNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        disableNfcReaderMode()
    }

    override fun onDestroy() {
        NfcTagDispatcher.removeConsumerStateListener(nfcConsumerStateListener)
        super.onDestroy()
    }

    private fun updateNfcReaderMode() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { updateNfcReaderMode() }
            return
        }

        val adapter = nfcAdapter ?: return
        if (!resumed || NfcTagDispatcher.consumer == null) {
            disableNfcReaderMode()
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500)
        }
        adapter.enableReaderMode(
            this,
            { tag -> NfcTagDispatcher.consumer?.invoke(tag) },
            flags,
            options,
        )
    }

    private fun disableNfcReaderMode() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { disableNfcReaderMode() }
            return
        }

        runCatching { nfcAdapter?.disableReaderMode(this) }
    }
}
