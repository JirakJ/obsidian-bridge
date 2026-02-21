package com.jakubjirak.obsidianbridge.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object ObsidianBridgeNotifications {
    private const val GROUP_ID = "Obsidian Bridge"

    fun notifyInfo(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

    fun notifyWarning(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.WARNING)
            .notify(project)
    }

    fun notifyError(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }
}
