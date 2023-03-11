package com.alainmk.cameraview

import java.io.Serializable
import java.util.*

class Options private constructor() : Serializable {

    var count = 1
    private var requestCode = 0
    var path = "Pix/Camera"
    val width = 0
    var isFrontfacing = false
    var videoDurationLimitinSeconds = 40
    var isExcludeVideos = false
    val preSelectedUrls = ArrayList<String>()

    @ScreenOrientation
    var screenOrientation = SCREEN_ORIENTATION_UNSPECIFIED
        private set

    fun setVideoDurationLimitinSeconds(videoDurationLimitinSececonds: Int): Options {
        videoDurationLimitinSeconds = videoDurationLimitinSececonds
        return this
    }

    fun setExcludeVideos(excludeVideos: Boolean): Options {
        isExcludeVideos = excludeVideos
        return this
    }

    fun setFrontfacing(frontfacing: Boolean): Options {
        isFrontfacing = frontfacing
        return this
    }

    fun getRequestCode(): Int {
        if (requestCode == 0) {
            throw NullPointerException("requestCode in Options class is null")
        }
        return requestCode
    }

    fun setRequestCode(requestcode: Int): Options {
        requestCode = requestcode
        return this
    }

    fun setPath(path: String): Options {
        this.path = path
        return this
    }

    fun setScreenOrientation(@ScreenOrientation screenOrientation: Int): Options {
        this.screenOrientation = screenOrientation
        return this
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class ScreenOrientation

    companion object {
        const val SCREEN_ORIENTATION_UNSPECIFIED = -1
        const val SCREEN_ORIENTATION_PORTRAIT = 1
        fun init(): Options {
            return Options()
        }
    }
}