package com.dragonpi.timegem

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dragonpi.timegem.app.TimeGemRoot
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository
import com.dragonpi.timegem.widget.QuickCaptureWidget
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var newNoteRequest by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readWidgetIntent(intent)
        val preferencesRepository = AppPreferencesRepository(applicationContext)
        var ready = false
        val content = window.decorView
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
        setContent {
            TimeGemRoot(preferencesRepository, onReady = { ready = true }, newNoteRequest = newNoteRequest,
                onNewNoteHandled = { newNoteRequest = null })
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readWidgetIntent(intent)
    }
    private fun readWidgetIntent(intent: Intent) {
        when (intent.action) {
            QuickCaptureWidget.NEW_NOTE -> newNoteRequest = "TEXT:${UUID.randomUUID()}"
            QuickCaptureWidget.NEW_IMAGE -> newNoteRequest = "IMAGE:${UUID.randomUUID()}"
            QuickCaptureWidget.NEW_AUDIO -> newNoteRequest = "AUDIO:${UUID.randomUUID()}"
            QuickCaptureWidget.OPEN_NOTE -> intent.getStringExtra("note_id")?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]{1,100}")) }?.let { newNoteRequest = "NOTE:$it:${UUID.randomUUID()}" }
        }
    }
}
