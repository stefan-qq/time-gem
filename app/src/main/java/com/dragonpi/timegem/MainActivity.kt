package com.dragonpi.timegem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dragonpi.timegem.app.TimeGemRoot
import com.dragonpi.timegem.data.preferences.AppPreferencesRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = AppPreferencesRepository(applicationContext)

        setContent {
            TimeGemRoot(preferencesRepository = preferencesRepository)
        }
    }
}
