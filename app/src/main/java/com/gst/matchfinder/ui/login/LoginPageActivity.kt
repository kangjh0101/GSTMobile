package com.gst.matchfinder.ui.login

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.data.Constants.Companion.device_token
import com.gst.matchfinder.ui.findIDPW.FindIDPWActivity
import com.gst.matchfinder.ui.main.MainMenuActivity
import com.gst.matchfinder.ui.main.MainMenuV2Activity
import com.gst.matchfinder.ui.register.RegisterActivityScroll
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

class LoginPageActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var mAdView: AdView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        this.getSupportActionBar()?.hide()

        /*val login = findViewById<Button>(R.id.login)
        val register = findViewById<Button>(R.id.register)
        val findIDPW = findViewById<Button>(R.id.find_idpd)*/
        val login = findViewById<ImageButton>(R.id.login)
        val register = findViewById<ImageButton>(R.id.register)
        val findIDPW = findViewById<ImageButton>(R.id.find_idpd)

        FirebaseMessaging.getInstance().subscribeToTopic("new_message");
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(ContentValues.TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log and toast
                val msg = getString(R.string.msg_token_fmt, token)
                //Log.d(ContentValues.TAG, "Refreshed token 1: $token")
//Toast.makeText(this@LoginPageActivity, msg, Toast.LENGTH_LONG).show()
                if (token != null && token != "") {
                    //Log.d(ContentValues.TAG, "token before device_token: $token")
                    device_token = token
                    //device_token = msg
                    //Log.d(ContentValues.TAG, "device_token: $device_token")
                } else{
                    Toast.makeText(this@LoginPageActivity, "Device Token을 가져오지 못했습니다\n다시 시도하세요", Toast.LENGTH_SHORT).show()
                }
            })

        login.setOnClickListener{
            // launch function starts a coroutine which is like a new thread
            // coroutine below can be cancelled by calling login_job.cancel
            // login_job must be cancelled in case of network delay or server error
            val login_job = launch {
                userLogin()
            }
        }

        register.setOnClickListener{
            registserUser()
        }

        findIDPW.setOnClickListener{
            findIDPW()
        }

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.loginPageAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        val currentTimestamp = System.currentTimeMillis()
        Constants.last_active_time = currentTimestamp
        Constants.long_sleep = false

        // auto login
        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val login_id = sharedPref.getString("id_key","no_id")
        val login_pw = sharedPref.getString("pw_key","no_pw")

        if(login_id != null && login_pw != null && !login_id.equals("no_id") && !login_pw.equals("no_pw")){
            val login_job = launch {
                auto_userLogin(login_id, login_pw)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val currentTimestamp = System.currentTimeMillis()
        Constants.last_active_time = currentTimestamp
        Constants.long_sleep = false
    }

    private suspend fun auto_userLogin(login_id: String, login_pw: String){

        val loginObject= JSONObject()
        loginObject.put("action","login")
        loginObject.put("user_id",login_id)
        loginObject.put("password",login_pw)
        loginObject.put("device_token",device_token)
        loginObject.put("version", Constants.app_version)
        //loginObject.put("device_token",token)

        val result = userLoginHelper(loginObject.toString())

        if(result == Constants.LOGIN_SUCCESS){

            Constants.myID = login_id
            Constants.long_sleep = false

            //val intent = Intent(this, MainMenuActivity::class.java).apply {
            val intent = Intent(this, MainMenuV2Activity::class.java).apply {
                //putExtra(Constants.MY_ID, username_input)
            }
            startActivity(intent)
        } else if(result == Constants.LOGIN_UPDATE_REQUIRED){
            //Toast.makeText(this@LoginPageActivity, "앱을 업데이트 해야합니다.", Toast.LENGTH_LONG).show()
            val builder = AlertDialog.Builder(this@LoginPageActivity)
            builder.setTitle("앱을 업데이트 해야 합니다")
            builder.setMessage("업데이트 하시겠습니까?")

            builder.setPositiveButton("예") { dialog, which ->

                var intent: Intent

                try {
                    intent = Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=" + Constants.app_package_name))
                } catch(e: Exception){
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Constants.app_package_name))
                }

                startActivity(intent)
            }
            builder.setNegativeButton("아니요") { dialog, which ->
                finishAffinity()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private suspend fun userLogin(){
        val username_view = findViewById<EditText>(R.id.username)
        val password_view = findViewById<EditText>(R.id.password)

        val username_input: String = username_view.text.toString()
        val password_input: String = password_view.text.toString()

        if(username_input == null || password_input == null || username_input == "" || password_input == ""){
            Toast.makeText(this@LoginPageActivity, "사용자 ID와 Password를 바르게 입력하세요", Toast.LENGTH_LONG).show()
            return
        }

        val loginObject= JSONObject()
        loginObject.put("action","login")
        loginObject.put("user_id",username_input)
        loginObject.put("password",password_input)
        loginObject.put("device_token",device_token)
        loginObject.put("version", Constants.app_version)
        //loginObject.put("device_token",token)

        val result = userLoginHelper(loginObject.toString())

        if(result == Constants.LOGIN_SUCCESS){

            Constants.myID = username_input
            Constants.long_sleep = false

            val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor =  sharedPref.edit()
            editor.putString("id_key",username_input)
            editor.putString("pw_key", password_input)
            editor.apply()
            editor.commit()

            //val intent = Intent(this, MainMenuActivity::class.java).apply {
            val intent = Intent(this, MainMenuV2Activity::class.java).apply {
                //putExtra(Constants.MY_ID, username_input)
            }/*.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP/* or FLAG_ACTIVITY_SINGLE_TOP*/)*/
            startActivity(intent)
        } else if(result == Constants.LOGIN_FAIL){
            Toast.makeText(this@LoginPageActivity, "ID / PW가 정확하지 않습니다", Toast.LENGTH_SHORT).show()
        } else if(result == Constants.LOGIN_SERVER_ERROR || result == Constants.LOGIN_NETWORK_ERROR){
            Toast.makeText(this@LoginPageActivity, "잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
        } else if(result == Constants.LOGIN_UPDATE_REQUIRED){
            //Toast.makeText(this@LoginPageActivity, "앱을 업데이트 해야합니다.", Toast.LENGTH_LONG).show()
            val builder = AlertDialog.Builder(this@LoginPageActivity)
            builder.setTitle("앱을 업데이트 해야 합니다")
            builder.setMessage("업데이트 하시겠습니까?")

            builder.setPositiveButton("예") { dialog, which ->

                var intent: Intent

                try {
                    intent = Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=" + Constants.app_package_name))
                } catch(e: Exception){
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Constants.app_package_name))
                }

                startActivity(intent)
            }
            builder.setNegativeButton("아니요") { dialog, which ->
                finishAffinity()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

    }

    // return value: 0 - login fail (unidentified reason)
    //               1 - login success
    //               2 - login fail (ID / PW not matching)
    //               3 - login fail (network error)
    //               4 - login fail (server error)
    private suspend fun userLoginHelper(login_info: String): Int{
        return withContext(Dispatchers.Default){ // withContext - suspends until it completes and returns results
            // withContext might be replaced by withTimeout which suspends only for given time period
            var login_return_val: Int = 0
            val postData: ByteArray = login_info.toByteArray(StandardCharsets.UTF_8)

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
            val context: SSLContext = SSLContext.getInstance("TLSv1.2").apply {
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
                    val long_result_json: String = stringBuilder.toString()

                    val loginResultObject = JSONObject(long_result_json)
                    val login_result: String = loginResultObject.getString("login_result")

                    if (login_result.equals("success")) {
                        login_return_val = Constants.LOGIN_SUCCESS
                    } else if(login_result.equals("update_required")) {
                        login_return_val = Constants.LOGIN_UPDATE_REQUIRED
                    } else {
                        login_return_val = Constants.LOGIN_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    login_return_val = Constants.LOGIN_SERVER_ERROR
                }

                return@withContext login_return_val

            } catch(e: SocketTimeoutException){
/*val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
    Toast.makeText(this@LoginPageActivity, e.toString(), Toast.LENGTH_SHORT).show()
})*/
                return@withContext Constants.LOGIN_NETWORK_ERROR
            } catch(e: Exception){
/*val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
    Toast.makeText(this@LoginPageActivity, e.toString(), Toast.LENGTH_SHORT).show()
})*/

//var err_string: String = ""
//for (element in e.stackTrace) err_string = err_string + "\n" + element
//Log.v("ssl_error", err_string)
//Log.v("ssl_error", e.toString())
                return@withContext Constants.LOGIN_NETWORK_ERROR
            }
        }
    }

    private fun registserUser(){
        val intent = Intent(this, RegisterActivityScroll::class.java).apply {
            //putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
    }

    private fun findIDPW(){
        val intent = Intent(this, FindIDPWActivity::class.java).apply {
            //putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
    }

}

/*********************************************************
Toast.makeText(this@LoginPageActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@LoginPageActivity, String(postData), Toast.LENGTH_SHORT).show()
})

val bundle = intent.extras
if (bundle != null) {
for (key in bundle.keySet()) {
Log.e("login activity extra", key + " : " + if (bundle[key] != null) bundle[key] else "NULL")
}
}

// following is "HTTP" connction
val url = URL("http://192.168.0.200:65001/GSTWebAPI/Req_Service")
val httpClient = url.openConnection() as HttpURLConnection
//httpClient.hostnameVerifier = hostnameVerifier
httpClient.requestMethod = "POST"
httpClient.setReadTimeout(Constants.getHTTPTimeout())
httpClient.setConnectTimeout(Constants.getHTTPTimeout())
httpClient.doOutput = true
httpClient.doInput = true
httpClient.useCaches = false
httpClient.setRequestProperty("Content-Type", "application/json; charset=utf-16")
httpClient.setRequestProperty("Accept", "application/json")
httpClient.connect()
 *********************************************************/














