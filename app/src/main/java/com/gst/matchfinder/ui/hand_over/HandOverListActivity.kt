package com.gst.matchfinder.ui.hand_over

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
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
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class HandOverListActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var listView: ListView
    private lateinit var adapter_list: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hand_over_list)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        val handover_ad_page = findViewById<Button>(R.id.post_handover_ad_btn)

        adapter_list = ArrayAdapter(this, R.layout.handover_list_items, Constants.handoverAdList)

        listView = findViewById<ListView>(R.id.handoverListView)
        listView.setAdapter(adapter_list)

        listView.setOnItemClickListener { parent, view, position, id ->
            val element = listView.getItemAtPosition(position) // The item that was clicked
            val post_raw: String = element.toString()

            if(!post_raw.equals("코트 양도 공고 확인하기")) {
                val st = StringTokenizer(post_raw, " /")
                val post_id: String = st.nextToken()

                val handover_ad: HandOver = find_struc_by_post_id(post_id)
                val user_id = handover_ad.user_id
                val intro_string = handover_ad.intro_string
                val handover_detail = handover_ad.handover_detail

                val intent = Intent(this, HandOverDetailActivity::class.java).apply {
                    putExtra("handover_post_id", post_id)
                    putExtra("post_user_id", user_id)
                    putExtra("intro_string", intro_string)
                    putExtra("handover_detail", handover_detail)
                }

                startActivity(intent)
            } else{
//Toast.makeText(this@MyWantedAdListActivity, "what the heck!!", Toast.LENGTH_SHORT).show()
            }
        }

        handover_ad_page.setOnClickListener {
            val lesson_job = launch {
                post_handover_ad()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@MyWantedAdListActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/

        launch {
            get_handover_list()
            refresh_handover_list(adapter_list)
        }
    }

    private fun refresh_handover_list(adapter: ArrayAdapter<String>){
        adapter.notifyDataSetChanged()
    }

    private fun post_handover_ad(){

        val intent = Intent(this, HandOverAdActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }

    private suspend fun get_handover_list(){

        val lessonJSONObject = JSONObject()
        lessonJSONObject.put("action","handover_ad_list")
        lessonJSONObject.put("user_id", Constants.myID)

        val result: Int = get_handover_list_Helper(lessonJSONObject.toString())

        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@HandOverListActivity, "코트 양도 공고를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            return
        }
    }

    private suspend fun get_handover_list_Helper(search_info: String): Int{
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

                    val handover_ad_list_result: String = searchResultObject.getString("handover_ad_result")

                    if (handover_ad_list_result.equals("success")) {
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS

                        val search_result_list: JSONArray = searchResultObject.getJSONArray("data")
                        val search_result = process_handover_json_array(search_result_list)

                        if(search_result == Constants.WANTED_LIST_SUCCESS){
                            get_list_return_val = Constants.WANTED_LIST_SUCCESS
                        } else{
                            get_list_return_val = Constants.WANTED_LIST_FAIL
                        }

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
                e.printStackTrace()
                return@withContext Constants.WANTED_LIST_NETWORK_ERROR
            }
        }
    }

    private fun process_handover_json_array(search_result: JSONArray): Int{

        var return_value = Constants.WANTED_LIST_FAIL

        Constants.handoverAdList.clear()
        Constants.handoverAdStruc.clear()

        for (i in 0 until search_result.length()) {
            val item = search_result.getJSONObject(i)
            val list_index: String = item.getString("post_id")
            val user_id: String = item.getString("user_id")
            var intro_string: String = item.getString("intro_string")
            val handover_detail: String = item.getString("handover_detail")

            val temp_struc = HandOver(list_index, user_id, intro_string, handover_detail)
            intro_string = list_index + " " + intro_string
            Constants.handoverAdList.add(intro_string)
            Constants.handoverAdStruc.add(temp_struc)
        }

        sort_handover_list()

        return_value = Constants.WANTED_LIST_SUCCESS

        return return_value
    }

    private fun sort_handover_list(){
        for(i in 0 until (Constants.handoverAdStruc.size - 1)){
            for(j in 0 until ((Constants.handoverAdStruc.size - 1) - i)) {
                if(Constants.handoverAdStruc[j].idx.toInt() < Constants.handoverAdStruc[j + 1].idx.toInt()){
                    var tmp_struct: HandOver = Constants.handoverAdStruc[j]
                    var tmp_msg_str: String = Constants.handoverAdList[j]
                    Constants.handoverAdStruc[j] = Constants.handoverAdStruc[j + 1]
                    Constants.handoverAdList[j] = Constants.handoverAdList[j + 1]
                    Constants.handoverAdStruc[j + 1] = tmp_struct
                    Constants.handoverAdList[j + 1] = tmp_msg_str
                }
            }
        }
    }

    private fun find_struc_by_post_id(post_id: String): HandOver{
        var handover = HandOver("0", "user_id", "intro_string", "handover_detail")

        for(i in 0 until (Constants.handoverAdStruc.size)){
            val struc_post_id = Constants.handoverAdStruc[i].idx.toInt()
            val post_id_to_request = post_id.toInt()

            if(struc_post_id == post_id_to_request){
                handover = Constants.handoverAdStruc[i]
                break
            }
        }

        return handover
    }


}

















