package com.milauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Activity HOME del launcher. La lógica vive en ui/ y data/;
 * esta clase solo inicializa Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.milauncher.ui.MiLauncherTheme {
                com.milauncher.ui.PantallaPrincipal(this@MainActivity)
            }
        }
    }
}
