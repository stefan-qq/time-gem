package com.dragonpi.timegem.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.dragonpi.timegem.MainActivity
import com.dragonpi.timegem.R
import com.dragonpi.timegem.data.preferences.*
import com.dragonpi.timegem.data.notes.NotesRepository
import com.dragonpi.timegem.data.notes.orderNotes
import com.dragonpi.timegem.ui.theme.timeGemColorScheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NoteWidget : QuickCaptureWidget()

open class QuickCaptureWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { update(context) }
                catch (failure: Exception) { android.util.Log.w("TimeGemWidget", "Could not update widget", failure) }
                finally { pending.finish() }
            }
        }
    }
    companion object {
        const val NEW_NOTE = "com.dragonpi.timegem.NEW_NOTE"
        const val NEW_IMAGE = "com.dragonpi.timegem.NEW_IMAGE"
        const val NEW_AUDIO = "com.dragonpi.timegem.NEW_AUDIO"
        const val OPEN_NOTE = "com.dragonpi.timegem.OPEN_NOTE"
        fun refresh(context: Context) {
            context.sendBroadcast(Intent(context, QuickCaptureWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE))
        }
        private suspend fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val captureIds = manager.getAppWidgetIds(ComponentName(context, QuickCaptureWidget::class.java))
            val noteIds = manager.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            if (captureIds.isEmpty() && noteIds.isEmpty()) return
            val preferences = withTimeout(3000) { AppPreferencesRepository(context).preferences.first() }
            val appearance = preferences.appearance
            val dark = when (appearance.mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            val colors = timeGemColorScheme(context, appearance, dark)
            fun action(name: String, noteId: String? = null) = PendingIntent.getActivity(context, 0,
                Intent(context, MainActivity::class.java).setAction(name).setData(noteId?.let { android.net.Uri.parse("timegem://note/$it") }).putExtra("note_id", noteId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val themed = preferences.interactions.themedLogo && appearance.source != ColorSource.TIME_GEM
            val drawable = androidx.core.content.ContextCompat.getDrawable(context,
                if (themed) R.drawable.ic_search_mascot else R.drawable.ic_time_gem_logo)!!.mutate()
            if (themed) drawable.setTint(colors.primary.toArgb())
            val size = (48 * context.resources.displayMetrics.density).toInt()
            val mascot = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val ratio = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.coerceAtLeast(1)
            val height = (size * ratio).toInt().coerceIn(1, size)
            drawable.setBounds(0, (size - height) / 2, size, (size + height) / 2)
            drawable.draw(android.graphics.Canvas(mascot))
            captureIds.forEach { widgetId ->
                val options = manager.getAppWidgetOptions(widgetId)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                val widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64)
                val tall = widgetHeight > 140 && width < 180
                val views = RemoteViews(context.packageName, if (tall) R.layout.widget_quick_capture_tall else R.layout.widget_quick_capture)
                views.setInt(R.id.widget_background, "setColorFilter", colors.surfaceContainerHigh.toArgb())
                views.setImageViewBitmap(R.id.widget_mascot, mascot)
                views.setOnClickPendingIntent(R.id.widget_mascot, action("com.dragonpi.timegem.OPEN_NOTES"))
                views.setOnClickPendingIntent(R.id.widget_root, action("com.dragonpi.timegem.OPEN_NOTES"))
                val room = if (tall) widgetHeight else width
                views.setViewVisibility(R.id.widget_mascot, if (room < 112) android.view.View.GONE else android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_image, if (room < 180) android.view.View.GONE else android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_audio, if (room < 228) android.view.View.GONE else android.view.View.VISIBLE)
                listOf(R.id.widget_new to NEW_NOTE, R.id.widget_image to NEW_IMAGE, R.id.widget_audio to NEW_AUDIO).forEach { (id, name) ->
                    views.setInt(id, "setColorFilter", colors.primary.toArgb()); views.setOnClickPendingIntent(id, action(name))
                }
                manager.updateAppWidget(widgetId, views)
            }
            if (noteIds.isNotEmpty()) {
                val repository = NotesRepository(context)
                val note = try { orderNotes(repository.load(), NoteSort.MODIFIED).firstOrNull() } finally { repository.close() }
                val views = RemoteViews(context.packageName, R.layout.widget_note)
                views.setInt(R.id.widget_background, "setColorFilter", colors.surfaceContainerHigh.toArgb())
                views.setImageViewBitmap(R.id.widget_mascot, mascot)
                views.setTextColor(R.id.widget_note_title, colors.onSurface.toArgb())
                views.setTextColor(R.id.widget_note_body, colors.onSurface.toArgb())
                views.setTextColor(R.id.widget_hint, colors.onSurfaceVariant.toArgb())
                views.setTextViewText(R.id.widget_note_title, note?.displayTitle ?: "Room for an idea")
                val body = note?.body?.takeIf { it.isNotBlank() }?.take(1200)
                    ?: note?.attachments?.filter { it.type == com.dragonpi.timegem.data.notes.AttachmentType.AUDIO }?.joinToString("\n") { it.name }?.take(600).orEmpty()
                views.setTextViewText(R.id.widget_note_body, body)
                views.setViewVisibility(R.id.widget_note_body, if (body.isBlank()) android.view.View.GONE else android.view.View.VISIBLE)
                val image = note?.attachments?.firstOrNull { it.type == com.dragonpi.timegem.data.notes.AttachmentType.IMAGE }?.let { attachment ->
                    runCatching {
                        val file = com.dragonpi.timegem.data.notes.AttachmentStore(context).file(note.id, attachment)
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(file.path, bounds)
                        val sample = android.graphics.BitmapFactory.Options().apply {
                            inSampleSize = 1
                            while (bounds.outWidth / inSampleSize > 400 || bounds.outHeight / inSampleSize > 400) inSampleSize *= 2
                        }
                        android.graphics.BitmapFactory.decodeFile(file.path, sample)
                    }.getOrNull()
                }
                views.setViewVisibility(R.id.widget_note_image, if (image == null) android.view.View.GONE else android.view.View.VISIBLE)
                if (image != null) views.setImageViewBitmap(R.id.widget_note_image, image)

                views.setTextViewText(R.id.widget_hint, if (note == null) "Tap to write a note" else if (note.pinned) "Pinned - Tap to open" else "Latest note - Tap to open")
                views.setOnClickPendingIntent(R.id.widget_root, if (note == null) action(NEW_NOTE) else action(OPEN_NOTE, note.id))
                noteIds.forEach { widgetId ->
                    @Suppress("DEPRECATION")
                    val noteViews = if (android.os.Build.VERSION.SDK_INT >= 28) RemoteViews(views) else views.clone()
                    val widgetHeight = manager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
                    noteViews.setViewVisibility(R.id.widget_mascot, if (widgetHeight < 140) android.view.View.GONE else android.view.View.VISIBLE)
                    noteViews.setViewVisibility(R.id.widget_hint, if (widgetHeight < 130) android.view.View.GONE else android.view.View.VISIBLE)
                    noteViews.setInt(R.id.widget_note_title, "setMaxLines", if (widgetHeight < 140) 1 else 2)
                    manager.updateAppWidget(widgetId, noteViews)
                }
            }
        }
    }
}
