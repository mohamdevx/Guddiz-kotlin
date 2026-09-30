package com.guddiz.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.guddiz.app.core.DebugAudit
import com.guddiz.app.core.DebugSeeder
import com.guddiz.app.ui.GuddizApp
import com.guddiz.app.ui.theme.GuddizTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            val prefs = getSharedPreferences("debug_seed", Context.MODE_PRIVATE)
            lifecycleScope.launch {
                val seeded = prefs.getBoolean("seeded_v2", false)
                if (!seeded) {
                    prefs.edit().putBoolean("seeded_v2", true).apply()
                    try {
                        DebugSeeder.seed(applicationContext)
                    } catch (_: Exception) { }
                }
                // Give Room a moment to flush, then audit
                delay(1500L)
                try {
                    DebugAudit.run(applicationContext)
                } catch (_: Exception) { }
            }
        }

        setContent {
            GuddizTheme { GuddizApp() }
        }
    }
}
