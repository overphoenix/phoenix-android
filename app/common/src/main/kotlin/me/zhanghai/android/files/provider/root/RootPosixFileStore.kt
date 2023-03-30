package me.zhanghai.android.files.provider.root

import me.zhanghai.android.files.provider.common.PosixFileStore
import me.zhanghai.android.files.provider.remote.RemoteInterface
import me.zhanghai.android.files.provider.remote.RemotePosixFileStore

class RootPosixFileStore(fileStore: PosixFileStore) : RemotePosixFileStore(
    RemoteInterface { RootFileService.getRemotePosixFileStoreInterface(fileStore) }
)
