package com.milauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Repositorio de aplicaciones instaladas. Respeta la visibilidad de paquetes del sistema. */
class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(LauncherApps::class.java)

    /** Devuelve las apps con actividad LAUNCHER visibles para este perfil. */
    suspend fun cargarApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val items = mutableListOf<AppItem>()
        if (launcherApps != null) {
            for (profile in launcherApps.profiles) {
                val actividades = launcherApps.getActivityList(null, profile)
                for (act in actividades) {
                    try {
                        items += AppItem(
                            label = act.label.toString(),
                            packageName = act.componentName.packageName,
                            activityName = act.componentName.className,
                            icon = iconoDe(act.getIcon(0), pm)
                        )
                    } catch (_: Exception) { /* app desinstalada a mitad de lectura */ }
                }
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .forEach {
                    items += AppItem(
                        label = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        activityName = it.activityInfo.name,
                        icon = iconoDe(it.loadIcon(pm), pm)
                    )
                }
        }
        items.filter { it.packageName != context.packageName || it.activityName != "MainActivity" }
            .sortedBy { it.label.lowercase() }
    }

    private fun iconoDe(drawable: Drawable?, pm: PackageManager): Bitmap {
        val fallback = pm.defaultActivityIcon
        return (drawable ?: fallback).toBitmap(128, 128)
    }

    fun abrir(app: AppItem) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = android.content.ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        context.startActivity(intent)
    }
}
