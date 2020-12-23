package com.gst.matchfinder.util

import android.app.ActivityManager
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.ui.login.LoginPageActivity


class GSTLifeCycleListener(mContext: Context)  : LifecycleObserver {

    private val  context: Context = mContext

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // app moved to foreground
        val currentTimestamp = System.currentTimeMillis()
        val backgrount_time = currentTimestamp - Constants.last_active_time

        //if(backgrount_time > (1000 * 20)){
        if(backgrount_time > (1000 * 60 * 60)){
            //val intent = Intent (context, LoginPageActivity::class.java)
            //context?.startActivity(intent)

            Constants.long_sleep = true

//Log.v("to Foreground", "$backgrount_time")
        } else{
            Constants.long_sleep = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        // app moved to background
        val currentTimestamp = System.currentTimeMillis()
        Constants.last_active_time = currentTimestamp
    }

}

















