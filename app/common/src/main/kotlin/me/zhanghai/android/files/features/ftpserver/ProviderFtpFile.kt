package me.zhanghai.android.files.features.ftpserver

import java8.nio.file.Path
import java8.nio.file.StandardOpenOption
import java8.nio.file.attribute.FileTime
import java8.nio.file.attribute.PosixFileAttributeView
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.User
import org.apache.ftpserver.usermanager.impl.WriteRequest
import me.zhanghai.android.files.provider.common.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class ProviderFtpFile(
    private val path: Path,
    private val relativePath: Path,
    private val user: User
) : Comparable<ProviderFtpFile>, FtpFile {
    override fun getAbsolutePath(): String {
        val path = relativePath.toString()
        return "/$path"
    }

    override fun getName(): String {
        val name = relativePath.fileName.toString()
        return if (name.isNotEmpty()) name else "/"
    }

    override fun isHidden(): Boolean = false

    override fun isDirectory(): Boolean = path.isDirectory()

    override fun isFile(): Boolean = path.isRegularFile()

    override fun doesExist(): Boolean = path.exists()

    override fun isReadable(): Boolean = path.isReadable

    override fun isWritable(): Boolean {
        if (user.authorize(WriteRequest(absolutePath)) == null) {
            return false
        }
        return !path.exists() || path.isWritable
    }

    override fun isRemovable(): Boolean {
        if (relativePath.nameCount == 1 && relativePath.getName(0).toString().isEmpty()) {
            return false
        }
        if (user.authorize(WriteRequest(absolutePath)) == null) {
            return false
        }
        return path.parent.isWritable
    }

    override fun getOwnerName(): String =
        try {
            path.getOwner().name
        } catch (ignored: UnsupportedOperationException) {
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } ?: "user"

    override fun getGroupName(): String {
        val attributeView = path.getFileAttributeView(PosixFileAttributeView::class.java)
        return if (attributeView != null) {
            try {
                attributeView.readAttributes().group().name
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        } ?: "group"
    }

    override fun getLinkCount(): Int = if (isDirectory) 3 else 1

    override fun getLastModified(): Long =
        try {
            path.getLastModifiedTime().toMillis()
        } catch (e: IOException) {
            e.printStackTrace()
            0
        }

    override fun setLastModified(time: Long): Boolean =
        if (!isWritable) {
            false
        } else {
            try {
                path.setLastModifiedTime(FileTime.fromMillis(time))
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

    override fun getSize(): Long =
        try {
            path.size()
        } catch (e: IOException) {
            e.printStackTrace()
            0
        }

    override fun getPhysicalFile(): Path = path

    override fun mkdir(): Boolean =
        if (!isWritable) {
            false
        } else {
            try {
                path.createDirectory()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

    override fun delete(): Boolean =
        if (!isRemovable) {
            false
        } else {
            try {
                path.delete()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

    override fun move(destination: FtpFile): Boolean {
        if (!(isRemovable && destination.isWritable)) {
            return false
        }
        val targetPath = (destination as ProviderFtpFile).path
        return try {
            path.moveTo(targetPath)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun listFiles(): List<ProviderFtpFile>? {
        val directoryStream = try {
            path.newDirectoryStream()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return directoryStream.map {
            ProviderFtpFile(path.resolve(it), relativePath.resolve(it), user)
        }.sorted()
    }

    @Throws(IOException::class)
    override fun createOutputStream(offset: Long): OutputStream {
        if (!isWritable) {
            throw IOException("Not writable: $absolutePath")
        }
        return if (offset == 0L) {
            path.newOutputStream()
        } else {
            val channel = path.newByteChannel(StandardOpenOption.WRITE)
            var successful = false
            try {
                val size = channel.size()
                if (offset <= size) {
                    if (offset < size) {
                        channel.truncate(offset)
                    }
                    channel.position(offset)
                } else {
                    channel.position(offset - 1)
                    channel.write(ByteBuffer.allocate(1))
                }
                val outputStream = channel.newOutputStream()
                successful = true
                outputStream
            } finally {
                if (!successful) {
                    channel.close()
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun createInputStream(offset: Long): InputStream {
        return if (offset == 0L) {
            path.newInputStream()
        } else {
            val channel = path.newByteChannel()
            var successful = false
            try {
                channel.position(offset)
                val inputStream = channel.newInputStream()
                successful = true
                inputStream
            } finally {
                if (!successful) {
                    channel.close()
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as ProviderFtpFile
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()

    override fun compareTo(other: ProviderFtpFile): Int = path.compareTo(other.path)
}