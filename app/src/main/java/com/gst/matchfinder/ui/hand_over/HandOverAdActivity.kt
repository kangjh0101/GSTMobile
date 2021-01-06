package com.gst.matchfinder.ui.hand_over

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

class HandOverAdActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    var handover_comment: String? = null
    var handover_intro: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hand_over_ad)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        val handover_post_btn = findViewById<Button>(R.id.handover_ad_post_button)
        val handover_comment_text = findViewById<EditText>(R.id.handover_comment_input)
        val handover_intro_text = findViewById<EditText>(R.id.handover_intro_input)

        handover_post_btn.setOnClickListener {
            handover_comment = handover_comment_text.text.toString()
            handover_intro = handover_intro_text.text.toString()

            if(handover_intro == null || handover_intro.equals("") || handover_intro!!.length < 4){
                Toast.makeText(this, "제목를 4자 이상 작성하세요", Toast.LENGTH_LONG).show()
            } else if(handover_comment == null || handover_comment.equals("") || handover_comment!!.length < 20){
                Toast.makeText(this, "상세 내용을 20자 이상 작성하세요.", Toast.LENGTH_LONG).show()
            } else {
                launch {
                    upload_handover_info()
                }
            }
        }

    }

    private suspend fun upload_handover_info(){

        val handoverJSONObject = JSONObject()
        handoverJSONObject.put("action","post_handover")
        handoverJSONObject.put("user_id", Constants.myID)
        handoverJSONObject.put("intro", handover_intro)
        handoverJSONObject.put("comment", handover_comment)

        val result: Int = upload_handover_info_Helper(handoverJSONObject.toString())

        if(result == Constants.POST_WANTED_SUCCESS) {
            Toast.makeText(this@HandOverAdActivity, "코트 양도 공고가 등록되었습니다", Toast.LENGTH_LONG).show()
            finish()

        } else{
            Toast.makeText(this@HandOverAdActivity, "코트 양도 공고를 등록하지 못습니다\n다시 시도하세요", Toast.LENGTH_LONG).show()
            return
        }
    }

    private suspend fun upload_handover_info_Helper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var post_wanted_return_val: Int = 0
            val postData: ByteArray = id_info.toByteArray(StandardCharsets.UTF_8)

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
                //val url = URL("https://192.168.0.200:50001/GSTWebAPI/Req_Service")
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
                    val register_result_json: String = stringBuilder.toString()

                    val registerResultObject = JSONObject(register_result_json)
                    val register_result: String = registerResultObject.getString("post_handover_result")

                    if (register_result.equals("success")) {
                        post_wanted_return_val = Constants.POST_WANTED_SUCCESS
                    } else {
                        post_wanted_return_val = Constants.POST_WANTED_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    post_wanted_return_val = Constants.POST_WANTED_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext post_wanted_return_val

            } catch(e: SocketTimeoutException){
                return@withContext Constants.POST_WANTED_NETWORK_ERROR
            } catch(e: Exception){
                return@withContext Constants.POST_WANTED_NETWORK_ERROR
            }
        }
    }















}
























