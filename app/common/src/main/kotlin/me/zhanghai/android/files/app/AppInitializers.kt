package me.zhanghai.android.files.app

import me.zhanghai.android.files.features.ftpserver.ftpServerServiceNotificationTemplate
import me.zhanghai.android.files.filejob.fileJobNotificationTemplate
import tech.nagual.app.application
import tech.nagual.app.notificationManagerCompat

fun createNotificationChannels() {
    notificationManagerCompat.createNotificationChannels(
        listOf(
            backgroundActivityStartNotificationTemplate.channelTemplate,
            fileJobNotificationTemplate.channelTemplate,
            ftpServerServiceNotificationTemplate.channelTemplate,
        ).map { it.create(application) }
    )
}