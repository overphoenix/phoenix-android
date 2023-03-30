package me.zhanghai.android.files.file

import android.text.format.DateUtils
import org.threeten.bp.Duration

fun Duration.format(): String = DateUtils.formatElapsedTime(seconds)
