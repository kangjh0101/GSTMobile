package com.gst.matchfinder.ui.wanted

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.db.MessageEntry
import com.gst.matchfinder.ui.main.WantedAd
import com.gst.matchfinder.ui.message.Message
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

class WantedDetailActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    lateinit var post_id: String
    var is_user_blocked: Int = 0
    //private lateinit var myID: String
    private lateinit var recID: String
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wanted_detail)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        post_id = intent.getStringExtra(Constants.POST_ID).toString()
        //myID = intent.getStringExtra(Constants.MY_ID).toString()

        val user_id_text = findViewById<TextView>(R.id.wanted_detail_uid)
        val ntrp_text = findViewById<TextView>(R.id.wanted_detail_ntrp)
        val howlong_text = findViewById<TextView>(R.id.wanted_detail_howlong)
        val gender_text = findViewById<TextView>(R.id.wanted_detail_gender)
        val location_text = findViewById<TextView>(R.id.wanted_detail_location)
        val court_text = findViewById<TextView>(R.id.wanted_detail_court)
        val comment_text = findViewById<TextView>(R.id.wanted_detail_comment)
        val send_msg_btn = findViewById<Button>(R.id.msg_btn)
        val unblock_user = findViewById<Button>(R.id.unblock_btn)

        for (each in Constants.wantedAdStruc) {
            val ad_id: String = each.idx

            if (ad_id.equals(post_id)) {

                user_id_text.setText(each.user_id)
                recID = each.user_id
                ntrp_text.setText(each.op_ntrp)
                howlong_text.setText(each.op_howlong)
                gender_text.setText(each.op_gender)
                location_text.setText(each.location)
                court_text.setText(each.court_booked)
                comment_text.setText(each.comment)

                break
            }
        }

        is_user_blocked = Constants.CHECK_USER_BLOCK_WAITING
        GlobalScope.launch {
            check_block_user(recID)
        }
        while(is_user_blocked == Constants.CHECK_USER_BLOCK_WAITING){
            Thread.sleep(100)
        }
        if(is_user_blocked == 1){
            //unblock_user.isVisible = true
            unblock_user.isEnabled = true
        } else{
            unblock_user.isEnabled = false
        }

        if(Constants.myID.equals(user_id_text.text)){
            send_msg_btn.isEnabled = false
        } else{
            send_msg_btn.setOnClickListener {
                val intent = Intent(this, ViewMessageActivity::class.java).apply {
                    putExtra(Constants.RECEIVER_ID, recID)
                    putExtra(Constants.MY_ID, Constants.myID)
                }

                startActivity(intent)
            }

            launch {
                //get_my_message_list()
                get_my_message_list_from_db()
            }
        }

        if(unblock_user.isEnabled == true){
            unblock_user.setOnClickListener {
                launch {
                    unblock_user_chat(Constants.myID, recID)
                }
            }
        }

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.WantedPageAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@WantedDetailActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/
    }

    private suspend fun get_my_message_list_from_db(){
        val msg_list: List<MessageEntry>? = Constants.msg_db?.messageDAO()?.getAllMessages()
        val receiver_list: List<String>? = Constants.msg_db?.messageDAO()?.selectReceiverIDs()

        Constants.myMessageList.clear()
        Constants.myMessageStruc.clear()

        var msg_index: Int = 0
        if(receiver_list != null && msg_list != null) {
            for (each_receiver in receiver_list) {
                var new_msg: Boolean = false

                var tmp_array_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage("",0, null, null, null))
                tmp_array_list.clear()

                val temp_struc: Message
                var tmp_last_msg_id: Long = 0

                for(each_msg in msg_list) {
                    val msg_time = each_msg.msg_time
                    val message_id = each_msg.message_id
                    val receiver_id = each_msg.receiver_id
                    val direction = each_msg.direction
                    val message_text = each_msg.message_text
                    val message_read = each_msg.message_read

                    if(receiver_id.equals(each_receiver)){
                        var tmp_msg_list: Message.IndividualMessage = Message.IndividualMessage(msg_time, message_id, direction, message_text, message_read)
                        tmp_array_list.add(tmp_msg_list)

                        if(message_id > tmp_last_msg_id) tmp_last_msg_id = message_id
                    }

                    if(message_read.equals("n")){
                        new_msg = true
                    }
                }

                val row_to_insert: String
                if(new_msg) row_to_insert = each_receiver + "와 주고 받은 메세지 (새 메세지)"
                else row_to_insert = each_receiver + "와 주고 받은 메세지"
                Constants.myMessageList.add(row_to_insert)

                temp_struc = Message(each_receiver, tmp_last_msg_id, tmp_array_list)
                Constants.myMessageStruc.add(temp_struc)

                msg_index++
            }

            sort_message_list()
// *** following is an example of how to access actual messages *** //
/*for(i in 0 until msg_index){
    var tmp_str: String = Constants.myMessageList[i] + "  /  " + Constants.myMessageStruc[i].last_msg_id
    val gara_msg_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage(0, "s", Constants.myMessageStruc[i].receiver_id + "에게 메세지 보내기", "n"))
    var message: Message = Message("", 0, gara_msg_list)

    for (j in 0 until (Constants.myMessageStruc[i].messages?.size)!!) {

        tmp_str = tmp_str + " / " + (Constants.myMessageStruc[i].messages!![j].message.toString())
    }
    Log.e("chatting", tmp_str)
}*/
        }

    }

    private suspend fun unblock_user_chat(my_id: String, receiver_id: String){
        val msgObject= JSONObject()
        msgObject.put("action","unblock_user")
        msgObject.put("user_id", my_id)
        msgObject.put("receiver_id",receiver_id)

        val result: Int = unblock_user_chat_helper(msgObject.toString())

        if(result == Constants.UNBLOCK_USER_SUCCESS){

            val unblock_user = findViewById<Button>(R.id.unblock_btn)
            unblock_user.isEnabled = false

            return

        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(Runnable {
                Toast.makeText(this@WantedDetailActivity, "차단을 해제하지 못했습니다.\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            })

            return
        }
    }

    private suspend fun unblock_user_chat_helper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var exit_return_val: Int = 0
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
                    val exit_result_json: String = stringBuilder.toString()

                    val exitResultObject = JSONObject(exit_result_json)
                    val exit_result: String = exitResultObject.getString("unblock_user_result")

                    if (exit_result.equals("success")) {
                        exit_return_val = Constants.UNBLOCK_USER_SUCCESS
                    } else {
                        exit_return_val = Constants.UNBLOCK_USER_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    exit_return_val = Constants.UNBLOCK_USER_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext exit_return_val

            } catch(e: SocketTimeoutException){
                //e.printStackTrace()
                return@withContext Constants.UNBLOCK_USER_NETWORK_ERROR
            } catch(e: Exception){
                //e.printStackTrace()
                return@withContext Constants.UNBLOCK_USER_NETWORK_ERROR
            }
        }
    }

    private suspend fun check_block_user(receiver_id: String){
        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","check_block_user")
        searchJSONObject.put("user_id", Constants.myID)
        searchJSONObject.put("receiver_id", receiver_id)

        val result: Int = check_block_user_Helper(searchJSONObject.toString())

        if(result == Constants.CHECK_USER_BLOCK_BLOCKED){
            is_user_blocked = 1
        } else if(result == Constants.CHECK_USER_BLOCK_UNBLOCKED){
            is_user_blocked = 2
        } else{
            is_user_blocked = 2
        }
    }

    private suspend fun check_block_user_Helper(msg_info: String): Int{
        return withContext(Dispatchers.Default) {
            var is_user_blocked: Int = Constants.CHECK_USER_BLOCK_FAIL
            val postData: ByteArray = msg_info.toByteArray(StandardCharsets.UTF_8)

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
                    val check_block_result_json: String = stringBuilder.toString()
                    val check_blockResultObject = JSONObject(check_block_result_json)
                    val check_block_result: String = check_blockResultObject.getString("check_block_user_result")

                    if (check_block_result.equals("success")) {
                        val check_block_blocked: String = check_blockResultObject.getString("block")

                        if(check_block_blocked.equals("yes")){
                            is_user_blocked = Constants.CHECK_USER_BLOCK_BLOCKED
                        } else{
                            is_user_blocked = Constants.CHECK_USER_BLOCK_UNBLOCKED
                        }

                    } else {
                        is_user_blocked = Constants.CHECK_USER_BLOCK_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    is_user_blocked = Constants.WANTED_LIST_SERVER_ERROR
                }

                return@withContext is_user_blocked

            } catch(e: SocketTimeoutException){
                //e.printStackTrace()
                return@withContext Constants.CHECK_USER_BLOCK_NETWORK_ERROR
            } catch(e: Exception){
                //e.printStackTrace()
                return@withContext Constants.CHECK_USER_BLOCK_NETWORK_ERROR
            }
        }
    }

    /*private suspend fun get_my_message_list(){

        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","get_my_msg")
        searchJSONObject.put("user_id", Constants.myID)

        val result: Int = get_my_message_list_Helper(searchJSONObject.toString())
    }

    private suspend fun get_my_message_list_Helper(msg_info: String): Int{
        return withContext(Dispatchers.Default){ // withContext - suspends until it completes and returns results
            // withContext might be replaced by withTimeout which suspends only for given time period
            var get_list_return_val: Int = Constants.WANTED_LIST_FAIL
            val postData: ByteArray = msg_info.toByteArray(StandardCharsets.UTF_8)

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
                httpClient.setHostnameVerifier(HostnameVerifier { hostname, session -> true }) // this is to make all host name trusted
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
                    val message_result_json: String = stringBuilder.toString()

                    val messageResultObject = JSONObject(message_result_json)

                    val message_list_result: String = messageResultObject.getString("message_result")

                    if (message_list_result.equals("success")) {
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS

                        val message_result_list: JSONArray = messageResultObject.getJSONArray("data")
                        val message_result = process_message_json_array(message_result_list)

                        if(message_result == Constants.WANTED_LIST_SUCCESS){
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
                //e.printStackTrace()
                return@withContext Constants.WANTED_LIST_NETWORK_ERROR
            } catch(e: Exception){
                //e.printStackTrace()
                return@withContext Constants.WANTED_LIST_NETWORK_ERROR
            }
        }
    }

    private fun process_message_json_array(message_result: JSONArray): Int{

        var return_value = Constants.WANTED_LIST_FAIL

        Constants.myMessageList.clear()
        Constants.myMessageStruc.clear()

        for (i in 0 until message_result.length()) {

            val item = message_result.getJSONObject(i)

            val receiver_id = item.getString("receiver_id")
            val temp_struc: Message
            var tmp_array_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage(0,null, null, null))
            tmp_array_list.clear()
            val row_to_insert: String = receiver_id + "와 주고 받은 메세지"
            var tmp_last_msg_id: Long = 0

            val msg_array: JSONArray = item.getJSONArray("messages")
            for(j in 0 until msg_array.length()){
                val msg_item_str: String = msg_array.get(j).toString()
                val msg_item: JSONObject = JSONObject(msg_item_str)

                val direction: String = msg_item.get("direction").toString()
                val msg: String = msg_item.get("msg").toString()
                val msg_id: Long = msg_item.getString("msg_id").toLong()
                var tmp_msg_list: Message.IndividualMessage = Message.IndividualMessage(msg_id, direction, msg, "y")
                tmp_array_list.add(tmp_msg_list)
                if(msg_id > tmp_last_msg_id) tmp_last_msg_id = msg_id
            }

            temp_struc = Message(receiver_id, tmp_last_msg_id, tmp_array_list)
            Constants.myMessageList.add(row_to_insert)
            Constants.myMessageStruc.add(temp_struc)

            return_value = Constants.WANTED_LIST_SUCCESS
        }

        sort_message_list()

        return return_value
    }*/

    private fun sort_message_list(){
        for(i in 0 until (Constants.myMessageStruc.size - 1)){
            for(j in 0 until ((Constants.myMessageStruc.size - 1) - i)) {
                if(Constants.myMessageStruc[j].last_msg_id < Constants.myMessageStruc[j + 1].last_msg_id){
                    var tmp_struct: Message = Constants.myMessageStruc[j]
                    var tmp_msg_str: String = Constants.myMessageList[j]
                    Constants.myMessageStruc[j] = Constants.myMessageStruc[j + 1]
                    Constants.myMessageList[j] = Constants.myMessageList[j + 1]
                    Constants.myMessageStruc[j + 1] = tmp_struct
                    Constants.myMessageList[j + 1] = tmp_msg_str
                }
            }
        }
    }
}

/***********************************************************************
Toast.makeText(this@WantedDetailActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@WantedDetailActivity, String(postData), Toast.LENGTH_SHORT).show()
})

 ***********************************************************************/














