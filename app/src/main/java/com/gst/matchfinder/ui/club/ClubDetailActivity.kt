package com.gst.matchfinder.ui.club

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.ui.message.ViewMessageActivity
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

class ClubDetailActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    lateinit var post_id: String
    lateinit var post_user_id: String
    lateinit var intro_string: String
    lateinit var club_detail: String
    lateinit var data_format: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_detail)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        post_id = intent.getStringExtra("club_post_id").toString()
        post_user_id = intent.getStringExtra("post_user_id").toString()
        intro_string = intent.getStringExtra("intro_string").toString()
        club_detail = intent.getStringExtra("club_detail").toString()
        data_format = intent.getStringExtra("data_format").toString()

        val club_intro = findViewById<TextView>(R.id.club_intro_string)
        val club_captain = findViewById<TextView>(R.id.club_captain_name)
        val club_briefing = findViewById<TextView>(R.id.club_briefing)
        val club_msg_button = findViewById<Button>(R.id.club_msg_btn)

        club_intro.text = intro_string
        club_captain.text = post_user_id
        club_briefing.text = club_detail

        if(!data_format.equals("no_pic")) {
            launch {
                get_club_detail(post_id)
            }
        }

        club_msg_button.setOnClickListener {
            val intent = Intent(this, ViewMessageActivity::class.java).apply {
                putExtra(Constants.RECEIVER_ID, post_user_id)
                putExtra(Constants.MY_ID, Constants.myID)
            }

            if(!post_user_id.equals(Constants.myID)) startActivity(intent)
        }


    }

    private suspend fun get_club_detail(post_id: String){

        val lessonJSONObject = JSONObject()
        lessonJSONObject.put("action","club_detail")
        lessonJSONObject.put("user_id", Constants.myID)
        lessonJSONObject.put("post_id", post_id)

        val result: String? = get_club_detail_Helper(lessonJSONObject.toString())

        if(result == null) {
            Toast.makeText(this@ClubDetailActivity, "레슨 상세 정보를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            return
        } else{
            val image_write_result: Boolean = write_image_to_disk(result, post_id)

            if(image_write_result == false){
                Toast.makeText(this@ClubDetailActivity, "레슨 상세 정보를 표시하지 못했습니다", Toast.LENGTH_LONG).show()
                return
            }
        }
    }

    private fun write_image_to_disk(image_array_str: String, post_id: String): Boolean{

        var image_write_result: Boolean = false

        try {
            val file_path_to_save: String = applicationContext.getFilesDir().getPath().toString() + "/club_" + post_id + ".jpg"

            var file_byte_array = Base64.decode(image_array_str, Base64.DEFAULT)

            val fos = FileOutputStream(file_path_to_save)
            fos.write(file_byte_array)
            fos.close()

            var imgFile = File(file_path_to_save)

            val myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            val lesson_pic = findViewById<ImageView>(R.id.club_pic)
            lesson_pic.setImageBitmap(myBitmap)

            image_write_result = true
            //}
        } catch(e: Exception){
            e.printStackTrace()
        }

        return image_write_result
    }

    private suspend fun get_club_detail_Helper(search_info: String): String?{
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































