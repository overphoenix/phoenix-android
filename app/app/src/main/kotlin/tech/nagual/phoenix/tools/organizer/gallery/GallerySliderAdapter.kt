package tech.nagual.phoenix.tools.organizer.gallery

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.camera.CamConfig
import tech.nagual.phoenix.tools.organizer.camera.capturer.VideoCapturer

class GallerySliderAdapter(
    private val gActivity: GalleryActivity,
    val mediaUris: ArrayList<Uri>
) : RecyclerView.Adapter<GallerySlide>() {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(
        gActivity
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            GallerySlide {
        return GallerySlide(
            layoutInflater.inflate(
                R.layout.organizer_gallery_slide,
                parent, false
            )
        )
    }

    override fun getItemId(position: Int): Long {
        return mediaUris[position].hashCode().toLong()
    }

    override fun onBindViewHolder(holder: GallerySlide, position: Int) {

        val mediaPreview: ZoomableImageView =
            holder.itemView.findViewById(R.id.organizer_slide_preview)

        mediaPreview.disableZooming()

        mediaPreview.setGalleryActivity(gActivity)

        val mediaUri = mediaUris[position]

        val playButton: ImageView =
            holder.itemView.findViewById(R.id.play_button)

        mediaPreview.setOnClickListener {

            val mUri = getCurrentUri()

            if (VideoCapturer.isVideo(mUri)) {
                val intent = Intent(
                    gActivity,
                    VideoPlayer::class.java
                )
                intent.putExtra("videoUri", mUri)

                gActivity.startActivity(intent)
            } else {
                gActivity.toggleActionBarState()
            }
        }

        if (VideoCapturer.isVideo(mediaUri)) {
            try {
                mediaPreview.setImageBitmap(
                    CamConfig.getVideoThumbnail(
                        gActivity,
                        mediaUri
                    )
                )

                playButton.visibility = View.VISIBLE

            } catch (exception: Exception) {
            }

        } else {
            playButton.visibility = View.INVISIBLE
            mediaPreview.enableZooming()
            mediaPreview.setImageURI(mediaUri)
        }
    }

    fun removeUri(uri: Uri) {
        removeChildAt(mediaUris.indexOf(uri))
    }

    fun removeChildAt(index: Int) {
        mediaUris.removeAt(index)

        // Close gallery if no files are present
        if (mediaUris.isEmpty()) {
            gActivity.finish()
        }

        notifyItemRemoved(index)
    }

    fun getCurrentPosition(): Int = gActivity.gallerySlider.currentItem

    fun getCurrentUri(): Uri = mediaUris[gActivity.gallerySlider.currentItem]

    override fun getItemCount(): Int = mediaUris.size
}