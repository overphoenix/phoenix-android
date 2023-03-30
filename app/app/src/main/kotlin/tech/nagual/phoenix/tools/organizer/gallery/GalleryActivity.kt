package tech.nagual.phoenix.tools.organizer.gallery

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.camera.capturer.VideoCapturer
import tech.nagual.phoenix.tools.organizer.data.model.Attachment
import tech.nagual.phoenix.tools.organizer.utils.EditImageContract
import tech.nagual.phoenix.tools.organizer.utils.shareAttachment
import tech.nagual.common.dialogs.PropertiesDialog
import java.io.InputStream
import kotlin.properties.Delegates

@AndroidEntryPoint
class GalleryActivity : AppCompatActivity() {
    lateinit var gallerySlider: ViewPager2
    private val mediaUris: ArrayList<Uri> = arrayListOf()
    private lateinit var attachments: ArrayList<Attachment>
    private var snackBar: Snackbar? = null
    private var ogColor by Delegates.notNull<Int>()

//    val editorViewModel: EditorViewModel by viewModels()

    private val editIntentLauncher = registerForActivityResult(EditImageContract) { result ->
        if (result) {
            recreate()
        }
    }

    private lateinit var rootView: View

    private val itemCount: Int
        get() = (gallerySlider.adapter as GallerySliderAdapter).itemCount

    private fun getCurrentPosition(): Int =
        (gallerySlider.adapter as GallerySliderAdapter).getCurrentPosition()

    private fun getCurrentUri(): Uri =
        (gallerySlider.adapter as GallerySliderAdapter).getCurrentUri()

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.organizer_gallery, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_icon -> {
                editCurrentMedia()
                true
            }
//            R.id.delete_icon -> {
//                deleteCurrentMedia()
//                true
//            }
            R.id.info -> {
                showCurrentMediaDetails()
                true
            }
            R.id.share_icon -> {
                shareCurrentMedia()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun editCurrentMedia() {
        editIntentLauncher.launch(getCurrentUri())
    }

//    private fun deleteCurrentMedia() {
//        val pos = getCurrentPosition()
//        val attachment = attachments[pos]
//        (gallerySlider.adapter as GallerySliderAdapter).removeChildAt(pos)
//        MaterialAlertDialogBuilder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
//            .setTitle(attachment.description)
//            .setMessage(
//                getString(
//                    R.string.organizer_delete_attachment_ask_message,
//                    attachment.description
//                )
//            )
//            .setPositiveButton(R.string.delete) { _, _ ->
//                editorViewModel.deleteAttachment(attachment)
//                (gallerySlider.adapter as GallerySliderAdapter).removeChildAt(pos)
//            }
//            .setNegativeButton(R.string.cancel, null).show()
//    }

    private fun showCurrentMediaDetails() {
        PropertiesDialog(this, getCurrentUri().toString())
    }

    private fun animateBackgroundToBlack() {
        val cBgColor = (rootView.background as ColorDrawable).color

        if (cBgColor == Color.BLACK) {
            return
        }

        val bgColorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            ogColor,
            Color.BLACK
        )
        bgColorAnim.duration = 300
        bgColorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            rootView.setBackgroundColor(color)
        }
        bgColorAnim.start()
    }

    private fun animateBackgroundToOriginal() {
        val cBgColor = (rootView.background as ColorDrawable).color

        if (cBgColor == ogColor) {
            return
        }

        val bgColorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.BLACK,
            ogColor,
        )
        bgColorAnim.duration = 300
        bgColorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            this.rootView.setBackgroundColor(color)
        }
        bgColorAnim.start()
    }

    private fun shareCurrentMedia() {
        shareAttachment(this, attachments[getCurrentPosition()])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showVideosOnly = intent.extras?.getBoolean("videosOnly")!!

        ogColor = ContextCompat.getColor(this, R.color.system_neutral1_900)

        setContentView(R.layout.organizer_gallery)

        supportActionBar?.let {
            it.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.appbar)))
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }

        rootView = findViewById(R.id.root_view)
        rootView.setOnClickListener {
            toggleActionBarState()
        }

        gallerySlider = findViewById(R.id.gallery_slider)
        gallerySlider.setPageTransformer(GSlideTransformer())

        attachments = intent.extras?.getParcelableArrayList("attachments") ?: arrayListOf()
        val intentMediaUris = intent.extras?.getParcelableArrayList<Uri>("mediaUris")

        if (intentMediaUris != null) {
            if (showVideosOnly) {
                for (mediaUri in intentMediaUris) {
                    if (VideoCapturer.isVideo(mediaUri)) {
                        mediaUris.add(mediaUri)
                    }
                }
            } else {
                mediaUris.addAll(intentMediaUris)
            }
        }

        // Close gallery if no files are present
        if (mediaUris.isEmpty()) {
            showMessage(
                "Please capture a photo/video before trying to view" +
                        " them."
            )
            finish()
        }

        gallerySlider.adapter = GallerySliderAdapter(this, mediaUris)

        snackBar = Snackbar.make(
            gallerySlider,
            "",
            Snackbar.LENGTH_LONG
        )
    }

    fun toggleActionBarState() {
        supportActionBar?.let {
            if (it.isShowing) {
                hideActionBar()
            } else {
                showActionBar()
            }
        }
    }

    fun showActionBar() {
        supportActionBar?.let {
            it.show()
            animateBackgroundToOriginal()
        }
    }

    fun hideActionBar() {
        supportActionBar?.let {
            it.hide()
            animateBackgroundToBlack()
        }
    }

    private fun uriExists(uri: Uri): Boolean {
        try {
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return false
            inputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun onResume() {
        super.onResume()

        val gsaUris = (gallerySlider.adapter as GallerySliderAdapter).mediaUris


        val newUris = intent.extras?.getParcelableArrayList<Uri>("mediaUris")
        var urisHaveChanged = false

        if (newUris != null) {
            for (mediaUri in gsaUris) {
                if (!newUris.contains(mediaUri)) {
                    urisHaveChanged = true
                    break
                }
            }

            if (urisHaveChanged) {
                gallerySlider.adapter = GallerySliderAdapter(this, newUris)
            }
        }

        showActionBar()
    }

    fun showMessage(msg: String) {
        snackBar?.setText(msg)
        snackBar?.show()
    }
}
