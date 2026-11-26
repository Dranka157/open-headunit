package com.andrerinas.openheadunit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CarPlayHomeActivity : AppCompatActivity() {

    private lateinit var speedTracker: GpsSpeedTracker
    private lateinit var powerReceiver: CarPowerReceiver
    private lateinit var tvSpeedValue: TextView
    private lateinit var tvStatusInfo: TextView
    private var gpsActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(R.layout.activity_carplay_home)
        enableImmersiveFullscreen()

        tvSpeedValue = findViewById(R.id.tvSpeedValue)
        tvStatusInfo = findViewById(R.id.tvStatusInfo)

        setupTiles()
        setupSpeedometer()
        setupIgnitionAutomation()
        updateBatteryStatus()
    }

    private fun setupTiles() {
        bindAppTile(
            R.id.tile1, R.id.icon1,
            packages = listOf("com.samsung.android.dialer", "com.google.android.dialer"),
            fallbackIntent = Intent(Intent.ACTION_DIAL),
            fallbackIcon = android.R.drawable.sym_action_call
        )
        bindAppTile(
            R.id.tile2, R.id.icon2,
            packages = listOf("com.spotify.music", "com.google.android.apps.youtube.music"),
            fallbackIntent = null,
            fallbackIcon = android.R.drawable.ic_media_play
        )
        bindAppTile(
            R.id.tile3, R.id.icon3,
            packages = listOf("com.google.android.apps.maps"),
            fallbackIntent = null,
            fallbackIcon = android.R.drawable.ic_dialog_map
        )
        bindAppTile(
            R.id.tile4, R.id.icon4,
            packages = listOf("com.waze"),
            fallbackIntent = null,
            fallbackIcon = android.R.drawable.ic_menu_compass
        )
        findViewById<ImageView>(R.id.icon5).setImageResource(android.R.drawable.ic_menu_slideshow)
        findViewById<LinearLayout>(R.id.tile5).setOnClickListener {
            openOriginalHeadunit()
        }
        bindAppTile(
            R.id.tile6, R.id.icon6,
            packages = listOf("com.android.settings"),
            fallbackIntent = Intent(Settings.ACTION_SETTINGS),
            fallbackIcon = android.R.drawable.ic_menu_preferences
        )
        bindAppTile(
            R.id.tile7, R.id.icon7,
            packages = listOf("com.google.android.youtube"),
            fallbackIntent = null,
            fallbackIcon = android.R.drawable.ic_media_ff
        )
        bindAppTile(
            R.id.tile8, R.id.icon8,
            packages = emptyList(),
            fallbackIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            fallbackIcon = android.R.drawable.stat_sys_data_bluetooth
        )

        bindDockShortcut(R.id.ivDock1, listOf("com.waze", "com.google.android.apps.maps"), android.R.drawable.ic_dialog_map)
        bindDockShortcut(R.id.ivDock2, listOf("com.spotify.music", "com.google.android.apps.youtube.music"), android.R.drawable.ic_media_play)
        bindDockShortcut(R.id.ivDock3, listOf("com.samsung.android.dialer", "com.google.android.dialer"), android.R.drawable.sym_action_call)

        findViewById<ImageView>(R.id.ivDockGrid).setOnClickListener {
            openOriginalHeadunit()
        }
    }

    private fun bindAppTile(
        tileId: Int,
        iconId: Int,
        packages: List<String>,
        fallbackIntent: Intent?,
        fallbackIcon: Int
    ) {
        val tile = findViewById<LinearLayout>(tileId)
        val iconView = findViewById<ImageView>(iconId)
        val installedPkg = packages.firstOrNull { isPackageInstalled(it) }

        if (installedPkg != null) {
            iconView.setImageDrawable(packageManager.getApplicationIcon(installedPkg))
            tile.setOnClickListener {
                packageManager.getLaunchIntentForPackage(installedPkg)?.let { startActivity(it) }
            }
        } else {
            iconView.setImageResource(fallbackIcon)
            tile.setOnClickListener {
                if (fallbackIntent != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "App não instalado no Samsung", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindDockShortcut(iconId: Int, packages: List<String>, fallbackIcon: Int) {
        val iv = findViewById<ImageView>(iconId)
        val installedPkg = packages.firstOrNull { isPackageInstalled(it) }
        if (installedPkg != null) {
            iv.setImageDrawable(packageManager.getApplicationIcon(installedPkg))
            iv.setOnClickListener {
                packageManager.getLaunchIntentForPackage(installedPkg)?.let { startActivity(it) }
            }
        } else {
            iv.setImageResource(fallbackIcon)
        }
    }

    private fun openOriginalHeadunit() {
        try {
            val intent = Intent(this, Class.forName("com.andrerinas.openheadunit.main.MainActivity"))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Erro ao abrir Open Headunit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun setupSpeedometer() {
        speedTracker = GpsSpeedTracker(this) { kmh, hasSignal ->
            gpsActive = hasSignal
            tvSpeedValue.text = kmh.toString()
            updateBatteryStatus()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        } else {
            speedTracker.start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            speedTracker.start()
        }
    }

    private fun setupIgnitionAutomation() {
        powerReceiver = CarPowerReceiver(
            onIgnitionOn = {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                setScreenBrightness(1.0f)
                updateBatteryStatus()
            },
            onIgnitionOff = {
                setScreenBrightness(0.01f)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                updateBatteryStatus()
            }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, filter)
    }

    private fun setScreenBrightness(level: Float) {
        val params = window.attributes
        params.screenBrightness = level
        window.attributes = params
    }

    private fun updateBatteryStatus() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val gpsLabel = if (gpsActive) "GPS OK" else "GPS..."
        tvStatusInfo.text = "$gpsLabel • $pct%"
    }

    private fun enableImmersiveFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveFullscreen()
        updateBatteryStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        speedTracker.stop()
        try {
            unregisterReceiver(powerReceiver)
        } catch (_: Exception) {
        }
    }
}
