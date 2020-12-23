package com.gst.matchfinder.ui.register

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import kotlinx.android.synthetic.main.activity_register_scroll.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.lang.NumberFormatException
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

class RegisterActivityScroll : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val location_text = findViewById<TextView>(R.id.register_location_text)

        if(requestCode == Constants.PICK_ADDRESS_REQUEST && resultCode == Activity.RESULT_OK) {
            var returned_address: String? = data?.getStringExtra(Constants.MAP_ADDRESS_RESULT_KEY)
            var address_list: List<String>? =
                returned_address?.split(" ", ignoreCase = true, limit = 0)
            var addr_to_register: String = ""
            if (address_list != null) {
                for (x in 0 until 3) {
                    if (x != 0) {
                        if (x == 1) addr_to_register = address_list[x]
                        else addr_to_register = addr_to_register + " " + address_list[x]
                    }
                }

                location_text.setText(addr_to_register)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_scroll)

        this.getSupportActionBar()?.hide()

        location_self_input_text.visibility = View.INVISIBLE
        val register = findViewById<Button>(R.id.register_btn)
        val location_map = findViewById<Button>(R.id.search_location_btn)
        val location_self_input = findViewById<Button>(R.id.location_insert_button)
        val check_id = findViewById<Button>(R.id.check_id_btn)

        check_id.setOnClickListener {
            val check_job = launch {
                checkID()
            }
        }

        register.setOnClickListener {
            val register_job = launch {
                requestRegister()
            }
        }

        location_map.setOnClickListener {
            chooseLocation()
        }

        location_self_input.setOnClickListener {
            insertLocation()
        }

        val spinner: Spinner = findViewById(R.id.ntrp_select)
        ArrayAdapter.createFromResource(
            this,
            R.array.ntrp_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
    }

    private suspend fun checkID(){

        if(Constants.getCheckID()){
            return
        }

        val id_input = findViewById<EditText>(R.id.id_input)
        val id_str: String = id_input.text.toString()

        if(id_str.length < 4 || !(id_str.matches("^[a-zA-Z0-9]*$".toRegex()))){
            Constants.setCheckID(false)
            Toast.makeText(this@RegisterActivityScroll, "아이디는 4자 이상의 영문/숫자 조합이어야 합니다. 특수문자는 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        val chekIDObject= JSONObject()
        chekIDObject.put("action","check_id")
        chekIDObject.put("user_id",id_str)

        val result: Int = checkIDHelper(chekIDObject.toString())

        if(result == 1){
            Constants.setCheckID(true)
            id_input.setText(id_str)
            id_input.isEnabled = false
            Toast.makeText(this@RegisterActivityScroll, "사용가능한 아이디 입니다.", Toast.LENGTH_LONG).show()
            return
        } else if(result == 2){
            Constants.setCheckID(false)
            Toast.makeText(this@RegisterActivityScroll, "다른 아이디를 선택하세요", Toast.LENGTH_LONG).show()
            return
        } else{
            Constants.setCheckID(false)
            Toast.makeText(this@RegisterActivityScroll, "다시 시도해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
    }

    private suspend fun checkIDHelper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var checkid_return_val: Int = 0
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

            // Create an HostnameVerifier that hardwires the expected hostname.
            // following is not needed because verify method is overridden with the following line
            //      httpClient.setHostnameVerifier(HostnameVerifier { hostname, session -> true })
            // the line above overrides verify method so that it always returns TRUE.
            // and it replaces the following line after val httpClient = url.openConnection() as HttpsURLConnection
            //      httpClient.hostnameVerifier = hostnameVerifier
            // see below for complete code snippet
            /*val hostnameVerifier = HostnameVerifier { _, session ->
                HttpsURLConnection.getDefaultHostnameVerifier().run {
                    verify("192.168.0.200", session)
                }
            }*/

            try{
                // following is "HTTPS" connction
                // for "HTTP" connection, refer to the code snippet at the end of file
                // Tell the URLConnection to use a SocketFactory from our SSLContext
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
                    val check_id_result_json: String = stringBuilder.toString()

                    val check_idResultObject = JSONObject(check_id_result_json)
                    val check_id_result: String = check_idResultObject.getString("check_id_result")

                    if (check_id_result.equals("valid")) {
                        checkid_return_val = Constants.CHECK_ID_VALID
                    } else {
                        checkid_return_val = Constants.CHECK_ID_INVALID
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    checkid_return_val = Constants.CHECK_ID_ERROR
                }

                return@withContext checkid_return_val

            } catch(e: SocketTimeoutException){
                return@withContext Constants.CHECK_ID_ERROR
            } catch(e: Exception){
                return@withContext Constants.CHECK_ID_ERROR
            }
        }
    }

    private suspend fun requestRegister(){
        val name_input = findViewById<EditText>(R.id.name_input)
        val phonenumber_input = findViewById<EditText>(R.id.phonenumber_input)
        val email_input = findViewById<EditText>(R.id.email_input)
        val id_input = findViewById<EditText>(R.id.id_input)
        val password_input = findViewById<EditText>(R.id.password_input)
        val password_input_2 = findViewById<EditText>(R.id.password_input_2)
        val location_text = findViewById<TextView>(R.id.register_location_text)
        val location_self_input_text = findViewById<EditText>(R.id.location_self_input_text)
        val gender_btn_grp = findViewById<RadioGroup>(R.id.gender_btn_grp)
        val year_input = findViewById<EditText>(R.id.year_input)
        val ntrp_select = findViewById<Spinner>(R.id.ntrp_select)


        /**************  Get values from input  **************/
        /**************  Get values from input  **************/
        val name_str: String = name_input.text.toString()
        val phonenumber_str: String = phonenumber_input.text.toString().trim()
        val email_str: String = email_input.text.toString().trim()
        val id_str: String = id_input.text.toString()
        val password_str: String = password_input.text.toString()
        val password_str_2: String = password_input_2.text.toString()
        val location_str: String
        // location
        if (search_location_btn.isEnabled == true) {
            location_str = location_text.text.toString()
        } else {
            location_str = location_self_input_text.text.toString()
        }
        // gender
        val gender_btn_id = gender_btn_grp.checkedRadioButtonId
        val gender_btn:RadioButton = findViewById(gender_btn_id)
        val gender_str: String = gender_btn.text.toString()
        val year_str: String = year_input.text.toString()
        val ntrp_str: String = ntrp_select.selectedItem.toString()


        /**************  Check values  **************/


        /**************  Check values  **************/
        if(name_str.length == 0){
            Toast.makeText(this@RegisterActivityScroll, "이름을 정확하게 입력하세요", Toast.LENGTH_LONG).show()
            return
        }

        if(phonenumber_str.length != 11 || !phonenumber_str.matches("[0-9]+".toRegex())){
            Toast.makeText(this@RegisterActivityScroll, "전화번호를 정확하게 입력 하세요 (- 없이 입력)", Toast.LENGTH_LONG).show()
            return
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email_str).matches()){
            Toast.makeText(this@RegisterActivityScroll, "이메일을 정확하게 입력 하세요", Toast.LENGTH_LONG).show()
            return
        }

        if(id_str.length < 4 || !(id_str.matches("^[a-zA-Z0-9]*$".toRegex()))){
            Constants.setCheckID(false)
            // id check must be done again somewhere around here. need to implement checkIDHelper_2.
            // id check must be done again somewhere around here. need to implement checkIDHelper_2.
            // id check must be done again somewhere around here. need to implement checkIDHelper_2.
            // id check must be done again somewhere around here. need to implement checkIDHelper_2.
            // id check must be done again somewhere around here. need to implement checkIDHelper_2.
            Toast.makeText(this@RegisterActivityScroll, "아이디는 4자 이상의 영문/숫자 조합이어야 합니다. 특수문자는 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        if(password_str.length < 4){
            Toast.makeText(this@RegisterActivityScroll, "Password가 너무 짧습니다.", Toast.LENGTH_LONG).show()
            return
        }

        if(!password_str.equals(password_str_2)){
            Toast.makeText(this@RegisterActivityScroll, "Password가 일치하지 않습니다.", Toast.LENGTH_LONG).show()
            return
        }

        if(location_str.length < 5){
            Toast.makeText(this@RegisterActivityScroll, "지역을 정확하게 입력하세요.", Toast.LENGTH_LONG).show()
            return
        }

        val how_many_years: Double
        try{
            how_many_years = year_str.toDouble()
        } catch(e: NumberFormatException){
            Toast.makeText(this@RegisterActivityScroll, "구력을 정확하게 입력하세요.", Toast.LENGTH_LONG).show()
            return
        } catch(e: IllegalArgumentException){
            Toast.makeText(this@RegisterActivityScroll, "구력을 정확하게 입력하세요.", Toast.LENGTH_LONG).show()
            return
        }

        if(!Constants.getCheckID()){
            Toast.makeText(this@RegisterActivityScroll, "아이디를 체크해 주세요", Toast.LENGTH_LONG).show()
            return
        }

        val registerIDObject= JSONObject()
        registerIDObject.put("action","register")
        registerIDObject.put("user_name", name_str)
        registerIDObject.put("phone_number", phonenumber_str)
        registerIDObject.put("email", email_str)
        registerIDObject.put("user_id", id_str)
        registerIDObject.put("password", password_str)
        registerIDObject.put("location", location_str)
        registerIDObject.put("howlong", year_str)
        registerIDObject.put("ntrp", ntrp_str)
        if(gender_str.equals("남")) registerIDObject.put("gender", "m")
        else registerIDObject.put("gender", "f")

        val result: Int = registerHelper(registerIDObject.toString())

        if(result == 1){
            Constants.setCheckID(false)
            Toast.makeText(this@RegisterActivityScroll, "가입되었습니다.", Toast.LENGTH_LONG).show()

            //return
            finish()
        } else{
            Constants.setCheckID(false)
            Toast.makeText(this@RegisterActivityScroll, "다시 시도해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
    }

    private suspend fun registerHelper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var register_return_val: Int = 0
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

            // Create an HostnameVerifier that hardwires the expected hostname.
            // following is not needed because verify method is overridden with the following line
            //      httpClient.setHostnameVerifier(HostnameVerifier { hostname, session -> true })
            // the line above overrides verify method so that it always returns TRUE.
            // and it replaces the following line after val httpClient = url.openConnection() as HttpsURLConnection
            //      httpClient.hostnameVerifier = hostnameVerifier
            // see below for complete code snippet
            /*val hostnameVerifier = HostnameVerifier { _, session ->
                HttpsURLConnection.getDefaultHostnameVerifier().run {
                    verify("192.168.0.200", session)
                }
            }*/

            try{
                // following is "HTTPS" connction
                // for "HTTP" connection, refer to the code snippet at the end of file
                // Tell the URLConnection to use a SocketFactory from our SSLContext
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
                    val register_result: String = registerResultObject.getString("result")

                    if (register_result.equals("success")) {
                        register_return_val = Constants.REGISTER_SUCCESS
                    } else {
                        register_return_val = Constants.REGISTER_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    register_return_val = Constants.REGISTER_SERVER_ERROR
                }

                return@withContext register_return_val

            } catch(e: SocketTimeoutException){
                return@withContext Constants.REGISTER_NETWORK_ERROR
            } catch(e: Exception){
                return@withContext Constants.REGISTER_NETWORK_ERROR
            }
        }
    }

    private fun chooseLocation(){
        val intent = Intent(this, RegisterMyLocation::class.java).apply {
            //putExtra(EXTRA_MESSAGE, message)
        }
        //startActivity(intent)
        startActivityForResult(intent, Constants.PICK_ADDRESS_REQUEST)
    }

    private fun insertLocation(){

        if(search_location_btn.isEnabled == true) {
            search_location_btn.isEnabled = false
            search_location_btn.isClickable = false
            register_location_text.visibility = View.INVISIBLE
            location_self_input_text.visibility = View.VISIBLE
        } else{
            search_location_btn.isEnabled = true
            search_location_btn.isClickable = true
            location_self_input_text.visibility = View.INVISIBLE
            register_location_text.visibility = View.VISIBLE
        }
    }
}

/*
val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
    Toast.makeText(this@RegisterActivityScroll, String(postData), Toast.LENGTH_SHORT).show()
})
*/









