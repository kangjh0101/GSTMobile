package com.gst.matchfinder.ui.message

import android.app.NotificationManager
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.data.Constants.Companion.myMessageStruc
import com.gst.matchfinder.db.MessageEntry
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
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.collections.ArrayList

class ViewMessageActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var recID: String
    private lateinit var myID: String

    lateinit var chat_adapter_list: ArrayAdapter<String>
    private lateinit var chat_listView: ListView

    var is_user_blocked: Int = 0

    private val viewMessageReceiver = object: BroadcastReceiver() { // this receives broadcast message from GSTFirebaseMessagingSerivce for message notifiction
        override fun onReceive(context: Context?, intent: Intent?) {

            var c: Calendar = Calendar.getInstance()
            var date: String = (c.get(Calendar.MONTH) + 1).toString() + "/" + c.get(Calendar.DATE).toString()
            var time: String = c.get(Calendar.HOUR_OF_DAY).toString() + ":" + c.get(Calendar.MINUTE).toString()
            var date_time: String = date + " " + time

            val msg_str: String? = intent?.getStringExtra("msg_str")
            val msg_id: String? = intent?.getStringExtra("msg_id")
            val msg_sender: String? = intent?.getStringExtra("msg_sender")

            if(msg_str != null && msg_id != null && msg_sender != null) {
//Log.e("received_msg_5", msg_str + " / " + msg_id)
                if (recID.equals(msg_sender) && msg_str != null && msg_id != null) {
                    add_received_chat(date_time, msg_str, chat_adapter_list, chat_listView, msg_id.toLong())
                    refresh_my_message_list(chat_adapter_list, chat_listView)
                    Constants.msg_db?.messageDAO()?.updateReadBySender(recID)
                }
            }

            //val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //notificationManager.cancelAll()
        }
    }

    /*override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//Log.e("key_down", "is this even called?")
        if(keyCode == KeyEvent.KEYCODE_BACK){
//Log.e("key_down", "is this keycode_back?")
            finishActivity()
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun finishActivity(){
        this.finishActivity(0)
        return
    }*/

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(viewMessageReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_message)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(viewMessageReceiver, IntentFilter("message_received_toss_to_viewmessageactivity"))

        recID = intent.getStringExtra(Constants.RECEIVER_ID).toString()
        myID = intent.getStringExtra(Constants.MY_ID).toString()

        this.getSupportActionBar()?.title = recID + "와의 대화"

        chat_adapter_list = ArrayAdapter(this, R.layout.my_chatting_list_items, Constants.myChatting)

        chat_listView = findViewById<ListView>(R.id.viewMessageListView)
        chat_listView.setAdapter(chat_adapter_list)

        refresh_my_message_list(chat_adapter_list, chat_listView)

        val send_msg = findViewById<ImageButton>(R.id.send_message_btn)
        val exit_msg = findViewById<ImageButton>(R.id.exit_message_btn)
        val block_user = findViewById<Button>(R.id.block_user_btn)
        //val unblock_user = findViewById<ImageButton>(R.id.unblock_user_btn)

        send_msg.setOnClickListener {
            val register_job = launch {
                val msg_txt: String = findViewById<EditText>(R.id.send_message_input).text.toString()
                if(msg_txt.isNotEmpty()) send_messsage(msg_txt, chat_adapter_list, chat_listView)
            }
        }

        is_user_blocked = Constants.CHECK_USER_BLOCK_WAITING
        GlobalScope.launch {
            check_block_user(recID)
        }
        while(is_user_blocked == Constants.CHECK_USER_BLOCK_WAITING){
            Thread.sleep(100)
        }
        if(is_user_blocked == Constants.CHECK_USER_BLOCK_BLOCKED){
            block_user.text = " 메세지\n차단 해제"
        } else{
            block_user.text = "메세지 차단"
        }

        block_user.setOnClickListener {
            if(is_user_blocked == Constants.CHECK_USER_BLOCK_UNBLOCKED) {
                val block_user_job = launch {

                    val builder = AlertDialog.Builder(this@ViewMessageActivity)
                    builder.setTitle("사용자 차단")
                    builder.setMessage("이 사용자에게 더이상 메세지를 받지 않으시겠습니까?")

                    builder.setPositiveButton("예") { dialog, which ->
                        // GlobalScope.launch must be used because inner launch is executed within the job of your parent launch.
                        // As the parent finishes once the dialog is created, all children are cancelled.
                        val register_job = GlobalScope.launch {
                            block_user_chat(myID, recID)
                            finish()
                        }

                        //block_user.isEnabled = false
                        block_user.text = " 메세지\n차단 해제"
                    }
                    builder.setNegativeButton("아니요") { dialog, which -> }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            } else{
                val unblock_user_job = launch {

                    val builder = AlertDialog.Builder(this@ViewMessageActivity)
                    builder.setTitle("사용자 차단 해제")
                    builder.setMessage("이 사용자에게 메세지를 받으시겠습니까?")

                    builder.setPositiveButton("예") { dialog, which ->
                        // GlobalScope.launch must be used because inner launch is executed within the job of your parent launch.
                        // As the parent finishes once the dialog is created, all children are cancelled.
                        val register_job = GlobalScope.launch {
                            unblock_user_chat(myID, recID)
                        }

                        //block_user.isEnabled = true
                        block_user.text = "메세지 차단"
                    }
                    builder.setNegativeButton("아니요") { dialog, which -> }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }

        exit_msg.setOnClickListener{
            val exit_msg_job = launch {

                val builder = AlertDialog.Builder(this@ViewMessageActivity)
                builder.setTitle("채팅방에서 나가기")
                builder.setMessage("이 채팅방에서 나가시겠습니까?\n채틷방에서 나가면 이전 메세지를 더 이상 볼 수 없습니다.")

                builder.setPositiveButton("예"){dialog, which ->
                    // GlobalScope.launch must be used because inner launch is executed within the job of your parent launch.
                    // As the parent finishes once the dialog is created, all children are cancelled.
                    val register_job = GlobalScope.launch {
                        //exit_message(myID, recID)
                        exit_message_db(recID)
                        finish()
                    }
                }
                builder.setNegativeButton("아니요"){dialog, which ->}

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }

        val chatting_result: Int = process_chatting_list()
        if(chatting_result == Constants.CHATTING_LIST_SUCCESS) refresh_my_message_list(chat_adapter_list, chat_listView)
        else Toast.makeText(this@ViewMessageActivity, "내 메세지를 가져오지 못했습니다", Toast.LENGTH_LONG).show()

        //val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //notificationManager.cancelAll()
    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@ViewMessageActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/
    }

    private suspend fun exit_message_db(receiver_id: String){
        Constants.msg_db?.messageDAO()?.deleteMessageBySender(receiver_id)
    }

    private fun process_chatting_list(): Int{
        var return_value = Constants.CHATTING_LIST_FAIL
        Constants.myChatting.clear()

        //val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
        //val parsedDate: Date = dateFormat.parse("1970-01-01 00:00:00.000") as Date
        //val tmp_ts: Timestamp =  Timestamp(parsedDate.getTime())
        val gara_msg_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage("", 0, "s", recID + "에게 메세지 보내기", "y"))
        var message: Message = Message("", 0, gara_msg_list)

        for(i in 0 until myMessageStruc.size){
            val tmp_msg = myMessageStruc[i]

            if(tmp_msg.receiver_id.equals(recID)){
                message = tmp_msg

                Constants.msg_db?.messageDAO()?.updateReadBySender(recID)

                break
            }
        }

        if(message != null && message.messages != null) {
            for (i in 0 until message.messages!!.size) {
                if (message.messages!![i].direction == null || message.messages!![i].direction == "") {

                } else if (message.messages!![i].direction.equals("r")) {
                    Constants.myChatting.add("<< 받은 @ " + message.messages!![i].msg_time + " <<\n" + message.messages!![i].message.toString())
                } else if (message.messages!![i].direction.equals("s")) {
                    Constants.myChatting.add(">> 보낸 @ " + message.messages!![i].msg_time + " >>\n" + message.messages!![i].message.toString())
                } else {
                    Constants.myChatting.add(message.messages!![i].message.toString())
                }
            }
        }

        return_value = Constants.CHATTING_LIST_SUCCESS
        return return_value
    }

    private suspend fun send_messsage(msg: String, adapter: ArrayAdapter<String>, listView: ListView){

        var c: Calendar = Calendar.getInstance()
        var date: String = (c.get(Calendar.MONTH) + 1).toString() + "/" + c.get(Calendar.DATE).toString()
        var time: String = c.get(Calendar.HOUR_OF_DAY).toString() + ":" + c.get(Calendar.MINUTE).toString()
        var date_time: String = date + " " + time

        val send_msg = findViewById<ImageButton>(R.id.send_message_btn)
        send_msg.isEnabled = false

        val msgObject= JSONObject()
        msgObject.put("action","send_msg")
        msgObject.put("user_id", myID)

        val msgDataObject= JSONObject()
        msgDataObject.put("receiver_id",recID)
        msgDataObject.put("msg", msg)

        msgObject.put("data", msgDataObject)

        val result: Long = send_message_helper(msgObject.toString(), recID, adapter, listView)

        if(result > 0){
            val msgEntry: MessageEntry = MessageEntry(result, date_time, recID, "s", msg, "y")
            Constants.msg_db?.messageDAO()?.insertAll(msgEntry)

            add_sent_chat(date_time, msg, adapter, listView, result)

            findViewById<EditText>(R.id.send_message_input).setText("")
            send_msg.isEnabled = true
            return
        } else if(result == Constants.SEND_MESSAGE_BLOCKED){
            Toast.makeText(this@ViewMessageActivity, "메세지를 보낼 수 없는 사용자입니다.", Toast.LENGTH_LONG).show()
            send_msg.isEnabled = true
            return
        } else{

            Toast.makeText(this@ViewMessageActivity, "메세지를 보내지 못했습니다\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            send_msg.isEnabled = true
            return
        }

    }

    private suspend fun send_message_helper(id_info: String, recID: String, adapter: ArrayAdapter<String>, listView: ListView): Long {
        return withContext(Dispatchers.Default) {

            var post_wanted_return_val: Long = 0
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
                    val register_result: String = registerResultObject.getString("send_result")
                    val message_id: Long = registerResultObject.getString("msg_id").toLong()
                    //val msg_time_str: String = registerResultObject.getString("msg_time")

                    //val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
                    //val parsedDate: Date = dateFormat.parse(msg_time_str) as Date
                    //val tmp_ts: Timestamp =  Timestamp(parsedDate.getTime())

                    if (register_result.equals("success")) {
                        post_wanted_return_val = message_id

                    } else if(register_result.equals("blocked")){
//val handler = Handler(Looper.getMainLooper())
//handler.post(Runnable {
//    Toast.makeText(this@ViewMessageActivity, "blocked " + register_result, Toast.LENGTH_LONG).show()
//})
                        post_wanted_return_val = Constants.SEND_MESSAGE_BLOCKED
                    } else {
//val handler = Handler(Looper.getMainLooper())
//handler.post(Runnable {
//    Toast.makeText(this@ViewMessageActivity, "failed " + register_result, Toast.LENGTH_LONG).show()
//})
                        post_wanted_return_val = Constants.SEND_MESSAGE_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    post_wanted_return_val = Constants.SEND_MESSAGE_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext post_wanted_return_val

            } catch(e: SocketTimeoutException){
                e.printStackTrace()
                return@withContext Constants.SEND_MESSAGE_NETWORK_ERROR
            } catch(e: Exception){
                e.printStackTrace()
                return@withContext Constants.SEND_MESSAGE_NETWORK_ERROR
            }
        }
    }

    private fun add_sent_chat(tmp_ts: String, new_msg: String, adapter: ArrayAdapter<String>, listView: ListView, msg_id: Long){
        Constants.myChatting.add(">> 보낸 @ " + tmp_ts + " >>\n" + new_msg)
        refresh_my_message_list(adapter, listView)

        for(i in 0 until myMessageStruc.size){
            if(myMessageStruc[i].receiver_id.equals(recID)){
                myMessageStruc[i].messages!!.add(Message.IndividualMessage(tmp_ts, msg_id, "s", new_msg, "y"))
                break
            }
        }
    }

    private fun add_received_chat(msg_time: String, new_msg: String, adapter: ArrayAdapter<String>, listView: ListView, msg_id: Long){
        Constants.myChatting.add("<< 받은 @ " + msg_time + " <<\n" + new_msg)
        refresh_my_message_list(adapter, listView)
//var msglist: String = ""
        for(i in 0 until myMessageStruc.size){
            if(myMessageStruc[i].receiver_id.equals(recID)){
//Log.e("add chat", "how many times added?")
                myMessageStruc[i].messages!!.add(Message.IndividualMessage(msg_time, msg_id, "r", new_msg, "y"))
//                break
            }
//for(j in 0 until myMessageStruc[i].messages!!.size) {
//    msglist = msglist + " / " + myMessageStruc[i]!!.messages?.get(j)!!.msg_id+ "-" + myMessageStruc[i]!!.messages?.get(j)!!.message
//}
        }
//Log.e("add_chat", msglist)
    }

    private fun refresh_my_message_list(adapter: ArrayAdapter<String>, listView: ListView){
        //public fun refresh_my_message_list(adapter: ArrayAdapter<String>, listView: ListView) {

        adapter.notifyDataSetChanged()
        listView.setSelection(adapter.count - 1)
    }

    private suspend fun unblock_user_chat(my_id: String, receiver_id: String){
        val msgObject= JSONObject()
        msgObject.put("action","unblock_user")
        msgObject.put("user_id", my_id)
        msgObject.put("receiver_id",receiver_id)

        val result: Int = unblock_user_chat_helper(msgObject.toString())

        if(result == Constants.UNBLOCK_USER_SUCCESS){

            is_user_blocked = Constants.CHECK_USER_BLOCK_UNBLOCKED
            return

        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(Runnable {
                Toast.makeText(this@ViewMessageActivity, "차단을 해제하지 못했습니다.\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
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
            is_user_blocked = Constants.CHECK_USER_BLOCK_BLOCKED
        } else if(result == Constants.CHECK_USER_BLOCK_UNBLOCKED){
            is_user_blocked = Constants.CHECK_USER_BLOCK_UNBLOCKED
        } else{
            is_user_blocked = Constants.CHECK_USER_BLOCK_UNBLOCKED
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

    private suspend fun block_user_chat(my_id: String, receiver_id: String){
        val msgObject= JSONObject()
        msgObject.put("action","block_user")
        msgObject.put("user_id", my_id)
        msgObject.put("receiver_id",receiver_id)

        val result: Int = block_user_chat_helper(msgObject.toString())

        if(result == Constants.BLOCK_USER_SUCCESS){
            //return
            is_user_blocked = Constants.CHECK_USER_BLOCK_BLOCKED
            finish()
        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(Runnable {
                Toast.makeText(this@ViewMessageActivity, "사용자를 차단하지 못했습니다.\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            })
            return
        }
    }

    private suspend fun block_user_chat_helper(id_info: String): Int {
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
                    val exit_result: String = exitResultObject.getString("block_user_result")

                    if (exit_result.equals("success")) {
                        exit_return_val = Constants.BLOCK_USER_SUCCESS
                    } else {
                        exit_return_val = Constants.BLOCK_USER_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    exit_return_val = Constants.BLOCK_USER_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext exit_return_val

            } catch(e: SocketTimeoutException){
                //e.printStackTrace()
                return@withContext Constants.BLOCK_USER_NETWORK_ERROR
            } catch(e: Exception){
                //e.printStackTrace()
                return@withContext Constants.BLOCK_USER_NETWORK_ERROR
            }
        }
    }
}

/************************************************************************************
Toast.makeText(this@ViewMessageActivity, "Selected position: $position", Toast.LENGTH_LONG).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@ViewMessageActivity, String(postData), Toast.LENGTH_LONG).show()
})


 ************************************************************************************/










