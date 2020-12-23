package com.gst.matchfinder.ui.wanted

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.NavUtils
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.ui.main.MainPageActivity
import com.gst.matchfinder.ui.register.RegisterMyLocation
import kotlinx.android.synthetic.main.activity_register_scroll.*
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
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class WantedAdActivity : AppCompatActivity(), CoroutineScope by MainScope()  {

    //private lateinit var myID: String
    private lateinit var mAdView: AdView

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val location_text = findViewById<TextView>(R.id.wanted_location_pick_text)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wanted_ad)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        //myID = intent.getStringExtra(Constants.MY_ID).toString()

        val post_wanted_ad = findViewById<Button>(R.id.post_wanted_ad_button)
        val location_map = findViewById<Button>(R.id.wanted_location_btn)
        val mPickTimeBtn = findViewById<Button>(R.id.wanted_close_date_btn)
        val date_show_text = findViewById<TextView>(R.id.wanted_close_date_show_text)
        val comment_input = findViewById<EditText>(R.id.wanted_comment_input)

        post_wanted_ad.setOnClickListener {
            val register_job = launch {
                postWanted()
            }

            //post_wanted_ad.isEnabled = false
            //comment_input.setText("")
            //NavUtils.navigateUpFromSameTask(this) // by this global variable is reset. need to find some other way.
            //NavUtils.navigateUpTo(this, MainPageActivity::class.java)
            finish()
            return@setOnClickListener
        }

        val spinner1: Spinner = findViewById(R.id.wanted_ntrp_input)
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

        val spinner2: Spinner = findViewById(R.id.wanted_howlong_input)
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

        val spinner3: Spinner = findViewById(R.id.wanted_gender_input)
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

        location_map.setOnClickListener {
            chooseLocation()
        }

        val spinner4: Spinner = findViewById(R.id.wanted_court_booked_input)
        ArrayAdapter.createFromResource(
            this,
            R.array.wanted_court_book_array,
            android.R.layout.simple_spinner_item
        ).also { adapter4 ->
            // Specify the layout to use when the list of choices appears
            adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner4.adapter = adapter4
        }

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        mPickTimeBtn.setOnClickListener {
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val correntMonthOfYear = monthOfYear + 1
                if(correntMonthOfYear < 10){
                    if(dayOfMonth < 10){
                        date_show_text.setText("" + year + "-0" + correntMonthOfYear + "-0" + dayOfMonth)
                    } else{
                        date_show_text.setText("" + year + "-0" + correntMonthOfYear + "-" + dayOfMonth)
                    }
                } else {
                    if(dayOfMonth < 10){
                        date_show_text.setText("" + year + "-" + correntMonthOfYear + "-0" + dayOfMonth)
                    } else{
                        date_show_text.setText("" + year + "-" + correntMonthOfYear + "-" + dayOfMonth)
                    }
                }
            }, year, month, day)
            dpd.show()
        }

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.RegisterWantedPageAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@WantedAdActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/
    }

    private fun chooseLocation(){
        val intent = Intent(this, RegisterMyLocation::class.java).apply {
            //putExtra(EXTRA_MESSAGE, message)
        }
        //startActivity(intent)
        startActivityForResult(intent, Constants.PICK_ADDRESS_REQUEST)
    }

    private suspend fun postWanted(){

        val nrtp_input = findViewById<Spinner>(R.id.wanted_ntrp_input)
        val howlong_input = findViewById<Spinner>(R.id.wanted_howlong_input)
        val gender_input = findViewById<Spinner>(R.id.wanted_gender_input)
        val location_input = findViewById<TextView>(R.id.wanted_location_pick_text)
        val court_booked_input = findViewById<Spinner>(R.id.wanted_court_booked_input)
        val close_date_input = findViewById<TextView>(R.id.wanted_close_date_show_text)
        val comment_input = findViewById<EditText>(R.id.wanted_comment_input)

        var nrtp_input_str: String = nrtp_input.selectedItem.toString()
        if(nrtp_input_str.equals("NTRP")){
            nrtp_input_str = "무관"
        }
        var howlong_input_str: String = howlong_input.selectedItem.toString()
        if(howlong_input_str.equals("구력")){
            howlong_input_str = "무관"
        }
        var gender_input_str: String = gender_input.selectedItem.toString()
        if(gender_input_str.equals("남")){
            gender_input_str = "m"
        } else if(gender_input_str.equals("여")){
            gender_input_str = "f"
        } else{
            gender_input_str = "b"
        }
        val location_input_str: String = location_input.text.toString()
        if(location_input_str == null || location_input_str.equals("")){
            Toast.makeText(this@WantedAdActivity, "장소를 선택하세요", Toast.LENGTH_LONG).show()
            return
        }
        var court_booked_input_str: String = court_booked_input.selectedItem.toString()
        if(court_booked_input_str.equals("예약한 코트 있음")){
            court_booked_input_str = "y"
        } else{
            court_booked_input_str = "n"
        }
        val close_date_input_str: String = close_date_input.text.toString()
        if(close_date_input_str == null || close_date_input_str.equals("")){
            Toast.makeText(this@WantedAdActivity, "모집 마감일을 선택하세요", Toast.LENGTH_LONG).show()
            return
        }
        val comment_input_str: String = comment_input.text.toString()

        val wantedObject= JSONObject()
        wantedObject.put("action","post_wanted_ad")
        wantedObject.put("user_id", Constants.myID)
        wantedObject.put("op_ntrp", nrtp_input_str)
        wantedObject.put("op_gender", gender_input_str)
        wantedObject.put("op_howlong", howlong_input_str)
        wantedObject.put("court_booked", court_booked_input_str)
        wantedObject.put("location", location_input_str)
        wantedObject.put("date_to_close", close_date_input_str)
        wantedObject.put("comment", comment_input_str)

        val result: Int = postWantedHelper(wantedObject.toString())

        if(result == Constants.POST_WANTED_SUCCESS){
            Constants.setCheckID(false)
            Toast.makeText(this@WantedAdActivity, "모집 공고가 등록되었습니다", Toast.LENGTH_LONG).show()
            return
        } else{
            Constants.setCheckID(false)
            Toast.makeText(this@WantedAdActivity, "다시 시도해 주세요", Toast.LENGTH_LONG).show()
            return
        }

        //this@WantedAdActivity.finishAffinity()
    }

    private suspend fun postWantedHelper(id_info: String): Int {
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
                    val register_result: String = registerResultObject.getString("post_result")

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

/*
val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
    Toast.makeText(this@RegisterActivityScroll, String(postData), Toast.LENGTH_SHORT).show()
})
*/














