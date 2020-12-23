package com.gst.matchfinder.ui.lesson

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_BUSAN
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_CHUNGBUK
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_CHUNGNAM
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_DAEGU
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_DAEJON
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_GANGWON
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_GWANGJU
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_GYUNGBUK
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_GYUNGGI
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_GYUNGNAM
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_INCHON
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_JEJU
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_JUNBUK
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_JUNNAM
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_SEOUL
import com.gst.matchfinder.data.Constants.Companion.LESSON_LOCATION_ULSAN
import com.gst.matchfinder.ui.message.MessageBoxActivity
import kotlinx.coroutines.launch

class LessonLocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_location)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        val seoul_page = findViewById<Button>(R.id.seoul_btn)
        val daegu_page = findViewById<Button>(R.id.daegu_btn)
        val busan_page = findViewById<Button>(R.id.busan_btn)
        val ulsan_page = findViewById<Button>(R.id.ulsan_btn)
        val inchon_page = findViewById<Button>(R.id.inchon_btn)
        val daejon_page = findViewById<Button>(R.id.daejon_btn)
        val gwangju_page = findViewById<Button>(R.id.gwangju_btn)
        val jeju_page = findViewById<Button>(R.id.jeju_btn)
        val gyunggi_page = findViewById<Button>(R.id.gyunggi_btn)
        val gangwon_page = findViewById<Button>(R.id.gangwon_btn)
        val chungbuk_page = findViewById<Button>(R.id.chungbuk_btn)
        val chungnam_page = findViewById<Button>(R.id.chungnam_btn)
        val junbuk_page = findViewById<Button>(R.id.junbuk_btn)
        val junnam_page = findViewById<Button>(R.id.junnam_btn)
        val gyungbuk_page = findViewById<Button>(R.id.gyungbuk_btn)
        val gyungnam_page = findViewById<Button>(R.id.gyungnam_btn)

        seoul_page.setOnClickListener{
            open_seoul_page()
        }

        daegu_page.setOnClickListener{
            open_daegu_page()
        }

        busan_page.setOnClickListener{
            open_busan_page()
        }

        ulsan_page.setOnClickListener{
            open_ulsan_page()
        }

        inchon_page.setOnClickListener{
            open_inchon_page()
        }

        daejon_page.setOnClickListener{
            open_daejon_page()
        }

        gwangju_page.setOnClickListener{
            open_gwangju_page()
        }

        jeju_page.setOnClickListener{
            open_jeju_page()
        }

        gyunggi_page.setOnClickListener{
            open_gyunggi_page()
        }

        gangwon_page.setOnClickListener{
            open_gangwon_page()
        }

        chungbuk_page.setOnClickListener{
            open_chungbuk_page()
        }

        chungnam_page.setOnClickListener{
            open_chungnam_page()
        }

        junbuk_page.setOnClickListener{
            open_junbuk_page()
        }

        junnam_page.setOnClickListener{
            open_junnam_page()
        }

        gyungbuk_page.setOnClickListener{
            open_gyungbuk_page()
        }

        gyungnam_page.setOnClickListener{
            open_gyungnam_page()
        }


    }

    private fun open_seoul_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_SEOUL)
        }
        startActivity(intent)
    }

    private fun open_daegu_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_DAEGU)
        }
        startActivity(intent)
    }

    private fun open_busan_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_BUSAN)
        }
        startActivity(intent)
    }

    private fun open_ulsan_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_ULSAN)
        }
        startActivity(intent)
    }

    private fun open_inchon_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_INCHON)
        }
        startActivity(intent)
    }

    private fun open_daejon_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_DAEJON)
        }
        startActivity(intent)
    }

    private fun open_gwangju_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_GWANGJU)
        }
        startActivity(intent)
    }

    private fun open_jeju_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_JEJU)
        }
        startActivity(intent)
    }

    private fun open_gyunggi_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_GYUNGGI)
        }
        startActivity(intent)
    }

    private fun open_gangwon_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_GANGWON)
        }
        startActivity(intent)
    }

    private fun open_chungbuk_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_CHUNGBUK)
        }
        startActivity(intent)
    }

    private fun open_chungnam_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_CHUNGNAM)
        }
        startActivity(intent)
    }

    private fun open_junbuk_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_JUNBUK)
        }
        startActivity(intent)
    }

    private fun open_junnam_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_JUNNAM)
        }
        startActivity(intent)
    }

    private fun open_gyungbuk_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_GYUNGBUK)
        }
        startActivity(intent)
    }

    private fun open_gyungnam_page(){

        val intent = Intent(this, LessonInfoActivity::class.java).apply {
            putExtra("lesson_coarse_location", LESSON_LOCATION_GYUNGNAM)
        }
        startActivity(intent)
    }





















}



























