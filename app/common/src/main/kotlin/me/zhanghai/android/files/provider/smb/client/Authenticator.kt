package me.zhanghai.android.files.provider.smb.client

interface Authenticator {
    fun getPassword(authority: Authority): String?
}
