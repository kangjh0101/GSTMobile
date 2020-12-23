package com.gst.matchfinder.ui.wanted

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.ui.message.ViewMessageActivity
import kotlinx.coroutines.*
import org.json.JSONArray
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

class MyWantedDetailActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    lateinit var post_id: String
    private lateinit var myID: String
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_wanted_detail)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        post_id = intent.getStringExtra(Constants.POST_ID).toString()
        myID = intent.getStringExtra(Constants.MY_ID).toString()

        val ntrp_text = findViewById<TextView>(R.id.my_wanted_detail_ntrp)
        val howlong_text = findViewById<TextView>(R.id.my_wanted_detail_howlong)
        val gender_text = findViewById<TextView>(R.id.my_wanted_detail_gender)
        val location_text = findViewById<TextView>(R.id.my_wanted_detail_location)
        val court_text = findViewById<TextView>(R.id.my_wanted_detail_court)
        val date_to_close_txt = findViewById<TextView>(R.id.my_wanted_detail_date_to_close)
        val comment_text = findViewById<TextView>(R.id.my_wanted_detail_comment)
        val close_post_btn = findViewById<Button>(R.id.close_ad_btn)

        for(each in Constants.myWantedAdStruc) {
            val ad_id: String = each.idx

            if(ad_id.equals(post_id)){

                ntrp_text.setText(each.op_ntrp)
                howlong_text.setText(each.op_howlong)
                gender_text.setText(each.op_gender)
                location_text.setText(each.location)
                court_text.setText(each.court_booked)
                date_to_close_txt.setText(each.date_to_close)
                comment_text.setText(each.comment)
                if(each.active.equals("마감된 공고")){
                    close_post_btn.setText("마감된 공고입니다")
                    close_post_btn.isEnabled = false
                }

                break
            }
        }

        close_post_btn.setOnClickListener {
            launch {
                close_my_post(post_id, myID)
            }
        }

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.MyAdPageAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@MyWantedDetailActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/
    }

    private suspend fun close_my_post(post_id: String, myID: String){

        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","close_my_wanted_ad")
        searchJSONObject.put("user_id", myID)
        searchJSONObject.put("post_id", post_id)

        val result: Int = close_my_post_Helper(searchJSONObject.toString())
//val result: Int = Constants.WANTED_LIST_SUCCESS
        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@MyWantedDetailActivity, "공고를 마감하지 못했습니다\n잠시 후 다시 시도하십시요", Toast.LENGTH_LONG).show()
            return
        } else{
            val close_post_btn = findViewById<Button>(R.id.close_ad_btn)

            close_post_btn.setText("마감된 공고입니다")
            close_post_btn.isEnabled = false

            var index: Int = 0
            for(each in Constants.myWantedAdStruc) {
                val ad_id: String = each.idx

                if(ad_id.equals(post_id)){

                    val idx = each.idx
                    val op_ntrp = each.op_ntrp
                    val op_howlong = each.op_howlong
                    val op_gender = each.op_gender
                    val location = each.location
                    val court_booked = each.court_booked
                    val date_to_close = each.date_to_close
                    val comment = each.comment
                    val active = "마감된 공고"

                    val temp_struc: MyWantedAd = MyWantedAd(idx, op_ntrp, op_gender, op_howlong, court_booked, location, date_to_close, comment, active)
                    val row_to_insert: String = idx + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location + " / " + active

                    Constants.myWantedAdStruc[index] = temp_struc
                    Constants.myWantedAdList[index] = row_to_insert
                }

                index++
            }
        }
    }

    private suspend fun close_my_post_Helper(search_info: String): Int{
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

                    val wanted_ad_list_result: String = searchResultObject.getString("close_result")

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

/***********************************************************************
Toast.makeText(this@MyWantedDetailActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@MyWantedDetailActivity, String(postData), Toast.LENGTH_SHORT).show()
})

 ***********************************************************************/















