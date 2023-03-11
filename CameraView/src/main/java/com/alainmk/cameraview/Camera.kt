package com.alainmk.cameraview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.alainmk.cameraview.utility.PermUtil
import com.alainmk.cameraview.utility.Utility
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.size.AspectRatio
import com.otaliastudios.cameraview.size.SizeSelectors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Camera : AppCompatActivity() {

    private var maxVideoDuration = 40000
    var camera: CameraView? = null
    private var statusBarHeight = 0
    private var bottomBarHeight = 0
    private var colorPrimaryDark = 0
    private var zoom = 0.0f
    private val handler = Handler()
    private var runnable: Runnable? = null
    private var statusBarBg: View? = null
    private  var bottomButtons:View? = null
    private var videoCounterProgressbar: ProgressBar? = null
    private var currentUrl: String? = null
    private var options: Options? = null

    var flash: FrameLayout? = null
    var front: ImageView? = null
    var takeImageView: ImageView? = null
    var flashDrawable = 0

    var videoCounterProgress = 0

    fun returnObjects() {
        val url = currentUrl
        val resultIntent = Intent()
        resultIntent.putExtra(IMAGE_RESULT, url)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utility.setupStatusBarHidden(this)
        Utility.hideStatusBar(this)
        setContentView(R.layout.activity_main_lib)
        initialize()
    }

    override fun onRestart() {
        super.onRestart()
        camera!!.open()
        camera!!.mode = Mode.PICTURE
    }

    override fun onResume() {
        super.onResume()
        camera!!.open()
        camera!!.mode = Mode.PICTURE
    }

    override fun onPause() {
        camera!!.close()
        super.onPause()
    }

    private fun initialize() {
        val params: WindowManager.LayoutParams = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        Utility.getScreenSize(this)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        try {
            options = intent.getSerializableExtra(OPTIONS) as Options?
        } catch (e: Exception) {
            e.printStackTrace()
        }
        maxVideoDuration =
            options!!.videoDurationLimitinSeconds * 1000 //conversion in  milli seconds
        (findViewById<View>(R.id.message_bottom) as TextView).setText(if (options!!.isExcludeVideos) R.string.pix_bottom_message_without_video else R.string.pix_bottom_message_with_video)
        statusBarHeight = Utility.getStatusBarSizePort(this)
        requestedOrientation = options!!.screenOrientation
        colorPrimaryDark =
            ResourcesCompat.getColor(resources, R.color.colorPrimary, theme)
        camera = findViewById(R.id.camera_view)
        camera!!.mode = Mode.PICTURE
        if (options!!.isExcludeVideos) {
            camera!!.audio = Audio.OFF
        }
        val width = SizeSelectors.minWidth(Utility.WIDTH)
        val height = SizeSelectors.minHeight(Utility.HEIGHT)
        val dimensions =
            SizeSelectors.and(width, height) // Matches sizes bigger than width X height
        val ratio = SizeSelectors.aspectRatio(AspectRatio.of(1, 2), 0f) // Matches 1:2 sizes.
        val ratio3 = SizeSelectors.aspectRatio(AspectRatio.of(2, 3), 0f) // Matches 2:3 sizes.
        val ratio2 = SizeSelectors.aspectRatio(AspectRatio.of(9, 16), 0f) // Matches 9:16 sizes.
        val result = SizeSelectors.or(
            SizeSelectors.and(ratio, dimensions),
            SizeSelectors.and(ratio2, dimensions),
            SizeSelectors.and(ratio3, dimensions)
        )
        camera!!.setPictureSize(result)
        camera!!.setVideoSize(result)
        camera!!.setLifecycleOwner(this)
        if (options!!.isFrontfacing) {
            camera!!.facing = Facing.FRONT
        } else {
            camera!!.facing = Facing.BACK
        }
        camera!!.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                val dir = Environment.getExternalStoragePublicDirectory(
                    options!!.path
                )
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val photo = File(
                    dir, "IMG_"
                            + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                            + ".jpg"
                )
                result.toFile(photo) {
                    Utility.vibe(this@Camera, 50)
                    currentUrl = it?.absolutePath
                    if (it != null) {
                        Utility.scanPhoto(this@Camera, it)
                    }
                    returnObjects()
                }
            }

            override fun onVideoTaken(result: VideoResult) {
                // A Video was taken!
                Utility.vibe(this@Camera, 50)
                currentUrl = result.file.absolutePath
                Utility.scanPhoto(this@Camera, result.file)
                camera!!.mode = Mode.PICTURE
                returnObjects()
            }

            override fun onVideoRecordingStart() {
                findViewById<View>(R.id.video_counter_layout).visibility = View.VISIBLE
                videoCounterProgress = 0
                videoCounterProgressbar!!.progress = 0
                object : Runnable {
                    override fun run() {
                        ++videoCounterProgress
                        videoCounterProgressbar!!.progress = videoCounterProgress
                        val textView: TextView = findViewById(R.id.video_counter)
                        val counter: String
                        var min = 0
                        var sec = "" + videoCounterProgress
                        if (videoCounterProgress > 59) {
                            min = videoCounterProgress / 60
                            sec = "" + (videoCounterProgress - 60 * min)
                        }
                        if (sec.length == 1) {
                            sec = "0$sec"
                        }
                        counter = "$min:$sec"
                        textView.text = counter
                        handler.postDelayed(this, 1000)
                    }
                }.also { runnable = it }
                handler.postDelayed(runnable as Runnable, 1000)
                takeImageView!!.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                flash!!.animate().alpha(0f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                findViewById<View>(R.id.message_bottom).animate().alpha(0f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                front!!.animate().alpha(0f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }

            override fun onVideoRecordingEnd() {
                findViewById<View>(R.id.video_counter_layout).visibility = View.GONE
                handler.removeCallbacks(runnable!!)
                takeImageView!!.animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                findViewById<View>(R.id.message_bottom).animate().scaleX(1f).scaleY(1f)
                    .setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
                flash!!.animate().alpha(1f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                front!!.animate().alpha(1f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            } // And much more
        })
        zoom = 0.0f
        flash = findViewById(R.id.flash)
        takeImageView = findViewById(R.id.clickme)
        front = findViewById(R.id.front)
        videoCounterProgressbar = findViewById(R.id.video_pbr)
        bottomButtons = findViewById(R.id.bottomButtons)
        statusBarBg = findViewById(R.id.status_bar_bg)
        statusBarBg!!.layoutParams.height = statusBarHeight
        statusBarBg!!.translationY = (-1 * statusBarHeight).toFloat()
        statusBarBg!!.requestLayout()
        bottomBarHeight = Utility.getSoftButtonsBarSizePort(this)
        val lp1 = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        lp1.setMargins(0, statusBarHeight, 0, 0)
        onClickMethods()
        flashDrawable = R.drawable.ic_flash_off_black_24dp
        if (options!!.preSelectedUrls.size > options!!.count) {
            val large = options!!.preSelectedUrls.size - 1
            val small = options!!.count
            for (i in large downTo small - 1 + 1) {
                options!!.preSelectedUrls.removeAt(i)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onClickMethods() {
        takeImageView!!.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                findViewById<View>(R.id.clickmebg).visibility = View.GONE
                findViewById<View>(R.id.clickmebg).animate().scaleX(1f).scaleY(1f)
                    .setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
                takeImageView!!.animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            } else if (event.action == MotionEvent.ACTION_DOWN) {
                findViewById<View>(R.id.clickmebg).visibility = View.VISIBLE
                findViewById<View>(R.id.clickmebg).animate().scaleX(1.2f).scaleY(1.2f)
                    .setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
                takeImageView!!.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
            if (event.action == MotionEvent.ACTION_UP && camera!!.isTakingVideo) {
                camera!!.stopVideo()
            }

            false
        }

        takeImageView!!.setOnLongClickListener {
            if (options!!.isExcludeVideos) {
                return@setOnLongClickListener false
            }
            camera!!.mode = Mode.VIDEO
            val dir =
                Environment.getExternalStoragePublicDirectory(options!!.path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val video = File(
                dir, "VID_"
                        + SimpleDateFormat("yyyyMMdd_HHmmSS", Locale.ENGLISH)
                    .format(Date())
                        + ".mp4"
            )
            videoCounterProgressbar!!.max = maxVideoDuration / 1000
            videoCounterProgressbar!!.invalidate()
            camera!!.takeVideo(video, maxVideoDuration)
            true
        }
        takeImageView!!.setOnClickListener {
            if (camera!!.mode == Mode.VIDEO) {
                return@setOnClickListener
            }
            val oj = ObjectAnimator.ofFloat(camera, "alpha", 1f, 0f, 0f, 1f)
            oj.startDelay = 200L
            oj.duration = 600L
            oj.start()
            camera!!.takePicture()
        }
        val iv = flash!!.getChildAt(0) as ImageView
        flash!!.setOnClickListener {
            val height = flash!!.height
            iv.animate()
                .translationY(height.toFloat())
                .setDuration(100)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        iv.translationY = -(height / 2).toFloat()
                        when (flashDrawable) {
                            R.drawable.ic_flash_auto_black_24dp -> {
                                flashDrawable = R.drawable.ic_flash_off_black_24dp
                                iv.setImageResource(flashDrawable)
                                camera!!.flash = Flash.OFF
                            }
                            R.drawable.ic_flash_off_black_24dp -> {
                                flashDrawable = R.drawable.ic_flash_on_black_24dp
                                iv.setImageResource(flashDrawable)
                                camera!!.flash = Flash.ON
                            }
                            else -> {
                                flashDrawable = R.drawable.ic_flash_auto_black_24dp
                                iv.setImageResource(flashDrawable)
                                camera!!.flash = Flash.AUTO
                            }
                        }
                        iv.animate().translationY(0f).setDuration(50).setListener(null).start()
                    }
                })
                .start()
        }
        front!!.setOnClickListener {
            val oa1 = ObjectAnimator.ofFloat(front, "scaleX", 1f, 0f).setDuration(150)
            val oa2 = ObjectAnimator.ofFloat(front, "scaleX", 0f, 1f).setDuration(150)
            oa1.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    front!!.setImageResource(R.drawable.ic_photo_camera)
                    oa2.start()
                }
            })
            oa1.start()
            if (options!!.isFrontfacing) {
                options!!.isFrontfacing = false
                camera!!.facing = Facing.BACK
            } else {
                camera!!.facing = Facing.FRONT
                options!!.isFrontfacing = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camera!!.destroy()
    }

    companion object {
        const val OPTIONS = "options"
        const val IMAGE_RESULT = "image_results"
        fun start(context: FragmentActivity, options: Options) {
            PermUtil.checkForCamaraWritePermissions(context) {
                val i = Intent(context, Camera::class.java)
                i.putExtra(OPTIONS, options)
                context.startActivityForResult(i, options.getRequestCode())
            }
        }
    }
}