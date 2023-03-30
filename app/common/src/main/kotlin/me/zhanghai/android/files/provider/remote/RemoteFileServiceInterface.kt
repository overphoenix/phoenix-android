package me.zhanghai.android.files.provider.remote

import java8.nio.file.FileSystem
import me.zhanghai.android.files.provider.FileSystemProviders
import me.zhanghai.android.files.provider.archive.archiveRefresh

open class RemoteFileServiceInterface : IRemoteFileService.Stub() {
    override fun getRemoteFileSystemProviderInterface(scheme: String): IRemoteFileSystemProvider =
        RemoteFileSystemProviderInterface(me.zhanghai.android.files.provider.FileSystemProviders[scheme])

    override fun getRemoteFileSystemInterface(fileSystem: ParcelableObject): IRemoteFileSystem =
        RemoteFileSystemInterface(fileSystem.value())

    override fun getRemotePosixFileStoreInterface(
        fileStore: ParcelableObject
    ): IRemotePosixFileStore = RemotePosixFileStoreInterface(fileStore.value())

    override fun getRemotePosixFileAttributeViewInterface(
        attributeView: ParcelableObject
    ): IRemotePosixFileAttributeView =
        RemotePosixFileAttributeViewInterface(attributeView.value())

    override fun refreshArchiveFileSystem(fileSystem: ParcelableObject) {
        fileSystem.value<FileSystem>().getPath("").archiveRefresh()
    }
}
