package com.milauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    private lateinit var acciones: Acciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acciones = Acciones(this)
        setContent { PantallaInicio(acciones, resumeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}