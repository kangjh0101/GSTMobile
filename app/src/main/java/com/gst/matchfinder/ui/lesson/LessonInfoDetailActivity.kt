package com.gst.matchfinder.ui.lesson

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.coroutines.CoroutineContext

class LessonInfoDetailActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    lateinit var post_id: String
    lateinit var coarse_location: String
    lateinit var coach_name: String
    lateinit var coach_phone: String
    lateinit var coach_addr: String
    lateinit var intro_string: String
    lateinit var intro_file_name: String

    private lateinit var mAdView: AdView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_info_detail)

        post_id = intent.getStringExtra("lesson_post_id").toString()
        coarse_location = intent.getStringExtra("coarse_location").toString()
        coach_name = intent.getStringExtra("coach_name").toString()
        coach_phone = intent.getStringExtra("coach_phone").toString()
        coach_addr = intent.getStringExtra("coach_addr").toString()
        intro_string = intent.getStringExtra("intro_string").toString()
        intro_file_name = intent.getStringExtra("intro_file_name").toString()

        val intro_str_text = findViewById<TextView>(R.id.lesson_intro_string)
        val coach_name_text = findViewById<TextView>(R.id.lesson_coach_name)
        val coach_phone_text = findViewById<TextView>(R.id.lesson_coach_phone)
        val coach_addr_text = findViewById<TextView>(R.id.lesson_coach_addr)
        val lesson_pic = findViewById<ImageView>(R.id.lesson_pic)

        intro_str_text.setText(intro_string)
        coach_name_text.setText(coach_name)
        coach_phone_text.setText(coach_phone)
        coach_addr_text.setText(coach_addr)

        launch {
            get_lesson_detail(post_id)
        }

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.lessonDetailPageAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private suspend fun get_lesson_detail(post_id: String){

        val lessonJSONObject = JSONObject()
        lessonJSONObject.put("action","lesson_detail")
        lessonJSONObject.put("user_id", Constants.myID)
        lessonJSONObject.put("post_id", post_id)

        val result: String? = get_lesson_detail_Helper(lessonJSONObject.toString())

        if(result == null) {
            Toast.makeText(this@LessonInfoDetailActivity, "레슨 상세 정보를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            return
        } else{
            val image_write_result: Boolean = write_image_to_disk(result, post_id)
            
            if(image_write_result == false){
                Toast.makeText(this@LessonInfoDetailActivity, "레슨 상세 정보를 표시하지 못했습니다", Toast.LENGTH_LONG).show()
                return
            }
        }
    }

    private fun write_image_to_disk(image_array_str: String, post_id: String): Boolean{

        var image_write_result: Boolean = false

        try {
            val file_path_to_save: String = applicationContext.getFilesDir().getPath().toString() + "/" + post_id + ".jpg"

            var file_byte_array = Base64.decode(image_array_str, Base64.DEFAULT)

            val fos = FileOutputStream(file_path_to_save)
            fos.write(file_byte_array)
            fos.close()

            var imgFile = File(file_path_to_save)

            val myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            val lesson_pic = findViewById<ImageView>(R.id.lesson_pic)
            lesson_pic.setImageBitmap(myBitmap)

            image_write_result = true
            //}
        } catch(e: Exception){
            e.printStackTrace()
        }

        return image_write_result
    }

    private suspend fun get_lesson_detail_Helper(search_info: String): String?{
        return withContext(Dispatchers.Default){ // withContext - suspends until it completes and returns results
            // withContext might be replaced by withTimeout which suspends only for given time period
            var get_list_return_val: String? = null
            val postData: ByteArray = search_info.toByteArray(StandardCharsets.UTF_8)

            // Load CAs from an InputStream
            val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
            val caInput: InputStream = ByteArrayInputStream(Constants.getCert().toByteArray(Charsets.UTF_8))
            val ca: X509Certificate = caInput.use {
                cf.generateCertificate(it) as X509Certificate
            }

            // Create a KeyStore containing our trusted CAs
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType).apply {
                load(null, null)
                setCertificateEntry("ca", ca)
            }

            // Create a TrustManager that trusts the CAs inputStream our KeyStore
            val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
            val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(keyStore)
            }

            // Create an SSLContext that uses our TrustManager
            val context: SSLContext = SSLContext.getInstance("TLS").apply {
                init(null, tmf.trustManagers, null)
            }

            try{
                // Tell the URLConnection to use a SocketFactory from our SSLContext
                val url = URL(Constants.HTTP_PROTOCOL + "://" + Constants.GST_SERVER + ":" + Constants.GST_PORT + Constants.GST_SUB_URL)
                val httpClient = url.openConnection() as HttpsURLConnection
                //httpClient.hostnameVerifier = hostnameVerifier
                //httpClient.setHostnameVerifier(HostnameVerifier { hostname, session -> true }) // this is to make all host name trusted
                httpClient.setHostnameVerifier(HostnameVerifier { _, session ->
                    HttpsURLConnection.getDefaultHostnameVerifier().run {
                        verify(Constants.GST_SERVER, session)
                    }
                })
                httpClient.sslSocketFactory = context.socketFactory
                httpClient.requestMethod = "POST"
                httpClient.setReadTimeout(Constants.HTTP_TIMEOUT)
                httpClient.setConnectTimeout(Constants.HTTP_TIMEOUT)
                httpClient.doOutput = true
                httpClient.doInput = true
                httpClient.useCaches = false
                httpClient.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                httpClient.setRequestProperty("Accept", "application/json")
                httpClient.connect()

                val outputStream: DataOutputStream = DataOutputStream(httpClient.outputStream)

                outputStream.write(postData)
                outputStream.flush()
                outputStream.close()

                if (httpClient.responseCode == HttpURLConnection.HTTP_OK) {

                    val inputStream: DataInputStream
                    val reader: BufferedReader

                    inputStream = DataInputStream(httpClient.inputStream)
                    reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                    val stringBuilder = StringBuilder()
                    reader.forEachLine { stringBuilder.append(it) }
                    val search_result_json: String = stringBuilder.toString()

                    val searchResultObject = JSONObject(search_result_json)

                    val lesson_ad_list_result: String = searchResultObject.getString("lesson_detail_result")

                    if (lesson_ad_list_result.equals("success")) {

                        get_list_return_val = searchResultObject.getString("data")

                    } else {
                        get_list_return_val = null
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    get_list_return_val = null
                }

                return@withContext get_list_return_val

            } catch(e: SocketTimeoutException){
                return@withContext null
            } catch(e: Exception){
                return@withContext null
            }
        }
    }











}
















