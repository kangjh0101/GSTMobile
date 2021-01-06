package com.gst.matchfinder.ui.hand_over

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.ui.message.ViewMessageActivity
import com.gst.matchfinder.ui.wanted.MyWantedAd
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

class HandOverDetailActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    lateinit var post_id: String
    lateinit var post_user_id: String
    lateinit var intro_string: String
    lateinit var handover_detail: String
    var is_post_mine: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hand_over_detail)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        post_id = intent.getStringExtra("handover_post_id").toString()
        post_user_id = intent.getStringExtra("post_user_id").toString()
        intro_string = intent.getStringExtra("intro_string").toString()
        handover_detail = intent.getStringExtra("handover_detail").toString()

        val handover_intro = findViewById<TextView>(R.id.handover_intro_string)
        val handover_captain = findViewById<TextView>(R.id.handover_name)
        val handover_briefing = findViewById<TextView>(R.id.handover_briefing)
        val handover_msg_button = findViewById<Button>(R.id.handover_msg_btn)

        handover_intro.text = intro_string
        handover_captain.text = post_user_id
        handover_briefing.text = handover_detail
        if(post_user_id == Constants.myID) {
            is_post_mine = true
            handover_msg_button.text = "이 공고 마감하기"
        } else{
            is_post_mine = false
            handover_msg_button.text = "이 사용자에게 메세지 보내기"
        }

        handover_msg_button.setOnClickListener {

            if(is_post_mine) {
                launch {
                    close_handover(post_id, Constants.myID)
                }
            } else{
                val intent = Intent(this, ViewMessageActivity::class.java).apply {
                    putExtra(Constants.RECEIVER_ID, post_user_id)
                    putExtra(Constants.MY_ID, Constants.myID)
                }

                startActivity(intent)
            }
        }

    }

    private suspend fun close_handover(post_id: String, myID: String){

        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","close_handover")
        searchJSONObject.put("user_id", myID)
        searchJSONObject.put("post_id", post_id)

        val result: Int = close_handover_Helper(searchJSONObject.toString())
//val result: Int = Constants.WANTED_LIST_SUCCESS
        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@HandOverDetailActivity, "공고를 마감하지 못했습니다\n잠시 후 다시 시도하십시요", Toast.LENGTH_LONG).show()
            return
        } else{
            val handover_msg_button = findViewById<Button>(R.id.handover_msg_btn)

            handover_msg_button.setText("마감된 공고입니다")
            handover_msg_button.isEnabled = false
        }
    }

    private suspend fun close_handover_Helper(search_info: String): Int{
        return withContext(Dispatchers.Default){ // withContext - suspends until it completes and returns results
            // withContext might be replaced by withTimeout which suspends only for given time period
            var get_list_return_val: Int = Constants.WANTED_LIST_FAIL
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

                    val wanted_ad_list_result: String = searchResultObject.getString("close_handover_result")

                    if (wanted_ad_list_result.equals("success")) {
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS
                    } else {
                        get_list_return_val = Constants.WANTED_LIST_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    get_list_return_val = Constants.WANTED_LIST_SERVER_ERROR
                }

                return@withContext get_list_return_val

            } catch(e: SocketTimeoutException){
                return@withContext Constants.WANTED_LIST_NETWORK_ERROR
            } catch(e: Exception){
                return@withContext Constants.WANTED_LIST_NETWORK_ERROR
            }
        }
    }









}



















