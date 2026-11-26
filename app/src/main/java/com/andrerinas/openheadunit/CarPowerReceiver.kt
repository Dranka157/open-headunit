package com.andrerinas.openheadunit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

class CarPowerReceiver(
    private val onIgnitionOn: () -> Unit,
    private val onIgnitionOff: () -> Unit
) : BroadcastReceiver() {

    private val handler = Handler(Looper.getMainLooper())
    private var shutdownRunnable: Runnable? = null

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                shutdownRunnable?.let { handler.removeCallbacks(it) }
                onIgnitionOn()
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                shutdownRunnable?.let { handler.removeCallbacks(it) }
                shutdownRunnable = Runnable {
                    pauseMedia(context)
                    onIgnitionOff()
                }
                handler.postDelayed(shutdownRunnable!!, 5000L)
            }
        }
    }

    private fun pauseMedia(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }
}
