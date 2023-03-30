package tech.nagual.phoenix.tools.organizer.photoeditor

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.SeekBar
import tech.nagual.common.extensions.*
import tech.nagual.common.helpers.PERMISSION_WRITE_STORAGE
import tech.nagual.common.helpers.SAVE_DISCARD_PROMPT_INTERVAL
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerPhotoeditorMainActivityBinding
import tech.nagual.phoenix.tools.organizer.photoeditor.dialogs.SaveImageDialog
import tech.nagual.phoenix.tools.organizer.photoeditor.models.Svg
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.dialogs.ColorPickerDialog
import tech.nagual.common.dialogs.ConfirmationAdvancedDialog
import tech.nagual.common.extensions.*
import tech.nagual.common.models.FileDirItem
import tech.nagual.common.helpers.config
import tech.nagual.common.helpers.PAINT_JPG
import tech.nagual.common.helpers.PAINT_PNG
import tech.nagual.common.helpers.PAINT_SVG
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

class PhotoEditorActivity : tech.nagual.common.activities.BaseSimpleActivity(), PainterCanvas.Listener {
    private lateinit var binding: OrganizerPhotoeditorMainActivityBinding

    private val SAVE_IMAGE_INTENT = 2

    private val FOLDER_NAME = "images"
    private val FILE_NAME = "simple-draw.png"
    private val BITMAP_PATH = "bitmap_path"
    private val URI_TO_LOAD = "uri_to_load"

    private var defaultPath = ""
    private var defaultFilename = ""

    private var defaultExtension = PAINT_PNG
    private var intentUri: Uri? = null
    private var uriToLoad: Uri? = null
    private var color = 0
    private var brushSize = 0f
    private var savedPathsHash = 0L
    private var lastSavePromptTS = 0L
    private var isEditIntent = false
    private var lastBitmapPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OrganizerPhotoeditorMainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appLaunched()

        binding.myCanvas.mListener = this
        binding.strokeWidthBar.setOnSeekBarChangeListener(onStrokeWidthBarChangeListener)

        setBackgroundColor(Color.BLACK)
        setColor(config.paintBrushColor)
        defaultPath = config.paintLastSaveFolder
        defaultExtension = config.paintLastSaveExtension

        brushSize = config.paintBrushSize
        updateBrushSize()
        binding.strokeWidthBar.progress = brushSize.toInt()

