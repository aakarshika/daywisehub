package com.example.todoapp.reminders

import com.example.todoapp.domain.Reminder
import platform.Foundation.NSDateComponents
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/** Local notifications, scheduled ahead with their text. iOS keeps at most 64 pending, so the soonest ones go first. */
class IosReminderScheduler : ReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()
    // The center holds its delegate weakly.
    private val delegate = ReminderDelegate()

    init {
        center.delegate = delegate
    }

    override fun askPermission() {
        // A sample scene (debug builds' `-sample`, for screenshots) starts without the prompt in the way.
        if (NSUserDefaults.standardUserDefaults.stringForKey("sample") != null) return
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    override fun replaceAll(reminders: List<Reminder>) {
        center.removeAllPendingNotificationRequests()
        reminders.take(MAX_PENDING).forEach { r ->
            val content = UNMutableNotificationContent().apply {
                setTitle(r.title)
                setBody(r.body)
                setSound(UNNotificationSound.defaultSound())
                setUserInfo(mapOf<Any?, Any?>(ReminderLinks.EXTRA_DATE to r.openDate.toString()))
            }
            val when_ = NSDateComponents().apply {
                year = r.at.year.toLong()
                month = r.at.monthNumber.toLong()
                day = r.at.dayOfMonth.toLong()
                hour = r.at.hour.toLong()
                minute = r.at.minute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(when_, repeats = false)
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(r.id, content, trigger), null)
        }
    }

    private companion object {
        const val MAX_PENDING = 60
    }
}

/** Shows reminders while the app is open too, and a tap opens the diary on the reminder's day. */
private class ReminderDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        ReminderLinks.open(didReceiveNotificationResponse.notification.request.content.userInfo[ReminderLinks.EXTRA_DATE] as? String)
        withCompletionHandler()
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound
        )
    }
}
