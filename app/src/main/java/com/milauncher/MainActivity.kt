package com.milauncher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
        var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        LaunchedEffect(Unit) { apps = loadApps() }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(8.dp)
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
                        modifier = Modifier.size(52.dp)
                    )
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
}