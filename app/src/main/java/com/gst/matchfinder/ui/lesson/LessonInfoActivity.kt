package com.gst.matchfinder.ui.lesson

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gst.matchfinder.R
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.ui.wanted.MyWantedAd
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

class LessonInfoActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()
    //private lateinit var myID: String
    private lateinit var listView: ListView
    private lateinit var adapter_list: ArrayAdapter<String>

    private var lesson_location: Int? = 0

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_info)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        var bundle :Bundle ?=intent.extras
        val coarse_location_tmp: Int? = bundle?.getInt("lesson_coarse_location", 0)
        lesson_location = coarse_location_tmp

        adapter_list = ArrayAdapter(this, R.layout.lesson_info_list_items, Constants.lessonAdList)

        listView = findViewById<ListView>(R.id.lessonInfoListView)
        listView.setAdapter(adapter_list)

        listView.setOnItemClickListener { parent, view, position, id ->
            val element = listView.getItemAtPosition(position) // The item that was clicked
            val post_raw: String = element.toString()

            if(!post_raw.equals("레슨 공고 확인하기")) {
                val st = StringTokenizer(post_raw, " /")
                val post_id: String = st.nextToken()

                val lesson_ad: LessonAd = find_struc_by_post_id(post_id)
                val coarse_location = lesson_ad.coarse_location
                val coach_name = lesson_ad.coach_name
                val coach_phone = lesson_ad.coach_phone
                val coach_addr = lesson_ad.coach_addr
                val intro_string = lesson_ad.intro_string
                val intro_file_name = lesson_ad.intro_file_location

                val intent = Intent(this, LessonInfoDetailActivity::class.java).apply {
                    putExtra("lesson_post_id", post_id)
                    putExtra("coarse_location", coarse_location)
                    putExtra("coach_name", coach_name)
                    putExtra("coach_phone", coach_phone)
                    putExtra("coach_addr", coach_addr)
                    putExtra("intro_string", intro_string)
                    putExtra("intro_file_name", intro_file_name)
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
            if(lesson_location != null && lesson_location!! >=1 && lesson_location!! <= 16){
                get_lesson_list(lesson_location!!)
                refresh_lesson_list(adapter_list)
            }
        }
    }

    private suspend fun get_lesson_list(coarse_location: Int){

        val lessonJSONObject = JSONObject()
        lessonJSONObject.put("action","lesson_ad_list")
        lessonJSONObject.put("user_id", Constants.myID)
        lessonJSONObject.put("coarse_location", lesson_location.toString())

        val result: Int = get_lesson_list_Helper(lessonJSONObject.toString())

        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@LessonInfoActivity, "레슨 공고를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            return
        }
    }

    private fun refresh_lesson_list(adapter: ArrayAdapter<String>){
        adapter.notifyDataSetChanged()
    }

    private suspend fun get_lesson_list_Helper(search_info: String): Int{
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

                    val lesson_ad_list_result: String = searchResultObject.getString("lesson_ad_result")

                    if (lesson_ad_list_result.equals("success")) {
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS

                        val search_result_list: JSONArray = searchResultObject.getJSONArray("data")
                        val search_result = process_lesson_json_array(search_result_list)

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

    private fun process_lesson_json_array(search_result: JSONArray): Int{

        var return_value = Constants.WANTED_LIST_FAIL

        Constants.lessonAdList.clear()
        Constants.lessonAdStruc.clear()

        for (i in 0 until search_result.length()) {
            val item = search_result.getJSONObject(i)
            val list_index: String = item.getString("post_id")
            val coarse_location: String = item.getString("coarse_location")
            val coach_name: String = item.getString("coach_name")
            val coach_phone: String = item.getString("coach_phone")
            val coach_addr: String = item.getString("coach_addr")
            var intro_string: String = item.getString("intro_string")
            val intro_file_name: String = item.getString("intro_file_name")
//Log.e("lesson_retrieve", list_index + " / " + coarse_location + " / " + coach_name + " / " + coach_phone + " / " + coach_addr + " / " + intro_string + " / " + intro_file_name)
            val temp_struc = LessonAd(list_index, coarse_location, coach_name, coach_phone, coach_addr, intro_string, intro_file_name)
            intro_string = list_index + " " + intro_string
            Constants.lessonAdList.add(intro_string)
            Constants.lessonAdStruc.add(temp_struc)
        }

        sort_my_wanted_list()

        return_value = Constants.WANTED_LIST_SUCCESS

        return return_value
    }

    private fun sort_my_wanted_list(){
        for(i in 0 until (Constants.lessonAdStruc.size - 1)){
            for(j in 0 until ((Constants.lessonAdStruc.size - 1) - i)) {
                if(Constants.lessonAdStruc[j].idx.toInt() > Constants.lessonAdStruc[j + 1].idx.toInt()){
                    var tmp_struct: LessonAd = Constants.lessonAdStruc[j]
                    var tmp_msg_str: String = Constants.lessonAdList[j]
                    Constants.lessonAdStruc[j] = Constants.lessonAdStruc[j + 1]
                    Constants.lessonAdList[j] = Constants.lessonAdList[j + 1]
                    Constants.lessonAdStruc[j + 1] = tmp_struct
                    Constants.lessonAdList[j + 1] = tmp_msg_str
                }
            }
        }
    }

    private fun find_struc_by_post_id(post_id: String): LessonAd{
        var lessonAd = LessonAd("0", "coarse_location", "coach_name", "coach_phone", "coach_addr", "intro_string", "intro_file_name")

        for(i in 0 until (Constants.lessonAdStruc.size)){
            val struc_post_id = Constants.lessonAdStruc[i].idx.toInt()
            val post_id_to_request = post_id.toInt()

            if(struc_post_id == post_id_to_request){
                lessonAd = Constants.lessonAdStruc[i]
                break
            }
        }

        return lessonAd
    }







}

















