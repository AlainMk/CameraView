package com.alainmk.cameramodule

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.AppCompatButton
import com.alainmk.cameraview.Camera
import com.alainmk.cameraview.Options
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button: AppCompatButton = findViewById(R.id.button_start)
        button.setOnClickListener {
            openCamera()
        }
    }

    private fun openCamera() {

        if (!EasyPermissions.hasPermissions(this, CAMERA_PERMS)) {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.popup_title_permission_files_access),
                RC_CAMERA_PERMS,
                CAMERA_PERMS
            )
            return
        }

        val options: Options = Options.init()
            .setRequestCode(RC_CAMERA_PERMS)
            .setFrontfacing(false)
            .setExcludeVideos(false)
            .setVideoDurationLimitinSeconds(10)
            .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT) //Orientaion
            .setPath("/CameraView")

        Camera.start(this, options)
    }

    // --------------------
    // FILE MANAGEMENT
    // --------------------

    companion object {

        private const val CAMERA_PERMS: String = Manifest.permission.CAMERA
        private const val RC_CAMERA_PERMS = 10
    }
}