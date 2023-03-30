package me.zhanghai.android.files.provider.ftp

import java8.nio.file.Path
import me.zhanghai.android.files.provider.ftp.client.Authority

fun Authority.createFtpRootPath(): Path =
    FtpFileSystemProvider.getOrNewFileSystem(this).rootDirectory
