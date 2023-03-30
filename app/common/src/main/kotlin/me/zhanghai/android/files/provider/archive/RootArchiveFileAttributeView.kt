package me.zhanghai.android.files.provider.archive

import java8.nio.file.Path
import me.zhanghai.android.files.provider.common.PosixFileAttributeView
import me.zhanghai.android.files.provider.common.PosixFileAttributes
import me.zhanghai.android.files.provider.root.RootPosixFileAttributeView
import java.io.IOException

internal class RootArchiveFileAttributeView(
    attributeView: PosixFileAttributeView,
    private val path: Path
) : RootPosixFileAttributeView(attributeView) {
    @Throws(IOException::class)
    override fun readAttributes(): PosixFileAttributes {
        ArchiveFileSystemProvider.doRefreshIfNeeded(path)
        return super.readAttributes()
    }
}
