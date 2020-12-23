package com.gst.matchfinder.ui.wanted

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
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
import kotlin.coroutines.CoroutineContext

class MyWantedAdListActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()
    //private lateinit var myID: String
    private lateinit var listView: ListView
    private lateinit var adapter_list: ArrayAdapter<String>


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_wanted_ad_list)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        //myID = intent.getStringExtra(Constants.MY_ID).toString()

        adapter_list = ArrayAdapter(this, R.layout.my_wanted_list_items, Constants.myWantedAdList)

        listView = findViewById<ListView>(R.id.myWantedListView)
        listView.setAdapter(adapter_list)

        listView.setOnItemClickListener { parent, view, position, id ->
            val element = listView.getItemAtPosition(position) // The item that was clicked
            val post_raw: String = element.toString()
//Toast.makeText(this@MyWantedAdListActivity, post_raw, Toast.LENGTH_SHORT).show()
            if(!post_raw.equals("내 공고 확인하기")) {
                val st = StringTokenizer(post_raw, " /")

                val post_id: String = st.nextToken()
                val intent = Intent(this, MyWantedDetailActivity::class.java).apply {
                    putExtra(Constants.POST_ID, post_id)
                    putExtra(Constants.MY_ID, Constants.myID)
                }

                startActivity(intent)
            } else{
//Toast.makeText(this@MyWantedAdListActivity, "what the heck!!", Toast.LENGTH_SHORT).show()
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
            get_my_wanted_list()
            refresh_my_wanted_list(adapter_list)
        }
    }

    private suspend fun get_my_wanted_list(){

        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","my_wanted_ad")
        searchJSONObject.put("user_id", Constants.myID)

        val result: Int = get_my_wanted_list_Helper(searchJSONObject.toString())

        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@MyWantedAdListActivity, "내가 등록한 공고를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            return
        }
    }

    private fun refresh_my_wanted_list(adapter: ArrayAdapter<String>){
        adapter.notifyDataSetChanged()
    }

    private suspend fun get_my_wanted_list_Helper(search_info: String): Int{
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

                    val wanted_ad_list_result: String = searchResultObject.getString("search_result")

                    if (wanted_ad_list_result.equals("success")) {
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS

                        val search_result_list: JSONArray = searchResultObject.getJSONArray("data")
                        val search_result = process_wanted_json_array(search_result_list)

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
                return@withContext Constants.WANTED_LIST_NETWORK_ERROR
            }
        }
    }

    private fun process_wanted_json_array(search_result: JSONArray): Int{

        var return_value = Constants.WANTED_LIST_FAIL

        Constants.myWantedAdList.clear()
        Constants.myWantedAdStruc.clear()

        for (i in 0 until search_result.length()) {
            val item = search_result.getJSONObject(i)
            val list_index = item.getString("post_id")
            val op_ntrp: String = item.getString("op_ntrp")
            val op_gender_raw: String = item.getString("op_gender")
            val op_gender: String
            if(op_gender_raw.equals("b")) op_gender = "성별 무관"
            else if(op_gender_raw.equals("m")) op_gender = "남자만"
            else op_gender = "여자만"
            val op_howlong: String = item.getString("op_howlong")
            val court_booked_raw: String = item.getString("court_booked")
            val court_booked: String
            if(court_booked_raw.equals("y")) court_booked = "코트 있음"
            else court_booked = "코트 없음"
            val location: String = item.getString("location")
            val date_to_close: String = item.getString("date_to_close")
            val comment: String = item.getString("comment")
            val active_raw: String = item.getString("active")
            val active: String
            if(active_raw.equals("y")) active = "진행중인 공고"
            else active = "마감된 공고"

            val temp_struc: MyWantedAd = MyWantedAd(list_index, op_ntrp, op_gender, op_howlong, court_booked, location, date_to_close, comment, active)
            val row_to_insert: String = list_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location + " / " + active

            Constants.myWantedAdList.add(row_to_insert)
            Constants.myWantedAdStruc.add(temp_struc)
        }

        sort_my_wanted_list()

        return_value = Constants.WANTED_LIST_SUCCESS
        return return_value
    }

    private fun sort_my_wanted_list(){
        for(i in 0 until (Constants.myWantedAdStruc.size - 1)){
            for(j in 0 until ((Constants.myWantedAdStruc.size - 1) - i)) {
                if(Constants.myWantedAdStruc[j].idx.toInt() < Constants.myWantedAdStruc[j + 1].idx.toInt()){
                    var tmp_struct: MyWantedAd = Constants.myWantedAdStruc[j]
                    var tmp_msg_str: String = Constants.myWantedAdList[j]
                    Constants.myWantedAdStruc[j] = Constants.myWantedAdStruc[j + 1]
                    Constants.myWantedAdList[j] = Constants.myWantedAdList[j + 1]
                    Constants.myWantedAdStruc[j + 1] = tmp_struct
                    Constants.myWantedAdList[j + 1] = tmp_msg_str
                }
            }
        }
    }

}











