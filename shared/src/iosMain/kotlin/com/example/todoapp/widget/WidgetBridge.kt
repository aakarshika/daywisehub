package com.example.todoapp.widget

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import co.touchlab.kermit.Logger
import com.example.todoapp.di.KoinF
import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.PillarType
import com.example.todoapp.reminders.ReminderLinks
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * What the iPhone app and its widget extension call from Swift: the widget data, the art as PNG
 * data at 3 pixels per point, and the app's links.
 */
object WidgetBridge {

    fun data(): WidgetData {
        KoinF.setupKoin()
        return KoinF.di!!.get()
    }

    /** Today's snapshot, or null when it can't be read; nothing thrown reaches Swift. */
    suspend fun snapshot(): WidgetSnapshot? = quietly { data().snapshot() }

    suspend fun toggleTask(taskId: Long) { quietly { data().toggleTask(taskId) } }
    suspend fun addGlass() { quietly { data().addGlass() } }
    suspend fun toggleMood(icon: String) { quietly { data().toggleMood(icon) } }

    /** The app, on coming back to the front: shows what the widgets changed meanwhile. */
    suspend fun touchToday() { quietly { data().touchToday() } }

    private suspend fun <T> quietly(block: suspend () -> T): T? = try {
        block()
    } catch (e: Throwable) {
        Logger.w(e) { "A widget call failed." }
        null
    }

    /** The app sets this so its own changes reload the widgets (WidgetCenter is Swift-only). */
    fun onChange(refresh: () -> Unit) {
        WidgetHooks.refresh = refresh
    }

    /** inbalance://day/yyyy-MM-dd and inbalance://metrics, from a widget tap. */
    fun open(url: String) {
        val path = url.substringAfter("://")
        when {
            path.startsWith("metrics") -> ReminderLinks.openTab(ReminderLinks.TAB_METRICS)
            path.startsWith("day/") -> ReminderLinks.open(path.removePrefix("day/"))
        }
    }

    /** Next up's saved place; see NextUp. */
    fun nextUpIndex(saved: String?, dateKey: String, count: Int): Int = NextUp.index(saved, dateKey, count)
    fun nextUpStep(saved: String?, dateKey: String, count: Int, by: Int): String = NextUp.step(saved, dateKey, count, by)

    fun seeSaw(snapshot: WidgetSnapshot): NSData = png(WidgetArt.seeSaw(snapshot.pillars, snapshot.tilt, SCALE))
    fun cheer(): NSData = png(WidgetArt.cheer(SCALE))
    fun rest(size: Float): NSData = png(WidgetArt.rest(size, SCALE))
    fun friend(pillar: WidgetPillar, size: Float): NSData = png(WidgetArt.friend(pillar, size, SCALE))
    fun taskFriend(pillar: PillarType, share: Float, size: Float): NSData = png(WidgetArt.friend(pillar, share, false, 0.6f, size, SCALE))
    fun sticker(badge: Badge, dateKey: String, size: Float): NSData = png(WidgetArt.sticker(badge, dateKey, size, SCALE))
    fun mood(mood: WidgetMood, size: Float): NSData = png(WidgetArt.mood(mood, size, SCALE))
    fun glass(index: Int, filled: Boolean, size: Float): NSData = png(WidgetArt.glass(index, filled, size, SCALE))
    fun crown(size: Float): NSData = png(WidgetArt.crown(size, SCALE))
    fun emptySlot(size: Float): NSData = png(WidgetArt.emptySlot(size, SCALE))

    private const val SCALE = 3f

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun png(bitmap: ImageBitmap): NSData {
        val bytes = Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)
        if (bytes.isEmpty()) return NSData()
        return bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    }
}
