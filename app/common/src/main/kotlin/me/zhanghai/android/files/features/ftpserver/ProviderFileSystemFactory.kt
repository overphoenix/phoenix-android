package me.zhanghai.android.files.features.ftpserver

import org.apache.ftpserver.ftplet.FileSystemFactory
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.User

class ProviderFileSystemFactory : FileSystemFactory {
    override fun createFileSystemView(user: User): FileSystemView = ProviderFileSystemView(user)
}
