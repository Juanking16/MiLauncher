package com.milauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    // Usamos mutableIntStateOf para que Compose reaccione a los cambios de estado
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent { 
            // Le pasamos solo el resumeTick. 
            // PantallaInicio se encarga del ViewModel y de crear Acciones.
            PantallaInicio(resumeTick = resumeTick) 
        }
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que vuelves al launcher (por ejemplo, al cerrar otra app),
        // incrementamos el contador para forzar la verificación de apps instaladas.
        resumeTick++
    }
}