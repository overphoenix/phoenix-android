package tech.nagual.common.ui.speeddial;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import tech.nagual.common.R;

public class UiUtils {

    private UiUtils() {
    }

    public static int getPrimaryColor(Context context) {
        int colorAttr;
        colorAttr = android.R.attr.colorPrimary;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public static int getOnSecondaryColor(Context context) {
        int colorAttr;
        colorAttr = R.attr.colorOnSecondary;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public static int getAccentColor(Context context) {
        int colorAttr;
        colorAttr = android.R.attr.colorAccent;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public static int dpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics));
    }

    public static int pxToDp(float px) {
        return Math.round(px / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Fade out animation.
     *
     * @param view view to animate.
     */
    public static void fadeOutAnim(final View view) {
        ViewCompat.animate(view).cancel();
        view.setAlpha(1F);
        view.setVisibility(VISIBLE);
        ViewCompat.animate(view)
                .alpha(0F)
                .withLayer()
                .setDuration(view.getContext().getResources().getInteger(R.integer.sd_close_animation_duration))
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(GONE);
                    }
                })
                .start();
    }

    /**
     * Fade out animation.
     *
     * @param view view to animate.
     */
    public static void fadeInAnim(final View view) {
        ViewCompat.animate(view).cancel();
        view.setAlpha(0);
        view.setVisibility(VISIBLE);
        ViewCompat.animate(view)
                .alpha(1F)
                .withLayer()
                .setDuration(view.getContext().getResources().getInteger(R.integer.sd_open_animation_duration))
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    /**
     * SpeedDial opening animation.
     *
     * @param view        view that starts that animation.
     * @param startOffset a delay in time to start the animation
     */
    public static void enlargeAnim(View view, long startOffset) {
        ViewCompat.animate(view).cancel();
        view.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.sd_scale_fade_and_translate_in);
        anim.setStartOffset(startOffset);
        view.startAnimation(anim);
    }

    /**
     * SpeedDial closing animation.
     *
     * @param view        view that starts that animation.
     * @param startOffset a delay in time to start the animation
     */
    public static void shrinkAnim(final View view, long startOffset) {
        ViewCompat.animate(view).cancel();
        view.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.sd_scale_fade_and_translate_out);
        anim.setStartOffset(startOffset);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(anim);
    }

    /**
     * Closing animation.
     *
     * @param view       view that starts that animation.
     * @param removeView true to remove the view when the animation is over, false otherwise.
     */
    public static void shrinkAnim(final View view, final boolean removeView) {
        ViewCompat.animate(view).cancel();
        ViewCompat.animate(view)
                .alpha(0F)
                .withLayer()
                .setDuration(view.getContext().getResources().getInteger(R.integer.sd_close_animation_duration))
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (removeView) {
                            ViewGroup parent = (ViewGroup) view.getParent();
                            if (parent != null) {
                                parent.removeView(view);
                            }
                        } else {
                            view.setVisibility(GONE);
                        }
                    }
                })
                .start();
    }

    /**
     * Rotate a view of the specified degrees.
     *
     * @param view    The view to rotate.
     * @param animate true to animate the rotation, false to be instant.
     * @see #rotateBackward(View, boolean)
     */
    public static void rotateForward(View view, float angle, boolean animate) {
        ViewCompat.animate(view)
                .rotation(angle)
                .withLayer()
                .setDuration(animate ?
                        view.getContext().getResources().getInteger(R.integer.sd_rotate_animation_duration) : 0)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    /**
     * Rotate a view back to its default angle (0°).
     *
     * @param view    The view to rotate.
     * @param animate true to animate the rotation, false to be instant.
     * @see #rotateForward(View, float, boolean)
     */
    public static void rotateBackward(View view, boolean animate) {
        ViewCompat.animate(view)
                .rotation(0.0F)
                .withLayer()
                .setDuration(animate ?
                        view.getContext().getResources().getInteger(R.integer.sd_rotate_animation_duration) : 0)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    public static Drawable getRotateDrawable(final Drawable drawable, final float angle) {
        if (angle == 0) {
            return drawable;
        }
        final Drawable[] drawables = {drawable};
        return new LayerDrawable(drawables) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, drawable.getIntrinsicWidth() / 2f, drawable.getIntrinsicHeight() / 2f);
                super.draw(canvas);
                canvas.restore();
            }
        };
    }

    /**
     * Creates a {@link Bitmap} from a {@link Drawable}.
     */
    @Nullable
    public static Bitmap getBitmapFromDrawable(@Nullable Drawable drawable) {
        if (drawable == null) {
            return null;
        } else {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if (bitmapDrawable.getBitmap() != null) {
                    return bitmapDrawable.getBitmap();
                }
            }

            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                // Single color bitmap will be created of 1x1 pixel
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap
                        .Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    /**
     * Creates a {@link Drawable} from a {@link Bitmap}.
     */
    @Nullable
    public static Drawable getDrawableFromBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        } else {
            return new BitmapDrawable(bitmap);
        }
    }

    /**
     * Perform a tap of {@link ViewConfiguration#getTapTimeout()} milliseconds on the view.
     *
     * @param view the view you want to tap
     */
    public static void performTap(final View view) {
        view.setPressed(true);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setPressed(false);
                view.performClick();
            }
        }, ViewConfiguration.getTapTimeout());
    }

    /**
     * Crop the image into a circle
     */
    public static Drawable cropFabImageInCircle(Drawable fabIcon) {
        Bitmap bitmap = UiUtils.getBitmapFromDrawable(fabIcon);
        if (bitmap == null) {
            Log.e(TAG, "Couldn't crop the Image");
            return fabIcon;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > height) {
            bitmap = Bitmap.createBitmap(bitmap, width / 2 - height / 2, 0, height, height);
        } else if (width < height) {
            bitmap = Bitmap.createBitmap(bitmap, 0, height / 2 - width / 2, width, width);
        }

        Bitmap circleBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        BitmapShader shader;
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        int circleCenter = bitmap.getWidth() / 2;
        Canvas canvas = new Canvas(circleBitmap);
        canvas.drawCircle(circleCenter, circleCenter, circleCenter, paint);
        Drawable cropped = UiUtils.getDrawableFromBitmap(circleBitmap);
        if (cropped == null) {
            Log.e(TAG, "Couldn't crop the Image");
            return fabIcon;
        }
        return cropped;
    }
}
