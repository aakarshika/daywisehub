package com.example.todoapp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.todoapp.app.App
import com.example.todoapp.app.AppClock
import com.example.todoapp.db.sample.SampleSeed
import com.example.todoapp.db.sample.ShotOptions
import kotlinx.datetime.LocalDate
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi

// The dev clock shows in debug frameworks only. Debug frameworks also read launch arguments
// (see scripts/seed-sample-ios.sh and scripts/shot-ios.sh): `-sample <name>` seeds an empty database,
// `-hideDevClock YES` hides the pill, `-shotDate yyyy-MM-dd` makes that day today, and the `-shot…`
// arguments pick a screenshot scene (ShotOptions).
@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController {
    val debug = Platform.isDebugBinary
    val defaults = NSUserDefaults.standardUserDefaults
    if (debug) {
        defaults.stringForKey("shotDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let(AppClock::startOn)
        ShotOptions.read { defaults.stringForKey(it) }
    }
    return ComposeUIViewController {
        App(
            sample = if (debug) SampleSeed.from(defaults.stringForKey("sample")) else null,
            devTools = debug && !defaults.boolForKey("hideDevClock"),
        )
    }
}
