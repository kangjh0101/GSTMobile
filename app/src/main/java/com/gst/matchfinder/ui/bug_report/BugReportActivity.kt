package com.gst.matchfinder.ui.bug_report

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import javax.net.ssl.*

class BugReportActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug_report)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        val send_bug_report = findViewById<Button>(R.id.bug_report_btn)
        val bug_content_txt = findViewById<EditText>(R.id.bug_content_input)

        send_bug_report.setOnClickListener {
            var bug_content: String = bug_content_txt.text.toString()

            if(bug_content != null && !bug_content.equals("")) {
                launch {
                    send_report(Constants.myID, bug_content)
                }

                finish()
                return@setOnClickListener
            }
        }
    }

    private suspend fun send_report(my_id: String, bug_content: String){
        val msgObject= JSONObject()
        msgObject.put("action","bug_report")
        msgObject.put("user_id", my_id)
        msgObject.put("content",bug_content)

        val result: Int = send_report_helper(msgObject.toString())

        if(result == Constants.SEND_REPORT_SUCCESS){
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this@BugReportActivity, "신고가 접수되었습니다.", Toast.LENGTH_LONG).show()
            })

            return

        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this@BugReportActivity, "신고를 접수하지 못했습니다\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            })

            return
        }
    }

    private suspend fun send_report_helper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var send_report_result: Int = Constants.SEND_REPORT_SUCCESS
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
                    val register_result: String = registerResultObject.getString("bug_report_result")

                    if (register_result.equals("success")) {
                        send_report_result = Constants.SEND_REPORT_SUCCESS
                    } else {
                        send_report_result = Constants.SEND_REPORT_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    send_report_result = Constants.SEND_REPORT_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext send_report_result
            } catch(e: SocketTimeoutException){
                return@withContext Constants.SEND_REPORT_NETWORK_ERROR
            } catch(e: Exception){
                return@withContext Constants.SEND_REPORT_NETWORK_ERROR
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@BugReportActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/
    }



















}