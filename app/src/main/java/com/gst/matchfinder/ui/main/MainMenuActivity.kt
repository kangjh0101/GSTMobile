package com.gst.matchfinder.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.db.MessageDB
import com.gst.matchfinder.db.MessageEntry
import com.gst.matchfinder.ui.bug_report.BugReportActivity
import com.gst.matchfinder.ui.message.Message
import com.gst.matchfinder.ui.message.MessageBoxActivity
import com.gst.matchfinder.ui.wanted.MyWantedAdListActivity
import com.gst.matchfinder.ui.wanted.WantedAdActivity
import com.gst.matchfinder.util.GSTLifeCycleListener
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
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MainMenuActivity : AppCompatActivity(), CoroutineScope by MainScope(), LifecycleObserver {

    private var doubleBackToExitPressedOnce = false
    private lateinit var mAdView: AdView

    /*private val mainMenuReceiver = object: BroadcastReceiver() { // this receives broadcast message from GSTFirebaseMessagingSerivce for message notifiction
        override fun onReceive(context: Context?, intent: Intent?) {

            val msg_str: String? = intent?.getStringExtra("msg_str")
            val msg_id: String? = intent?.getStringExtra("msg_id")
            val msg_sender: String? = intent?.getStringExtra("msg_sender")
//var data:List<MessageEntry>? = Constants.msg_db?.messageDAO()?.getAllMessages()
//val handler = Handler(Looper.getMainLooper())
//handler.post(Runnable {
//Toast.makeText(this@MainMenuActivity, msg_str + " / " + msg_id + " / " + msg_sender, Toast.LENGTH_LONG).show()
//})
            //if(msg_str != null && msg_id != null && msg_sender != null) {
            //    val tmp_msg_id: List<Long>? = Constants.msg_db?.messageDAO()?.checkMessagesID(msg_id.toLong())
            //    if(tmp_msg_id.isNullOrEmpty()){
            //        val msgEntry: MessageEntry = MessageEntry(/*msg_time, */msg_id.toLong(), msg_sender, "r", msg_str, "n")
            //        Constants.msg_db?.messageDAO()?.insertAll(msgEntry)

                    launch {
                        get_my_message_list_from_db()
                    }
            //    }
            //}
        }
    }*/

    override fun onDestroy() {
        super.onDestroy()

        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mainMenuReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        this.getSupportActionBar()?.hide()

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }
//Log.e("onCreate", "created")
        //LocalBroadcastManager.getInstance(this).registerReceiver(mainMenuReceiver, IntentFilter("message_received_toss_to_messageboxactivity"))

        ProcessLifecycleOwner.get().lifecycle.addObserver(GSTLifeCycleListener(this))

        if(Constants.msg_db == null || !Constants.msg_db!!.isOpen) Constants.msg_db = MessageDB.getInstance(this)

        /*val wanted_ad_page = findViewById<Button>(R.id.wanted_ad_btn)
        val post_wanted_page = findViewById<Button>(R.id.post_ad_btn)
        val my_wanted_list_page = findViewById<Button>(R.id.my_ad_btn)
        val check_message_page = findViewById<Button>(R.id.msg_btn)
        val bug_report_page = findViewById<Button>(R.id.report_btn)
        val withdraw_page = findViewById<Button>(R.id.drop_btn)*/

        val wanted_ad_page = findViewById<ImageButton>(R.id.wanted_ad_btn)
        val post_wanted_page = findViewById<ImageButton>(R.id.post_ad_btn)
        val my_wanted_list_page = findViewById<ImageButton>(R.id.my_ad_btn)
        val check_message_page = findViewById<ImageButton>(R.id.msg_btn)
        val new_message_page = findViewById<ImageButton>(R.id.new_msg_btn)
        val bug_report_page = findViewById<ImageButton>(R.id.report_btn)
        val withdraw_page = findViewById<ImageButton>(R.id.drop_btn)

        wanted_ad_page.setOnClickListener{
            val intent = Intent(this, MainPageActivity::class.java).apply {
                //putExtra(Constants.MY_ID, username_input)
            }
            startActivity(intent)
        }

        post_wanted_page.setOnClickListener {
            postWantedAd()
        }

        my_wanted_list_page.setOnClickListener{
            val refresh_job = launch {
                get_my_wanted_list()
            }
        }

        check_message_page.setOnClickListener{
            val check_msg_job = launch {
                check_messages()
            }
        }

        new_message_page.setOnClickListener{
            val check_msg_job = launch {
                check_messages()
            }
        }

        bug_report_page.setOnClickListener{
            val send_bug_job = launch {
                send_bug_report()
            }
        }

        withdraw_page.setOnClickListener {
            val block_user_job = launch {
                val builder = AlertDialog.Builder(this@MainMenuActivity)
                builder.setTitle("회원 탈퇴")
                builder.setMessage("진짜로 탈퇴하시겠습니까?")

                builder.setPositiveButton("예") { dialog, which ->
                    // GlobalScope.launch must be used because inner launch is executed within the job of your parent launch.
                    // As the parent finishes once the dialog is created, all children are cancelled.
                    val register_job = GlobalScope.launch {
                        withdraw_user(Constants.myID)
                    }
                }
                builder.setNegativeButton("아니요") { dialog, which -> }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.MainMenuPageAdView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@MainMenuActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/

        /*val check_message_page = findViewById<Button>(R.id.msg_btn)
        check_message_page.text = "메세지 확인하기"
        check_message_page.setTextColor(Color.parseColor("#FF000000"))*/

        val check_message_page = findViewById<ImageButton>(R.id.msg_btn)
        val new_message_page = findViewById<ImageButton>(R.id.new_msg_btn)
        check_message_page.isEnabled = true
        check_message_page.isVisible = true
        new_message_page.isEnabled = false
        new_message_page.isVisible = false

        launch {
            //get_my_message_list()
            get_my_message_list_from_db()
        }
    }

    override fun onBackPressed() {
        if(doubleBackToExitPressedOnce) {
            super.onBackPressed()
            ActivityCompat.finishAffinity(this)
        }

        this.doubleBackToExitPressedOnce = true

        Toast.makeText(this, "뒤로가기를 한 번 더 누르면 앱이 종료됩니다", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed(kotlinx.coroutines.Runnable {
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

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

    private suspend fun withdraw_user(my_id: String){
        val msgObject= JSONObject()
        msgObject.put("action","withdraw_user")
        msgObject.put("user_id", my_id)

        val result: Int = withdraw_user_helper(msgObject.toString())

        if(result == Constants.WITHDRAW_USER_SUCCESS){
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this@MainMenuActivity, "탈퇴되었습니다.", Toast.LENGTH_LONG).show()
            })

            finishAffinity()

        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this@MainMenuActivity, "탈퇴하지 못했습니다.\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            })

            return
        }
    }

    private suspend fun withdraw_user_helper(id_info: String): Int {
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
                    val exit_result: String = exitResultObject.getString("withdraw_user_result")

                    if (exit_result.equals("success")) {
                        exit_return_val = Constants.WITHDRAW_USER_SUCCESS
                    } else {
                        exit_return_val = Constants.WITHDRAW_USER_FAIL
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    exit_return_val = Constants.WITHDRAW_USER_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext exit_return_val

            } catch(e: SocketTimeoutException){
                //e.printStackTrace()
                return@withContext Constants.WITHDRAW_USER_NETWORK_ERROR
            } catch(e: Exception){
                //e.printStackTrace()
                return@withContext Constants.WITHDRAW_USER_NETWORK_ERROR
            }
        }
    }

    private fun send_bug_report(){
        val intent = Intent(this, BugReportActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }

    private fun postWantedAd(){
        val intent = Intent(this, WantedAdActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }

    private fun get_my_wanted_list(){
        val intent = Intent(this, MyWantedAdListActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
    }

    private fun check_messages(){

        val intent = Intent(this, MessageBoxActivity::class.java).apply {
            //putExtra(Constants.MY_ID, myID)
        }
        startActivity(intent)
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

                //val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
                //val parsedDate: Date = dateFormat.parse("1970-01-01 00:00:00.000") as Date
                //val tmp_ts: Timestamp =  Timestamp(parsedDate.getTime())
                var tmp_array_list: ArrayList<Message.IndividualMessage> = arrayListOf(
                    Message.IndividualMessage("",0, null, null, null))
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
                        /*val check_message_page = findViewById<Button>(R.id.msg_btn)
                        check_message_page.text = "메세지 확인하기 (new message)"
                        check_message_page.setTextColor(Color.parseColor("#FFFF0000"))*/

// 왜 이게 있으면, 버튼이 사라지는지 모르겠음                       val check_message_page = findViewById<ImageButton>(R.id.msg_btn)
                        val new_message_page = findViewById<ImageButton>(R.id.new_msg_btn)
// 왜 이게 있으면, 버튼이 사라지는지 모르겠음                         check_message_page.isEnabled = false
// 왜 이게 있으면, 버튼이 사라지는지 모르겠음                         check_message_page.isVisible = false
                        new_message_page.isEnabled = true
                        new_message_page.isVisible = true

                        new_msg = true
                    }
                }

                val row_to_insert: String
                if(new_msg) row_to_insert = each_receiver + "와 주고 받은 메세지 (새 메세지)"
                else row_to_insert = each_receiver + "와 주고 받은 메세지"
                Constants.myMessageList.add(row_to_insert)

                temp_struc = Message(
                    each_receiver,
                    tmp_last_msg_id,
                    tmp_array_list
                )
                Constants.myMessageStruc.add(temp_struc)

                msg_index++
            }

            sort_message_list()
// ******* following is an example of how to access actual messages ******* //
/*for(i in 0 until msg_index){
    var tmp_str: String = Constants.myMessageList[i] + "  /  " + Constants.myMessageStruc[i].last_msg_id
    //val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
    //val parsedDate: Date = dateFormat.parse("1970-01-01 00:00:00.000") as Date
    //val tmp_ts: Timestamp =  Timestamp(parsedDate.getTime())
    val gara_msg_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage(/*tmp_ts, */0, "s", Constants.myMessageStruc[i].receiver_id + "에게 메세지 보내기", "n"))
    var message: Message = Message("", 0, gara_msg_list)

    for (j in 0 until (Constants.myMessageStruc[i].messages?.size)!!) {

        tmp_str = tmp_str + " / " + (Constants.myMessageStruc[i].messages!![j].message.toString() + " : " + Constants.myMessageStruc[i].messages!![j].read)
    }
    Log.e("chatting", tmp_str)
}*/
// ************************************************************************ //
        }

    }
}

/*********************************************************
Toast.makeText(this@MainMenuActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@MainMenuActivity, String(postData), Toast.LENGTH_SHORT).show()
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
