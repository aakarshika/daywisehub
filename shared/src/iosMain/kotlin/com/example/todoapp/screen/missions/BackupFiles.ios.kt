@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.todoapp.screen.missions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

@Composable
actual fun rememberBackupFiles(onSaved: (Boolean) -> Unit, onOpened: (String?) -> Unit): BackupFiles {
    val saved by rememberUpdatedState(onSaved)
    val opened by rememberUpdatedState(onOpened)
    return remember { IosBackupFiles({ saved(it) }, { opened(it) }) }
}

/** Save goes through the share sheet, so the file can go to Files, AirDrop, Mail or a chat app. */
private class IosBackupFiles(private val onSaved: (Boolean) -> Unit, onOpened: (String?) -> Unit) : BackupFiles {
    // The picker holds its delegate weakly, so this keeps it alive.
    private val delegate = PickerDelegate(onOpened)

    @OptIn(BetaInteropApi::class)
    override fun save(fileName: String, text: String) {
        val path = NSTemporaryDirectory() + fileName
        val written = NSString.create(string = text).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!written) {
            onSaved(false)
            return
        }
        val top = topController() ?: return
        val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
        sheet.popoverPresentationController?.sourceView = top.view
        sheet.completionWithItemsHandler = { _, completed, _, error ->
            if (completed) onSaved(true) else if (error != null) onSaved(false)
        }
        top.presentViewController(sheet, animated = true, completion = null)
    }

    override fun open() {
        val top = topController() ?: return
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON, UTTypeData), asCopy = true)
        picker.delegate = delegate
        top.presentViewController(picker, animated = true, completion = null)
    }

    private fun topController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }
}

private class PickerDelegate(private val onOpened: (String?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        onOpened(NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null))
    }
}
