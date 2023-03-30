package tech.nagual.phoenix.tools.organizer.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import me.zhanghai.android.files.filelist.FileListActivity
import me.zhanghai.android.files.util.createIntent
import java.io.File
import java.time.Instant

fun ActivityResultLauncher<None>.launch() {
    launch(null)
}

object None

object ChooseFilesContract : ActivityResultContract<None, List<Uri>>() {
    override fun createIntent(context: Context, input: None): Intent {
        return FileListActivity::class.createIntent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "*/*"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null || resultCode != Activity.RESULT_OK) return emptyList()

        val clipItemCount = intent.clipData?.itemCount ?: 0
        return listOfNotNull(intent.data) + (0 until clipItemCount).mapNotNull {
            intent.clipData?.getItemAt(it)?.uri
        }
    }
}

object ExportOrganizerContract : ActivityResultContract<None, Uri?>() {
    override fun createIntent(context: Context, input: None): Intent {
        return FileListActivity::class.createIntent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT_TREE
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

object ExportNotesContract : ActivityResultContract<None, Uri?>() {
    override fun createIntent(context: Context, input: None): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "backup_${Instant.now().epochSecond}.zip")
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

object RestoreOrganizerContract : ActivityResultContract<None, Uri?>() {
    override fun createIntent(context: Context, input: None): Intent {
        return FileListActivity::class.createIntent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

object DrawImageContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return tech.nagual.phoenix.tools.painter.PainterActivity::class.createIntent()
            .setAction(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

object EditImageContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return tech.nagual.phoenix.tools.organizer.photoeditor.PhotoEditorActivity::class.createIntent()
            .setAction(Intent.ACTION_EDIT).apply {
                data = input
                putExtra(MediaStore.EXTRA_OUTPUT, input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

object TakePhotoContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return tech.nagual.phoenix.tools.organizer.camera.ui.activities.CaptureActivity::class.createIntent()
            .setAction(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

object TakePhotosContract : ActivityResultContract<File?, ArrayList<Uri>?>() {
    override fun createIntent(context: Context, path: File?): Intent {
        return tech.nagual.phoenix.tools.organizer.camera.ui.activities.MultiCaptureActivity::class.createIntent()
            .setAction(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ArrayList<Uri>? {
        val photoUris = intent?.getParcelableArrayListExtra<Uri>("photos")
        if (resultCode == Activity.RESULT_OK && photoUris != null) {
            return photoUris
        }
        return null
    }
}

object TakeVideoContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return tech.nagual.phoenix.tools.organizer.camera.ui.activities.VideoCaptureActivity::class.createIntent()
            .setAction(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}
