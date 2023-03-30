package tech.nagual.common.permissions

import android.R
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class OpenAppDetailsDialogFragment : AppCompatDialogFragment() {
    private var mRequestCode = 0
    private var mPackageName: String? = null
    private var mTitle: CharSequence? = null
    private var mMessage: CharSequence? = null
    private var mPositiveButtonText: CharSequence? = null
    private var mNegativeButtonText: CharSequence? = null
    private var mCancelable = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        mRequestCode = arguments!!.getInt(EXTRA_REQUEST_CODE)
        mPackageName = arguments.getString(EXTRA_PACKAGE_NAME)
        mTitle = arguments.getCharSequence(EXTRA_TITLE)
        mMessage = arguments.getCharSequence(EXTRA_MESSAGE)
        mPositiveButtonText = arguments.getCharSequence(EXTRA_POSITIVE_BUTTON_TEXT)
        mNegativeButtonText = arguments.getCharSequence(EXTRA_NEGATIVE_BUTTON_TEXT)
        mCancelable = arguments.getBoolean(EXTRA_CANCELABLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity: Activity? = activity
        val builder = MaterialAlertDialogBuilder(requireActivity(), theme)
        if (mTitle != null) {
            builder.setTitle(mTitle)
        }
        if (mMessage != null) {
            builder.setMessage(mMessage)
        }
        var positiveButtonText = mPositiveButtonText
        if (positiveButtonText == null) {
            positiveButtonText = activity!!.getText(R.string.ok)
        }
        builder.setPositiveButton(positiveButtonText) { dialog, which -> openAppDetails() }
        if (mNegativeButtonText != null) {
            builder.setNegativeButton(mNegativeButtonText, null)
        }
        builder.setCancelable(mCancelable)
        return builder.create()
    }

    override fun setTargetFragment(fragment: Fragment?, requestCode: Int) {
        super.setTargetFragment(fragment, requestCode)
        mRequestCode = requestCode
        requireArguments().putInt(EXTRA_REQUEST_CODE, requestCode)
    }

    private fun openAppDetails() {
        val activity: Activity? = activity
        val packageName = if (mPackageName != null) mPackageName!! else activity!!.packageName
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
        val targetFragment = targetFragment
        if (targetFragment != null) {
            if (mRequestCode != REQUEST_CODE_INVALID) {
                targetFragment.startActivityForResult(intent, mRequestCode)
            } else {
                targetFragment.startActivity(intent)
            }
            return
        }
        val parentFragment = parentFragment
        if (parentFragment != null) {
            if (mRequestCode != REQUEST_CODE_INVALID) {
                parentFragment.startActivityForResult(intent, mRequestCode)
            } else {
                parentFragment.startActivity(intent)
            }
            return
        }
        if (mRequestCode != REQUEST_CODE_INVALID) {
            activity!!.startActivityForResult(intent, mRequestCode)
        } else {
            activity!!.startActivity(intent)
        }
    }

    companion object {
        private val KEY_PREFIX = OpenAppDetailsDialogFragment::class.java.name + '.'
        private val EXTRA_REQUEST_CODE = KEY_PREFIX + "REQUEST_CODE"
        private val EXTRA_PACKAGE_NAME = KEY_PREFIX + "PACKAGE_NAME"
        private val EXTRA_TITLE = KEY_PREFIX + "TITLE"
        private val EXTRA_MESSAGE = KEY_PREFIX + "MESSAGE"
        private val EXTRA_POSITIVE_BUTTON_TEXT = KEY_PREFIX + "POSITIVE_BUTTON_TEXT"
        private val EXTRA_NEGATIVE_BUTTON_TEXT = KEY_PREFIX + "NEGATIVE_BUTTON_TEXT"
        private val EXTRA_CANCELABLE = KEY_PREFIX + "CANCELABLE"
        private const val REQUEST_CODE_INVALID = -1
        fun newInstance(
            requestCode: Int, packageName: String?,
            title: CharSequence?,
            message: CharSequence?,
            positiveButtonText: CharSequence?,
            negativeButtonText: CharSequence?,
            cancelable: Boolean
        ): OpenAppDetailsDialogFragment {
            val fragment = OpenAppDetailsDialogFragment()
            val arguments = Bundle()
            arguments.putInt(EXTRA_REQUEST_CODE, requestCode)
            arguments.putString(EXTRA_PACKAGE_NAME, packageName)
            arguments.putCharSequence(EXTRA_TITLE, title)
            arguments.putCharSequence(EXTRA_MESSAGE, message)
            arguments.putCharSequence(EXTRA_POSITIVE_BUTTON_TEXT, positiveButtonText)
            arguments.putCharSequence(EXTRA_NEGATIVE_BUTTON_TEXT, negativeButtonText)
            arguments.putBoolean(EXTRA_CANCELABLE, cancelable)
            fragment.arguments = arguments
            return fragment
        }

        fun show(
            title: CharSequence?, message: CharSequence?,
            positiveButtonText: CharSequence?, negativeButtonText: CharSequence?,
            fragment: Fragment
        ) {
            newInstance(
                REQUEST_CODE_INVALID, null, title, message,
                positiveButtonText, negativeButtonText, false
            )
                .show(fragment.childFragmentManager, null)
        }

        fun show(
            @StringRes titleRes: Int, @StringRes messageRes: Int,
            @StringRes positiveButtonTextRes: Int,
            @StringRes negativeButtonTextRes: Int, fragment: Fragment
        ) {
            show(
                fragment.getText(titleRes), fragment.getText(messageRes),
                fragment.getText(positiveButtonTextRes), fragment.getText(negativeButtonTextRes),
                fragment
            )
        }

        fun show(
            @StringRes messageRes: Int, @StringRes positiveButtonTextRes: Int,
            fragment: Fragment
        ) {
            show(
                null, fragment.getText(messageRes), fragment.getText(positiveButtonTextRes),
                fragment.getText(R.string.cancel), fragment
            )
        }

        fun show(
            title: CharSequence?, message: CharSequence?,
            positiveButtonText: CharSequence?, negativeButtonText: CharSequence?,
            activity: AppCompatActivity
        ) {
            newInstance(
                REQUEST_CODE_INVALID, null, title, message,
                positiveButtonText, negativeButtonText, false
            )
                .show(activity.supportFragmentManager, null)
        }

        fun show(
            @StringRes titleRes: Int, @StringRes messageRes: Int,
            @StringRes positiveButtonTextRes: Int,
            @StringRes negativeButtonTextRes: Int, activity: AppCompatActivity
        ) {
            show(
                activity.getText(titleRes), activity.getText(messageRes),
                activity.getText(positiveButtonTextRes), activity.getText(negativeButtonTextRes),
                activity
            )
        }

        fun show(
            @StringRes messageRes: Int, @StringRes positiveButtonTextRes: Int,
            activity: AppCompatActivity
        ) {
            show(
                null, activity.getText(messageRes), activity.getText(positiveButtonTextRes),
                activity.getText(R.string.cancel), activity
            )
        }
    }
}