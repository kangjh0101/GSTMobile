package com.gst.matchfinder.ui.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import com.gst.matchfinder.data.Constants.Companion.myMessageStruc
import com.gst.matchfinder.db.MessageEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MessageBoxActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()
    //private lateinit var myID: String
    private lateinit var listView: ListView
    private lateinit var adapter_list: ArrayAdapter<String>

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_box)

        this.getSupportActionBar()?.title = "메세지"

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        //myID = intent.getStringExtra(Constants.MY_ID).toString()

        adapter_list = ArrayAdapter(this, R.layout.my_message_items, Constants.myMessageList)

        listView = findViewById<ListView>(R.id.myMessageListView)
        listView.setAdapter(adapter_list)

        listView.setOnItemClickListener { parent, view, position, id ->
            val element = listView.getItemAtPosition(position) // The item that was clicked

            val receiver_id: String = myMessageStruc[position].receiver_id

            if(receiver_id != null && receiver_id != "") {
                val intent = Intent(this, ViewMessageActivity::class.java).apply {
                    putExtra(Constants.RECEIVER_ID, receiver_id)
                    putExtra(Constants.MY_ID, Constants.myID)
                }.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                /*.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)*/
                /*.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK )*/
                /*.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TOP)*/

                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*if(Constants.long_sleep){
            Toast.makeText(this@MessageBoxActivity, "앱을 다시 시작 하십시요", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else{
                finish()
            }
            //System.exit(0)
        }*/

        launch { /*APP SHOULD NOT GET THE MESSAGES FROM SERVER
                   INSTEAD, IT HAS TO GET THE MESSAGES FROM DATABASE*/
            //get_my_message_list()
            get_my_message_list_from_db()
            refresh_my_message_list(adapter_list)
        }
    }

    private suspend fun get_my_message_list_from_db(){
        val whole_msg_list_from_db: List<MessageEntry>? = Constants.msg_db?.messageDAO()?.getAllMessages()
        val receiver_list: List<String>? = Constants.msg_db?.messageDAO()?.selectReceiverIDs()

        Constants.myMessageList.clear()
        Constants.myMessageStruc.clear()

        var msg_index: Int = 0
        if(receiver_list != null && whole_msg_list_from_db != null) {
            for (each_receiver in receiver_list) {
                var new_msg: Boolean = false
//var tmp_str: String = each_receiver
                //val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")
                //val parsedDate: Date = dateFormat.parse("1970-01-01 00:00:00.000") as Date
                //val tmp_ts: Timestamp =  Timestamp(parsedDate.getTime())
                var tmp_msg_list_for_view_message: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage("",0, null, null, null))
                tmp_msg_list_for_view_message.clear()

                val temp_struc: Message
                var tmp_last_msg_id: Long = 0

                for(each_msg in whole_msg_list_from_db) {
                    val msg_time = each_msg.msg_time
                    val message_id = each_msg.message_id
                    val receiver_id = each_msg.receiver_id
                    val direction = each_msg.direction
                    val message_text = each_msg.message_text
                    val message_read = each_msg.message_read

                    if(receiver_id.equals(each_receiver)){
                        var tmp_msg_list: Message.IndividualMessage = Message.IndividualMessage(msg_time, message_id, direction, message_text, message_read)
                        tmp_msg_list_for_view_message.add(tmp_msg_list)

                        if(message_read.equals("n")) new_msg = true
                        if(message_id > tmp_last_msg_id) tmp_last_msg_id = message_id
                    }
                }

                val row_to_insert: String
                if(new_msg) row_to_insert = each_receiver + "와 주고 받은 메세지 (새 메세지)"
                else row_to_insert = each_receiver + "와 주고 받은 메세지"
                Constants.myMessageList.add(row_to_insert)

                temp_struc = Message(each_receiver, tmp_last_msg_id, tmp_msg_list_for_view_message)
                Constants.myMessageStruc.add(temp_struc)

                msg_index++
            }

            sort_message_list()
// ******* following is an example of how to access actual messages ******* //
/*for(i in 0 until msg_index){
    var tmp_str: String = Constants.myMessageList[i] + "  /  " + Constants.myMessageStruc[i].last_msg_id
    val gara_msg_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage(0, "s", Constants.myMessageStruc[i].receiver_id + "에게 메세지 보내기", "n"))
    var message: Message = Message("", 0, gara_msg_list)

    for (j in 0 until (Constants.myMessageStruc[i].messages?.size)!!) {

        tmp_str = tmp_str + " / " + (Constants.myMessageStruc[i].messages!![j].message.toString())
    }
    Log.e("chatting", tmp_str)
}*/
// ************************************************************************ //
        }


    }

    /*private suspend fun get_my_message_list(){

        val searchJSONObject = JSONObject()
        searchJSONObject.put("action","get_my_msg")
        searchJSONObject.put("user_id", Constants.myID)

        val result: Int = get_my_message_list_Helper(searchJSONObject.toString())

        if(result != Constants.WANTED_LIST_SUCCESS) {
            Toast.makeText(this@MessageBoxActivity, "내 메세지를 가져오지 못했습니다", Toast.LENGTH_LONG).show()
            return
        }
    }*/

    private fun refresh_my_message_list(adapter: ArrayAdapter<String>){
        adapter.notifyDataSetChanged()
    }

    /*private suspend fun get_my_message_list_Helper(msg_info: String): Int{
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
                    val message_result_json: String = stringBuilder.toString()

                    val messageResultObject = JSONObject(message_result_json)

                    val message_list_result: String = messageResultObject.getString("message_result")

                    if (message_list_result.equals("success")) {
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS

                        val message_result_list: JSONArray = messageResultObject.getJSONArray("data")
                        val message_result = process_message_json_array(message_result_list)

                        if (message_result == Constants.WANTED_LIST_SUCCESS) {
                            get_list_return_val = Constants.WANTED_LIST_SUCCESS
                        } else {
                            get_list_return_val = Constants.WANTED_LIST_FAIL
                        }
                    } else if(message_list_result.equals("no_message")){
                        get_list_return_val = Constants.WANTED_LIST_SUCCESS
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
    }*/

    /*private fun process_message_json_array(message_result: JSONArray): Int{

        var return_value = Constants.WANTED_LIST_FAIL

        Constants.myMessageList.clear()
        Constants.myMessageStruc.clear()

        for (i in 0 until message_result.length()) {

            val item = message_result.getJSONObject(i)

            val receiver_id = item.getString("receiver_id")
            val temp_struc: Message
            var tmp_array_list: ArrayList<Message.IndividualMessage> = arrayListOf(Message.IndividualMessage(0, null, null, null))
            tmp_array_list.clear()
            val row_to_insert: String = receiver_id + "와 주고 받은 메세지"
            var tmp_last_msg_id: Int = 0

            val msg_array: JSONArray = item.getJSONArray("messages")
            for(j in 0 until msg_array.length()){
                val msg_item_str: String = msg_array.get(j).toString()
                val msg_item: JSONObject = JSONObject(msg_item_str)

                val direction: String = msg_item.get("direction").toString()
                val msg: String = msg_item.get("msg").toString()
                val msg_id: Int = msg_item.getString("msg_id").toInt()
                var tmp_msg_list: Message.IndividualMessage = Message.IndividualMessage(msg_id, direction, msg, null)
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

/************************************************************************************
Toast.makeText(this@MainPageActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()

val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@MessageBoxActivity, String(postData), Toast.LENGTH_SHORT).show()
})


 ************************************************************************************/

