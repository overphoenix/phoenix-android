package me.zhanghai.android.files.provider.ftp.client

interface Authenticator {
    fun getPassword(authority: Authority): String?
}
