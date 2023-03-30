package tech.nagual.protection

import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.AuthPromptHost
import androidx.biometric.auth.Class2BiometricAuthPrompt
import androidx.fragment.app.FragmentActivity
import tech.nagual.common.R
import tech.nagual.common.extensions.isTargetSdkVersion30Plus
import tech.nagual.common.extensions.toast

fun FragmentActivity.showBiometricPrompt(
    successCallback: ((String, Int) -> Unit)? = null,
    failureCallback: (() -> Unit)? = null
) {
    Class2BiometricAuthPrompt.Builder(getText(R.string.authenticate), getText(R.string.cancel))
        .build()
        .startAuthentication(
            AuthPromptHost(this),
            object : AuthPromptCallback() {
                override fun onAuthenticationSucceeded(
                    activity: FragmentActivity?,
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    successCallback?.invoke("", PROTECTION_FINGERPRINT)
                }

                override fun onAuthenticationError(
                    activity: FragmentActivity?,
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    val isCanceledByUser =
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED
                    if (!isCanceledByUser) {
                        toast(errString.toString())
                    }
                    failureCallback?.invoke()
                }

                override fun onAuthenticationFailed(activity: FragmentActivity?) {
                    toast(R.string.authentication_failed)
                    failureCallback?.invoke()
                }
            }
        )
}

fun FragmentActivity.performSecurityCheck(
    protectionType: Int,
    requiredHash: String,
    successCallback: ((String, Int) -> Unit)? = null,
    failureCallback: (() -> Unit)? = null
) {
    if (protectionType == PROTECTION_FINGERPRINT && isTargetSdkVersion30Plus()) {
        showBiometricPrompt(successCallback, failureCallback)
    } else {
        SecurityDialog(
            activity = this,
            requiredHash = requiredHash,
            showTabIndex = protectionType,
            callback = { hash, type, success ->
                if (success) {
                    successCallback?.invoke(hash, type)
                } else {
                    failureCallback?.invoke()
                }
            }
        )
    }
}
