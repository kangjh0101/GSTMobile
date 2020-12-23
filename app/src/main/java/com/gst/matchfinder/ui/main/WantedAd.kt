package com.gst.matchfinder.ui.main

import android.content.Context
import android.os.Looper
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import java.util.logging.Handler

class WantedAd(
    val idx: String,
    val user_id: String,
    val op_ntrp: String,
    val op_gender: String,
    val op_howlong: String,
    val court_booked: String,
    val location: String,
    val comment: String) {

    /*companion object {

        fun getListfromJSONArray(wantedListArray: JSONArray, context: Context): ArrayList<WantedAd>{
            val wantedAdList = ArrayList<WantedAd>()

            try{
                (0 until wantedListArray.length()).mapTo(wantedAdList) {
                    WantedAd(wantedListArray.getJSONObject(it).getString("user_id"),
                        wantedListArray.getJSONObject(it).getString("op_ntrp"),
                        wantedListArray.getJSONObject(it).getString("op_gender"),
                        wantedListArray.getJSONObject(it).getString("op_howlong"),
                        wantedListArray.getJSONObject(it).getString("court_booked"),
                        wantedListArray.getJSONObject(it).getString("location"),
                        wantedListArray.getJSONObject(it).getString("comment"))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return wantedAdList
        }

    }*/
}

/*
val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
    Toast.makeText(this@RegisterActivityScroll, String(postData), Toast.LENGTH_SHORT).show()
})
*/