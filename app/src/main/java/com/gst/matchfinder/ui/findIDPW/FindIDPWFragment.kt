package com.gst.matchfinder.ui.findIDPW

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import kotlinx.android.synthetic.main.activity_find_i_d_p_w.*
import kotlinx.android.synthetic.main.fragment_find_i_d_p_w.*
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


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private var page_num: Int? = 0

/**
 * A simple [Fragment] subclass.
 * Use the [FindIDPWFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FindIDPWFragment : Fragment(), CoroutineScope by MainScope() {

    companion object {
        const val ARG_POSITION = "position"

        fun getInstance(position: Int): Fragment {
            page_num = position
            val findidpwFragment = FindIDPWFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_POSITION, position)
            findidpwFragment.arguments = bundle
            return findidpwFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_find_i_d_p_w, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val position = requireArguments().getInt(ARG_POSITION)

        val find_pw_btn = getView()?.findViewById<Button>(R.id.button_find_pw)
        val find_id_btn = getView()?.findViewById<Button>(R.id.button_find_id)

        if (position == 0) {
            id_name.visibility = View.VISIBLE
            id_name_input.visibility = View.VISIBLE
            id_phonenumber.visibility = View.VISIBLE
            id_phonenumber_input.visibility = View.VISIBLE
            id_email.visibility = View.VISIBLE
            id_email_input.visibility = View.VISIBLE
            button_find_id.visibility = View.VISIBLE
            //id_slide.visibility = View.VISIBLE

            pw_id.visibility = View.INVISIBLE
            pw_name_input.visibility = View.INVISIBLE
            pw_phonenumber.visibility = View.INVISIBLE
            pw_phonenumber_input.visibility = View.INVISIBLE
            pw_email.visibility = View.INVISIBLE
            pw_email_input.visibility = View.INVISIBLE
            button_find_pw.visibility = View.INVISIBLE
            //pw_slide.visibility = View.INVISIBLE

            if (find_id_btn != null) {
                find_id_btn.setOnClickListener {
                    launch {
                        val name_txt_input = getView()?.findViewById<EditText>(R.id.id_name_input)
                        val phone_txt_input = getView()?.findViewById<EditText>(R.id.id_phonenumber_input)
                        val email_txt_input = getView()?.findViewById<EditText>(R.id.id_email_input)

                        if (name_txt_input != null && phone_txt_input != null && email_txt_input != null) {
                            find_id(name_txt_input.text.toString(), phone_txt_input.text.toString(), email_txt_input.text.toString())
                        }
                    }

                    //finish()
                    return@setOnClickListener
                }
            }

        } else if (position == 1) {
            id_name.visibility = View.INVISIBLE
            id_name_input.visibility = View.INVISIBLE
            id_phonenumber.visibility = View.INVISIBLE
            id_phonenumber_input.visibility = View.INVISIBLE
            id_email.visibility = View.INVISIBLE
            id_email_input.visibility = View.INVISIBLE
            button_find_id.visibility = View.INVISIBLE
            //id_slide.visibility = View.INVISIBLE

            pw_id.visibility = View.VISIBLE
            pw_name_input.visibility = View.VISIBLE
            pw_phonenumber.visibility = View.VISIBLE
            pw_phonenumber_input.visibility = View.VISIBLE
            pw_email.visibility = View.VISIBLE
            pw_email_input.visibility = View.VISIBLE
            button_find_pw.visibility = View.VISIBLE
            //pw_slide.visibility = View.VISIBLE

            if (find_pw_btn != null) {
                find_pw_btn.setOnClickListener {
                    launch {
                        val id_txt_input = getView()?.findViewById<EditText>(R.id.pw_name_input)
                        val phone_txt_input = getView()?.findViewById<EditText>(R.id.pw_phonenumber_input)
                        val email_txt_input = getView()?.findViewById<EditText>(R.id.pw_email_input)

                        if (id_txt_input != null && phone_txt_input != null && email_txt_input != null) {
                            find_pw(id_txt_input.text.toString(), phone_txt_input.text.toString(), email_txt_input.text.toString())
                        }
                    }

                    //finish()
                    return@setOnClickListener
                }
            }

        } else {
            Toast.makeText(getActivity(), "invalid page num: $page_num", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun find_pw(id_txt: String, phone_txt: String, email_txt: String){
        val msgObject= JSONObject()
        msgObject.put("action","find_pw")
        msgObject.put("user_id", id_txt)
        msgObject.put("phone_number", phone_txt)
        msgObject.put("email", email_txt)

        val result: Int = find_pw_helper(msgObject.toString())

        if(result == Constants.FIND_ID_PW_SUCCESS) {
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this.context, "PASSWORD를 " + email_txt + "로 전송했습니다.", Toast.LENGTH_LONG).show()
            })

            return
        } else if(result == Constants.FIND_ID_PW_NO_ID_FOUND){
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this.context, "사용자 ID가 존재하지 않습니다", Toast.LENGTH_LONG).show()
            })

            return
        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this.context, "PASSWORD 찾기에 실패했습니다.\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            })

            return
        }
    }

    private suspend fun find_pw_helper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var send_report_result: Int = Constants.SEND_REPORT_SUCCESS
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
                    val register_result: String = registerResultObject.getString("find_pw_result")

                    if (register_result.equals("success")) {
                        send_report_result = Constants.FIND_ID_PW_SUCCESS
                    } else {
                        val message: String = registerResultObject.getString("message")

                        if(message.toInt() == Constants.FIND_ID_PW_NO_ID_FOUND) {
                            send_report_result = Constants.FIND_ID_PW_NO_ID_FOUND
                        } else{
                            send_report_result = Constants.FIND_ID_PW_EMAIL_FAIL
                        }
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    send_report_result = Constants.FIND_ID_PW_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext send_report_result
            } catch(e: SocketTimeoutException){
                return@withContext Constants.FIND_ID_PW_NETWORK_ERROR
            } catch(e: Exception){
                return@withContext Constants.FIND_ID_PW_NETWORK_ERROR
            }
        }
    }

    private suspend fun find_id(name_txt: String, phone_txt: String, email_txt: String){
        val msgObject= JSONObject()
        msgObject.put("action","find_id")
        msgObject.put("user_name", name_txt)
        msgObject.put("phone_number", phone_txt)
        msgObject.put("email", email_txt)

        val result: Int = find_id_helper(msgObject.toString())

        if(result == Constants.FIND_ID_PW_SUCCESS) {
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this.context, "ID를 " + email_txt + "로 전송했습니다.", Toast.LENGTH_LONG).show()
            })

            return
        } else if(result == Constants.FIND_ID_PW_NO_NAME_FOUND){
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this.context, "사용자 이름이 존재하지 않습니다", Toast.LENGTH_LONG).show()
            })

            return
        } else{
            val handler = Handler(Looper.getMainLooper())
            handler.post(kotlinx.coroutines.Runnable {
                Toast.makeText(this.context, "ID 찾기에 실패했습니다.\n다시 시도해 주세요", Toast.LENGTH_LONG).show()
            })

            return
        }
    }

    private suspend fun find_id_helper(id_info: String): Int {
        return withContext(Dispatchers.Default) {

            var send_report_result: Int = Constants.SEND_REPORT_SUCCESS
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
                    val register_result: String = registerResultObject.getString("find_id_result")

                    if (register_result.equals("success")) {
                        send_report_result = Constants.FIND_ID_PW_SUCCESS
                    } else {
                        val message: String = registerResultObject.getString("message")

                        if(message.toInt() == Constants.FIND_ID_PW_NO_NAME_FOUND) {
                            send_report_result = Constants.FIND_ID_PW_NO_NAME_FOUND
                        } else{
                            send_report_result = Constants.FIND_ID_PW_EMAIL_FAIL
                        }
                    }

                    reader.close()
                    inputStream.close()

                } else{
                    val errorstream: DataInputStream = DataInputStream(httpClient.errorStream)

                    send_report_result = Constants.FIND_ID_PW_SERVER_ERROR

                    errorstream.close()
                }

                return@withContext send_report_result
            } catch(e: SocketTimeoutException){
                return@withContext Constants.FIND_ID_PW_NETWORK_ERROR
            } catch(e: Exception){
                return@withContext Constants.FIND_ID_PW_NETWORK_ERROR
            }
        }
    }
}