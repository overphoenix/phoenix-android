package tech.nagual.common.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseInitActivity : AppCompatActivity() {
    abstract fun initActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivity()
    }
}
