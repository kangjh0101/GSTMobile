package com.gst.matchfinder.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ProcessLifecycleOwner
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.data.Constants.Companion.wantedAdList
import com.gst.matchfinder.data.Constants.Companion.wantedAdStruc
import com.gst.matchfinder.ui.register.RegisterMyLocation
import com.gst.matchfinder.ui.wanted.WantedDetailActivity
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

class MainPageActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    //private lateinit var myID: String
    private lateinit var listView: ListView
    private lateinit var adapter_list: ArrayAdapter<String>
    //private var doubleBackToExitPressedOnce = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        val location_text = findViewById<TextView>(R.id.wanted_list_location_text)

        if(requestCode == Constants.PICK_ADDRESS_REQUEST && resultCode == Activity.RESULT_OK){
            var returned_address: String? = data?.getStringExtra(Constants.MAP_ADDRESS_RESULT_KEY)
            var address_list: List<String>? = returned_address?.split(" ", ignoreCase = true, limit = 0)
            var addr_to_register: String = ""
            if (address_list != null) {
                for (x in 0 until 3){
                    if(x != 0) {
                        if(x == 1) addr_to_register = address_list[x]
                        else addr_to_register = addr_to_register + " " + address_list[x]
                    }
                }

                location_text.setText(addr_to_register)
            }
        }

    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@MainPageActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/

        launch {
            get_wanted_list()
            refresh_wanted_list(adapter_list)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        this.getSupportActionBar()?.hide()

        //ProcessLifecycleOwner.get().lifecycle.addObserver(GSTLifeCycleListener(this))

        //Constants.myID = intent.getStringExtra(Constants.MY_ID).toString()

        //var sender_id: String
        //var message: String

        adapter_list = ArrayAdapter(this, R.layout.wanted_list_items, wantedAdList)

        listView = findViewById<ListView>(R.id.wantedAdListView)
        listView.setAdapter(adapter_list)

        listView.setOnItemClickListener { parent, view, position, id ->
            val element = listView.getItemAtPosition(position) // The item that was clicked
            val post_raw: String = element.toString()
            val st = StringTokenizer(post_raw, " /")

            val post_id: String = st.nextToken()
            val intent = Intent(this, WantedDetailActivity::class.java).apply{
                putExtra(Constants.POST_ID, post_id)
                //putExtra(Constants.MY_ID, myID)
            }

            startActivity(intent)
        }

        val location_map = findViewById<Button>(R.id.wanted_list_location_btn)
        val search_wanted = findViewById<Button>(R.id.wanted_list_search_btn)
        //val post_wanted = findViewById<Button>(R.id.wanted_post_btn)
        //val my_wanted_list = findViewById<Button>(R.id.my_wanted_list_btn)
        val refresh_wanted_list = findViewById<Button>(R.id.refresh_wanted_list_btn)
        //val check_message = findViewById<Button>(R.id.check_msg_btn)

        //post_wanted.setOnClickListener {
        //    postWantedAd()
        //}

        location_map.setOnClickListener {
            chooseLocation()
        }

        search_wanted.setOnClickListener {
            val search_job = launch {
                search_wanted_list()
                refresh_wanted_list(adapter_list)
            }
        }

        refresh_wanted_list.setOnClickListener {
            val refresh_job = launch {
                get_wanted_list()
                refresh_wanted_list(adapter_list)
            }
        }

        /*my_wanted_list.setOnClickListener{
            val refresh_job = launch {
                get_my_wanted_list()
            }
        }*/

        /*check_message.setOnClickListener{
            val check_msg_job = launch {
                check_messages()
            }
        }*/

        val spinner1: Spinner = findViewById(R.id.search_ntrp)
        ArrayAdapter.createFromResource(
            this,
            R.array.search_ntrp_array,
            android.R.layout.simple_spinner_item
        ).also { adapter1 ->
            // Specify the layout to use when the list of choices appears
            adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner1.adapter = adapter1
        }

        val spinner2: Spinner = findViewById(R.id.search_howlong)
        ArrayAdapter.createFromResource(
            this,
            R.array.search_howlong_array,
            android.R.layout.simple_spinner_item
        ).also { adapter2 ->
            // Specify the layout to use when the list of choices appears
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner2.adapter = adapter2
        }

        val spinner3: Spinner = findViewById(R.id.search_gender)
        ArrayAdapter.createFromResource(
            this,
            R.array.search_gender_array,
            android.R.layout.simple_spinner_item
        ).also { adapter3 ->
            // Specify the layout to use when the list of choices appears
            adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner3.adapter = adapter3
        }

        val spinner4: Spinner = findViewById(R.id.search_court)
        ArrayAdapter.createFromResource(
            this,
            R.array.search_court_book_array,
            android.R.layout.simple_spinner_item
        ).also { adapter4 ->
            // Specify the layout to use when the list of choices appears
            adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner4.adapter = adapter4
        }
    }

    /*override fun onBackPressed() {
        if(doubleBackToExitPressedOnce) {
            super.onBackPressed()
            ActivityCompat.finishAffinity(this)
        }

        this.doubleBackToExitPressedOnce = true

        Toast.makeText(this, "뒤로가기를 한 번 더 누르면 앱이 종료됩니다", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }*/

    /*private fun check_messages(){

        val intent = Intent(this, MessageBoxActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }*/

    /*private fun get_my_wanted_list(){
        val intent = Intent(this, MyWantedAdListActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }*/

    /*private fun postWantedAd(){
        val intent = Intent(this, WantedAdActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }*/

    private fun chooseLocation(){
        val intent = Intent(this, RegisterMyLocation::class.java).apply {
            //putExtra(EXTRA_MESSAGE, message)
        }
        //startActivity(intent)
        startActivityForResult(intent, Constants.PICK_ADDRESS_REQUEST)
    }

    private fun refresh_wanted_list(adapter: ArrayAdapter<String>){
        adapter.notifyDataSetChanged()
    }

    private suspend fun get_wanted_list(){

        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","wanted_ad_list")
        searchJSONObject.put("user_id",Constants.myID)

        val result: Int = get_wanted_list_Helper(searchJSONObject.toString())

        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@MainPageActivity, "진행중인 공고를 표시할 수 없습니다", Toast.LENGTH_LONG).show()
            return
        } else{
            val refresh_button = findViewById<Button>(R.id.refresh_wanted_list_btn)
            refresh_button.setText("모집공고 새로고침")
        }
    }

    private suspend fun get_wanted_list_Helper(search_info: String): Int{
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
        //var list_index: Int = 0

        wantedAdList.clear()
        wantedAdStruc.clear()

        for (i in 0 until search_result.length()) {
            val item = search_result.getJSONObject(i)
            val list_index = item.getString("post_id")
            val user_id: String = item.getString("user_id")
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
            val comment: String = item.getString("comment")

            val temp_struc: WantedAd = WantedAd(list_index, user_id, op_ntrp, op_gender, op_howlong, court_booked, location, comment)
            val row_to_insert: String = list_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location

            wantedAdList.add(row_to_insert)
            wantedAdStruc.add(temp_struc)
            //list_index = list_index + 1
        }

        sort_wanted_list()

        return_value = Constants.WANTED_LIST_SUCCESS
        return return_value
    }

    private fun sort_wanted_list(){
        for(i in 0 until (wantedAdStruc.size - 1)){
            for(j in 0 until ((wantedAdStruc.size - 1) - i)) {
                if(wantedAdStruc[j].idx.toInt() < wantedAdStruc[j + 1].idx.toInt()){
                    var tmp_struct: WantedAd = wantedAdStruc[j]
                    var tmp_msg_str: String = wantedAdList[j]
                    wantedAdStruc[j] = wantedAdStruc[j + 1]
                    wantedAdList[j] = wantedAdList[j + 1]
                    wantedAdStruc[j + 1] = tmp_struct
                    wantedAdList[j + 1] = tmp_msg_str
                }
            }
        }
    }

    private fun search_wanted_list(){

        val location_input = findViewById<TextView>(R.id.wanted_list_location_text)
        val ntrp_input = findViewById<Spinner>(R.id.search_ntrp)
        val howlong_input = findViewById<Spinner>(R.id.search_howlong)
        val gender_input = findViewById<Spinner>(R.id.search_gender)
        val court_input = findViewById<Spinner>(R.id.search_court)

        var court_no_matter: Boolean = false

        var location_str: String = location_input.text.toString()
        if(location_str.equals("왼쪽 버튼으로 지역 선택") || location_str == null || location_str.equals("")){
            //Toast.makeText(this@MainPageActivity, "장소를 선택하세요", Toast.LENGTH_LONG).show()
            //return
            location_str = "무관"
        }
        var ntrp_str: String = ntrp_input.selectedItem.toString()
        if(ntrp_str.equals("NTRP")){
            ntrp_str = "무관"
        }
        var howlong_str: String = howlong_input.selectedItem.toString()
        if(howlong_str.equals("구력")){
            howlong_str = "무관"
        }
        val gender_str_raw: String = gender_input.selectedItem.toString()
        val gender_str: String
        if(gender_str_raw.equals("남")) gender_str = "남자만"
        else if(gender_str_raw.equals("여")) gender_str = "여자만"
        else gender_str = "성별 무관"
        val court_str_raw: String = court_input.selectedItem.toString()
        val court_str: String
        if(court_str_raw.equals("예약한 코트 있는 회원만 검색")) court_str = "코트 있음"
        else if(court_str_raw.equals("예약한 코트 없는 회원만 검색")) court_str = "코트 없음"
        else{
            court_no_matter = true
            court_str = "무관"
        }

        wantedAdList.clear()

        if(court_no_matter){ // 코트 예약 상관 없이 검색

            var idx: Int = 0

            for(each in wantedAdStruc){
                val row_index: String
                val op_ntrp: String
                val op_gender: String
                val op_howlong: String
                val court_booked: String
                val location: String

                row_index = each.idx
                op_ntrp = each.op_ntrp
                op_gender = each.op_gender
                op_howlong = each.op_howlong
                court_booked = each.court_booked
                location = each.location

                if(gender_str.equals("남자만")){
                    if(howlong_str.equals("무관")){
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    } else{
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    }
                } else if(gender_str.equals("여자만")){
                    if(howlong_str.equals("무관")){
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    } else{
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    }
                } else{
                    if(howlong_str.equals("무관")){
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                wantedAdList.add(row_to_insert)
                            } else{
                                if(location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_ntrp.equals(ntrp_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_ntrp.equals(ntrp_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    } else{
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && location.equals(location_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    }
                }

                idx++
            }

        } else{ // 코트 예약 상관 있음

            var idx: Int = 0

            for(each in wantedAdStruc){
                val row_index: String
                val op_ntrp: String
                val op_gender: String
                val op_howlong: String
                val court_booked: String
                val location: String

                row_index = each.idx
                op_ntrp = each.op_ntrp
                op_gender = each.op_gender
                op_howlong = each.op_howlong
                court_booked = each.court_booked
                location = each.location

                if(gender_str.equals("남자만")){
                    if(howlong_str.equals("무관")){
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    } else{
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    }
                } else if(gender_str.equals("여자만")){
                    if(howlong_str.equals("무관")){
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    } else{
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    }
                } else{
                    if(howlong_str.equals("무관")){
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(court_booked.equals(court_str)) {
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_ntrp.equals(ntrp_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_ntrp.equals(ntrp_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    } else{
                        if(ntrp_str.equals("무관")){
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        } else{
                            if(location_str.equals("무관")){
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            } else{
                                if(op_howlong.equals(howlong_str) && op_ntrp.equals(ntrp_str) && location.equals(location_str) && court_booked.equals(court_str)){
                                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                                    wantedAdList.add(row_to_insert)
                                }
                            }

                        }

                    }
                }

                /*if(op_ntrp.equals(ntrp_str) && op_gender.equals(gender_str) && op_howlong.equals(howlong_str) && court_booked.equals(court_str) && location.equals(location_str)){
                    val row_to_insert: String = row_index + " / " + op_ntrp + " / " + op_gender + " / " + op_howlong + " / " + court_booked + " / " + location
                    wantedAdList.add(row_to_insert)
                }*/

                idx++
            }
        }
    }
}


/*********************************************************
Toast.makeText(this@MainPageActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@MainPageActivity, String(postData), Toast.LENGTH_SHORT).show()
})

// get device dimensions
val displayMetrics = DisplayMetrics()
windowManager.defaultDisplay.getMetrics(displayMetrics)

var width = displayMetrics.widthPixels
var width_dp = width / displayMetrics.density
var height = displayMetrics.heightPixels
var height_dp = height / displayMetrics.density





listView = findViewById<ListView>(R.id.wantedAdListView)

wantedAdList = WantedAd.getListfromJSONArray(JSONArray("[{\"user_id\":\"사용자 ID\", \"op_ntrp\":\"NTRP\", \"op_gender\":\"성별\", \"op_howlong\":\"구력\", \"court_booked\":\"코트 예약\", \"location\":\"장소\", \"comment\":\"매너 필수\n코트비 1/n\"}]"), this)
//val adapter = ArrayAdapter(this, R.layout.wanted_list_items, wantedAdList)
val adapter = WantedListAdaptor(this, wantedAdList)

listView.adapter = adapter
 ********************************************************/






















