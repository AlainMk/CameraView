package com.alainmk.cameraview.utility

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Created by akshay on 21/01/18.
 */
object Utility {
    var HEIGHT = 0
    var WIDTH = 0
    fun setupStatusBarHidden(appCompatActivity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w = appCompatActivity.window
            w.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            w.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                w.statusBarColor = Color.TRANSPARENT
            }
        }
    }

    fun hideStatusBar(appCompatActivity: AppCompatActivity) {
        synchronized(appCompatActivity) { appCompatActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) }
    }

    fun getSoftButtonsBarSizePort(activity: Activity): Int {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            val usableHeight = metrics.heightPixels
            activity.windowManager.defaultDisplay.getRealMetrics(metrics)
            val realHeight = metrics.heightPixels
            return if (realHeight > usableHeight) {
                realHeight - usableHeight
            } else {
                0
            }
        }
        return 0
    }

    fun getStatusBarSizePort(check: AppCompatActivity): Int {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            var result = 0
            //Log.e("->activity", "----------->  " + check);
            val res = check.baseContext.resources
            val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = check.resources.getDimensionPixelSize(resourceId)
            }
            return result
        }
        return 0
    }

    fun getScreenSize(activity: Activity) {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        HEIGHT = displayMetrics.heightPixels
        WIDTH = displayMetrics.widthPixels
    }

    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun vibe(c: Context, l: Long) {
        (c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(l)
    }

    fun scanPhoto(pix: Context, photo: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(photo)
            scanIntent.data = contentUri
            pix.sendBroadcast(scanIntent)
        } else {
            pix.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse(photo.absolutePath))
            )
        }
    }
}