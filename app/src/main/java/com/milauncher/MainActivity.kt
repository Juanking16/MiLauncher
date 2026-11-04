package com.milauncher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }

    private suspend fun loadApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppItem(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                    icon = it.loadIcon(packageManager).toBitmap(128, 128).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun openApp(app: AppItem) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    @Composable
    fun HomeScreen() {
        val store = remember { SettingsStore(applicationContext) }
        val ajustes by store.flow.collectAsState(initial = Ajustes())
        val scope = rememberCoroutineScope()
        var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        var verAjustes by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { apps = loadApps() }
        BackHandler(enabled = verAjustes) { verAjustes = false }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ajustes.fondoOscuro))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (verAjustes) {
                PantallaAjustes(
                    ajustes = ajustes,
                    onCambio = { nuevo -> scope.launch { store.guardar(nuevo) } },
                    onCerrar = { verAjustes = false }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(ajustes.columnas),
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(apps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { openApp(app) }
                                .padding(vertical = 10.dp)
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier.size(ajustes.tamanoIcono.dp)
                            )
                            if (ajustes.mostrarNombres) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "⚙",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { verAjustes = true }
                        .padding(14.dp)
                )
            }
        }
    }

    @Composable
    fun PantallaAjustes(
        ajustes: Ajustes,
        onCambio: (Ajustes) -> Unit,
        onCerrar: () -> Unit
    ) {
        var columnas by remember { mutableStateOf(ajustes.columnas.toFloat()) }
        var tamano by remember { mutableStateOf(ajustes.tamanoIcono.toFloat()) }
        var fondo by remember { mutableStateOf(ajustes.fondoOscuro) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "← Ajustes",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onCerrar() }.padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text("Columnas: ${columnas.toInt()}", color = Color.White)
            Slider(
                value = columnas,
                onValueChange = { columnas = it },
                onValueChangeFinished = { onCambio(ajustes.copy(columnas = columnas.toInt())) },
                valueRange = 3f..6f,
                steps = 2
            )

            Text("Tamaño de iconos: ${tamano.toInt()}", color = Color.White)
            Slider(
                value = tamano,
                onValueChange = { tamano = it },
                onValueChangeFinished = { onCambio(ajustes.copy(tamanoIcono = tamano.toInt())) },
                valueRange = 36f..72f
            )

            Text("Oscuridad del fondo: ${(fondo * 100).toInt()}%", color = Color.White)
            Slider(
                value = fondo,
                onValueChange = { fondo = it },
                onValueChangeFinished = { onCambio(ajustes.copy(fondoOscuro = fondo)) },
                valueRange = 0f..0.9f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar nombres", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = ajustes.mostrarNombres,
                    onCheckedChange = { onCambio(ajustes.copy(mostrarNombres = it)) }
                )
            }
        }
    }
}