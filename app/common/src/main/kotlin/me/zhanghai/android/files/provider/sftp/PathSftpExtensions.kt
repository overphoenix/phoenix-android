package me.zhanghai.android.files.provider.sftp

import java8.nio.file.Path
import me.zhanghai.android.files.provider.sftp.client.Authority

fun Authority.createSftpRootPath(): Path =
    SftpFileSystemProvider.getOrNewFileSystem(this).rootDirectory
