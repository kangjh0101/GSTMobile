package com.gst.matchfinder.ui.message

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.db.MessageDB
import com.gst.matchfinder.db.MessageEntry
import com.gst.matchfinder.ui.main.MainMenuV2Activity

import kotlinx.coroutines.launch
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class GSTFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage){

        //Constants.broadcastTriggered = false

        //var title: String? = remoteMessage.notification?.title
        //var body: String? = remoteMessage.notification?.body
        var title: String? = remoteMessage.data["title"]
        var body: String? = remoteMessage.data["body"]
        var body_time: String?
        var msg_id: String? = remoteMessage.data["msg_id"]
        //var msg_time_str: String? = remoteMessage.data["msg_timestamp"]

        var c: Calendar = Calendar.getInstance()
        var date: String = (c.get(Calendar.MONTH) + 1).toString() + "/" + c.get(Calendar.DATE).toString()
        var time: String = c.get(Calendar.HOUR_OF_DAY).toString() + ":" + c.get(Calendar.MINUTE).toString()
        var date_time: String = date + " " + time

        val sender_id = "" + title?.split(" ")?.get(0)

        if (title != null && body != null/* && !isDuplicate(remoteMessage.sentTime)*/) {
            sendNotification(title, body, sender_id)
        }
//Log.e("notification", "onMessageReceived")
        if (body != null && msg_id != null) {
            //add_new_chat(body, Constants.chat_adapter_list, Constants.chat_listView, msg_id.toInt(), sender_id)
            /*val msg_box_broadcaster = LocalBroadcastManager.getInstance(baseContext)
            val msg_box_intent: Intent = Intent("message_received_toss_to_messageboxactivity")
            msg_box_intent.putExtra("msg_str", body)
            msg_box_intent.putExtra("msg_id", msg_id)
            msg_box_intent.putExtra("msg_sender", sender_id)
            msg_box_intent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            //msg_box_intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            //msg_box_intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            msg_box_broadcaster.sendBroadcast(msg_box_intent)*/

            val view_msg_broadcaster = LocalBroadcastManager.getInstance(baseContext)
            val view_msg_intent: Intent = Intent("message_received_toss_to_viewmessageactivity")
            view_msg_intent.putExtra("msg_str", body)
            view_msg_intent.putExtra("msg_id", msg_id)
            view_msg_intent.putExtra("msg_sender", sender_id)
            view_msg_intent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            view_msg_broadcaster.sendBroadcast(view_msg_intent)

            //insert_msg_to_db(msg_id, sender_id, body_time)
            insert_msg_to_db(msg_id, sender_id, body, date_time)
        }

        val pm: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
        val result: Boolean = Build.VERSION.SDK_INT>= Build.VERSION_CODES.KITKAT_WATCH && pm.isInteractive()|| Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && pm.isScreenOn()

        //if(!result){
        //val wl: PowerManager.WakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "GST:SCREENLOCK")
        val wl: PowerManager.WakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "GST:SCREENLOCK")
        wl.acquire(10000)
        //val wl_cpu: PowerManager.WakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GST:SCREENLOCK")
        //wl_cpu.acquire(10000)
        //}
    }

    private fun insert_msg_to_db(msg_id: String, sender_id: String, body: String, dat_time: String){

        if(Constants.msg_db == null || !Constants.msg_db!!.isOpen) Constants.msg_db = MessageDB.getInstance(this)

//var data:List<MessageEntry>? = Constants.msg_db?.messageDAO()?.getAllMessages()
//val handler = Handler(Looper.getMainLooper())
//handler.post(Runnable {
//    Toast.makeText(this@GSTFirebaseMessagingService, data.toString(), Toast.LENGTH_LONG).show()
//})
//Log.e("received_msg_1", data.toString())
        val tmp_msg_id: List<Long>? = Constants.msg_db?.messageDAO()?.checkMessagesID(msg_id.toLong())
        if(tmp_msg_id.isNullOrEmpty()) {
            val msgEntry: MessageEntry = MessageEntry(msg_id.toLong(), dat_time, sender_id, "r", body, "n")
            Constants.msg_db?.messageDAO()?.insertAll(msgEntry)
        }
//data = Constants.msg_db?.messageDAO()?.getAllMessages()
//Log.e("received_msg_3", data.toString())
    }

    override fun onNewToken(token: String) {
        //Log.d(TAG, "Refreshed token 2: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)

    }

    /*private fun add_new_chat(new_msg: String, adapter: ArrayAdapter<String>, listView: ListView, msg_id: Int, sender_id: String){
        Constants.myChatting.add("<< 받은 << :  " + new_msg)
        refresh_my_message_list(adapter, listView)

        for(i in 0 until Constants.myMessageStruc.size){
            if(Constants.myMessageStruc[i].receiver_id.equals(sender_id)){
                Constants.myMessageStruc[i].messages!!.add(Message.IndividualMessage(msg_id, "r", new_msg))
                break
            }
        }
    }

    private fun refresh_my_message_list(adapter: ArrayAdapter<String>, listView: ListView){
        adapter.notifyDataSetChanged()
        listView.setSelection(adapter.count - 1)
    }*/

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        //Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(msgTitle: String, msgBody: String, sender_id: String) {

        val vibe_time_array = longArrayOf(300, 300)

        //val intent = Intent(this, GSTFirebaseMessagingService::class.java)
        val intent = Intent(this, MainMenuV2Activity::class.java)
        //intent.putExtra("sender_id", sender_id)
        //intent.putExtra("messsage", msgBody)
        //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        //val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT)
        //val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, 0)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.gst_155x155_02)
            .setContentTitle(msgTitle)
            .setContentText(msgBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(vibe_time_array)
            .setLights(Color.BLUE,1,1)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "GST 메세지",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }


}

