package tech.nagual.protection

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.biometric.auth.AuthPromptHost
import tech.nagual.common.R
import tech.nagual.common.extensions.performHapticFeedback
import tech.nagual.common.extensions.toast
import tech.nagual.common.ui.views.MyScrollView
import tech.nagual.common.ui.views.MyTextView
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class PinTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), SecurityTab {
    private var hash = ""
    private var requiredHash = ""
    private var pin = ""
    lateinit var hashListener: HashListener

    private lateinit var pinLockTitle: MyTextView
    private lateinit var pinLockCurrentPin: MyTextView

    override fun onFinishInflate() {
        super.onFinishInflate()

        pinLockTitle = findViewById(R.id.pin_lock_title)
        pinLockCurrentPin = findViewById(R.id.pin_lock_current_pin)
        val pin0: MyTextView = findViewById(R.id.pin_0)
        val pin1: MyTextView = findViewById(R.id.pin_1)
        val pin2: MyTextView = findViewById(R.id.pin_2)
        val pin3: MyTextView = findViewById(R.id.pin_3)
        val pin4: MyTextView = findViewById(R.id.pin_4)
        val pin5: MyTextView = findViewById(R.id.pin_5)
        val pin6: MyTextView = findViewById(R.id.pin_6)
        val pin7: MyTextView = findViewById(R.id.pin_7)
        val pin8: MyTextView = findViewById(R.id.pin_8)
        val pin9: MyTextView = findViewById(R.id.pin_9)
        val pinC: MyTextView = findViewById(R.id.pin_c)
        val pinOk: ImageView = findViewById(R.id.pin_ok)

        pin0.setOnClickListener { addNumber("0") }
        pin1.setOnClickListener { addNumber("1") }
        pin2.setOnClickListener { addNumber("2") }
        pin3.setOnClickListener { addNumber("3") }
        pin4.setOnClickListener { addNumber("4") }
        pin5.setOnClickListener { addNumber("5") }
        pin6.setOnClickListener { addNumber("6") }
        pin7.setOnClickListener { addNumber("7") }
        pin8.setOnClickListener { addNumber("8") }
        pin9.setOnClickListener { addNumber("9") }
        pinC.setOnClickListener { clear() }
        pinOk.setOnClickListener { confirmPIN() }
//        pin_ok.applyColorFilter(context.baseConfig.textColor)
    }

    override fun initTab(
        requiredHash: String,
        listener: HashListener,
        scrollView: MyScrollView,
        biometricPromptHost: AuthPromptHost,
        showBiometricAuthentication: Boolean
    ) {
        this.requiredHash = requiredHash
        hash = requiredHash
        hashListener = listener
    }

    private fun addNumber(number: String) {
        if (pin.length < 10) {
            pin += number
            updatePinCode()
        }
        performHapticFeedback()
    }

    private fun clear() {
        if (pin.isNotEmpty()) {
            pin = pin.substring(0, pin.length - 1)
            updatePinCode()
        }
        performHapticFeedback()
    }

    private fun confirmPIN() {
        val newHash = getHashedPin()
        if (pin.isEmpty()) {
            context.toast(R.string.please_enter_pin)
        } else if (hash.isEmpty()) {
            hash = newHash
            resetPin()
            pinLockTitle.setText(R.string.repeat_pin)
        } else if (hash == newHash) {
            hashListener.receivedHash(hash, PROTECTION_PIN)
        } else {
            resetPin()
            context.toast(R.string.wrong_pin)
            if (requiredHash.isEmpty()) {
                hash = ""
                pinLockTitle.setText(R.string.enter_pin)
            }
        }
        performHapticFeedback()
    }

    private fun resetPin() {
        pin = ""
        pinLockCurrentPin.text = ""
    }

    private fun updatePinCode() {
        pinLockCurrentPin.text = "•".repeat(pin.length)
        if (hash.isNotEmpty() && hash == getHashedPin()) {
            hashListener.receivedHash(hash, PROTECTION_PIN)
        }
    }

    private fun getHashedPin(): String {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        messageDigest.update(pin.toByteArray(charset("UTF-8")))
        val digest = messageDigest.digest()
        val bigInteger = BigInteger(1, digest)
        return String.format(Locale.getDefault(), "%0${digest.size * 2}x", bigInteger).lowercase(Locale.getDefault())
    }

    override fun visibilityChanged(isVisible: Boolean) {}
}
