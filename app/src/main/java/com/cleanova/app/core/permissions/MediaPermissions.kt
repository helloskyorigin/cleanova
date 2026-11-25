package com.cleanova.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.cleanova.app.domain.Grant
import com.cleanova.app.domain.MediaAccess

object MediaPermissions {
    fun requested(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    fun read(context: Context): MediaAccess {
        fun granted(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT < 33) {
            val g = if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) Grant.FULL else Grant.NONE
            return MediaAccess(g, g)
        }
        val selected = Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        fun grant(p: String) = if (granted(p)) Grant.FULL else if (selected) Grant.SELECTED else Grant.NONE
        return MediaAccess(grant(Manifest.permission.READ_MEDIA_IMAGES), grant(Manifest.permission.READ_MEDIA_VIDEO))
    }
}