        binding.colorPicker.setOnClickListener { pickColor() }
        binding.undo.setOnClickListener { binding.myCanvas.undo() }
        binding.redo.setOnClickListener { binding.myCanvas.redo() }
        binding.confirm.setOnClickListener { confirmImage() }
        checkIntents()
    }

    override fun onResume() {
        super.onResume()

        binding.myCanvas.setAllowZooming(true)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        config.paintBrushColor = color
        config.paintBrushSize = brushSize
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.myCanvas.mListener = null
    }

    override fun onBackPressed() {
        val hasUnsavedChanges = savedPathsHash != binding.myCanvas.getDrawingHashCode()
        if (hasUnsavedChanges && System.currentTimeMillis() - lastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL) {
            lastSavePromptTS = System.currentTimeMillis()
            ConfirmationAdvancedDialog(
                this,
                "",
                R.string.save_before_closing,
                R.string.save,
                R.string.discard
            ) {
                if (it) {
                    trySaveImage()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == SAVE_IMAGE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            if (defaultExtension == PAINT_SVG) {
                Svg.saveToOutputStream(this, outputStream, binding.myCanvas)
            } else {
                saveToOutputStream(outputStream, defaultExtension.getCompressionFormat(), false)
            }
            savedPathsHash = binding.myCanvas.getDrawingHashCode()
        }
    }

    private fun checkIntents() {
        if (intent?.action == Intent.ACTION_EDIT) {
            val data = intent.data
            val output = intent.extras?.get(MediaStore.EXTRA_OUTPUT)
            if (data != null && output != null && output is Uri) {
                tryOpenUri(data, intent)
                isEditIntent = true
                intentUri = output
            }
        }
    }

    private fun getStoragePermission(callback: () -> Unit) {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                callback()
            } else {
                toast(R.string.no_storage_permissions)
            }
        }
    }

    private fun tryOpenUri(uri: Uri, intent: Intent) = when (uri.scheme) {
        "file" -> {
            uriToLoad = uri
            openPath(uri.path!!)
        }
        "content" -> {
            uriToLoad = uri
            openUri(uri, intent)
        }
        else -> false
    }

    private fun openPath(path: String) = when {
        path.endsWith(".svg") -> {
            binding.myCanvas.mBackgroundBitmap = null
            Svg.loadSvg(this, File(path), binding.myCanvas)
            defaultExtension = PAINT_SVG
            true
        }
        File(path).isImageSlow() -> {
            lastBitmapPath = path
            binding.myCanvas.drawBitmap(this, path)
            defaultExtension = PAINT_JPG
            true
        }
        else -> {
            toast(R.string.invalid_file_format)
            false
        }
    }

    private fun openUri(uri: Uri, intent: Intent): Boolean {
        val mime = MimeTypeMap.getSingleton()
        val type = mime.getExtensionFromMimeType(contentResolver.getType(uri)) ?: intent.type
        ?: contentResolver.getType(uri)
        return when (type) {
            "svg", "image/svg+xml" -> {
                binding.myCanvas.mBackgroundBitmap = null
                Svg.loadSvg(this, uri, binding.myCanvas)
                defaultExtension = PAINT_SVG
                true
            }
            "jpg", "jpeg", "png", "gif", "image/jpg", "image/png", "image/gif" -> {
                binding.myCanvas.drawBitmap(this, uri)
                defaultExtension = PAINT_JPG
                true
            }
            else -> {
                toast(R.string.invalid_file_format)
                false
            }
        }
    }

    private fun confirmImage() {
        when {
            isEditIntent -> {
                try {
                    val outputStream = contentResolver.openOutputStream(intentUri!!)
                    saveToOutputStream(outputStream, defaultPath.getCompressionFormat(), true)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
            intentUri?.scheme == "content" -> {
                val outputStream = contentResolver.openOutputStream(intentUri!!)
                saveToOutputStream(outputStream, defaultPath.getCompressionFormat(), true)
            }
            else -> handlePermission(PERMISSION_WRITE_STORAGE) {
                val fileDirItem = FileDirItem(defaultPath, defaultPath.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    saveToOutputStream(it, defaultPath.getCompressionFormat(), true)
                }
            }
        }
    }

    private fun saveToOutputStream(
        outputStream: OutputStream?,
        format: Bitmap.CompressFormat,
        finishAfterSaving: Boolean
    ) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        val quality = if (format == Bitmap.CompressFormat.PNG) {
            100
        } else {
            70
        }

        outputStream.use {
            binding.myCanvas.getBitmap().compress(format, quality, it)
        }

        if (finishAfterSaving) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun trySaveImage() {
        SaveImageDialog(
            this,
            defaultPath,
            defaultFilename,
            defaultExtension,
            true
        ) { fullPath, filename, extension ->
            val mimetype = if (extension == PAINT_SVG) "svg+xml" else extension

            defaultFilename = filename
            defaultExtension = extension
            config.paintLastSaveExtension = extension

            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "image/$mimetype"
                putExtra(Intent.EXTRA_TITLE, "$filename.$extension")
                addCategory(Intent.CATEGORY_OPENABLE)

                startActivityForResult(this, SAVE_IMAGE_INTENT)
            }
        }
    }

    private fun saveImage() {
        SaveImageDialog(
            this,
            defaultPath,
            defaultFilename,
            defaultExtension,
            false
        ) { fullPath, filename, extension ->
            savedPathsHash = binding.myCanvas.getDrawingHashCode()
            saveFile(fullPath)
            defaultPath = fullPath.getParentPath()
            defaultFilename = filename
            defaultExtension = extension
            config.paintLastSaveFolder = defaultPath
            config.paintLastSaveExtension = extension
        }
    }

    private fun saveFile(path: String) {
        when (path.getFilenameExtension()) {
            PAINT_SVG -> Svg.saveSvg(this, path, binding.myCanvas)
            else -> saveImageFile(path)
        }
        rescanPaths(arrayListOf(path)) {}
    }

    private fun saveImageFile(path: String) {
        val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                writeToOutputStream(path, it)
                toast(R.string.file_saved)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun writeToOutputStream(path: String, out: OutputStream) {
        out.use {
            binding.myCanvas.getBitmap().compress(path.getCompressionFormat(), 70, out)
        }
    }

    private fun getImagePath(bitmap: Bitmap, callback: (path: String?) -> Unit) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bytes)

        val folder = File(cacheDir, FOLDER_NAME)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                callback(null)
                return
            }
        }

        val newPath = "$folder/$FILE_NAME"
        val fileDirItem = FileDirItem(newPath, FILE_NAME)
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                try {
                    it.write(bytes.toByteArray())
                    callback(newPath)
                } catch (e: Exception) {
                } finally {
                    it.close()
                }
            } else {
                callback("")
            }
        }
    }

    private fun pickColor() {
        ColorPickerDialog(this, color) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                setColor(color)
            }
        }
    }

    fun setBackgroundColor(pickedColor: Int) {
        val contrastColor = pickedColor.getContrastColor()
        binding.undo.applyColorFilter(contrastColor)
        binding.redo.applyColorFilter(contrastColor)
        binding.myCanvas.updateBackgroundColor(pickedColor)
        defaultExtension = PAINT_PNG
    }

    private fun setColor(pickedColor: Int) {
        color = pickedColor
        binding.colorPicker.setFillWithStroke(color, config.paintCanvasBackgroundColor, true)
        binding.myCanvas.setColor(color)
    }


    private fun getBrushStrokeSize() =
        resources.getDimension(R.dimen.paint_preview_dot_stroke_size).toInt()

    override fun toggleUndoVisibility(visible: Boolean) {
        binding.undo.beVisibleIf(visible)
    }

    override fun toggleRedoVisibility(visible: Boolean) {
        binding.redo.beVisibleIf(visible)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BITMAP_PATH, lastBitmapPath)

        if (uriToLoad != null) {
            outState.putString(URI_TO_LOAD, uriToLoad.toString())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastBitmapPath = savedInstanceState.getString(BITMAP_PATH)!!
        if (lastBitmapPath.isNotEmpty()) {
            openPath(lastBitmapPath)
        } else if (savedInstanceState.containsKey(URI_TO_LOAD)) {
            uriToLoad = Uri.parse(savedInstanceState.getString(URI_TO_LOAD))
            tryOpenUri(uriToLoad!!, intent)
        }
    }

    private var onStrokeWidthBarChangeListener: SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                brushSize = progress.toFloat()
                updateBrushSize()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }

    private fun updateBrushSize() {
        binding.myCanvas.setBrushSize(brushSize)
    }
}
