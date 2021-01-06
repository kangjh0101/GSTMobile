package com.gst.matchfinder.ui.club

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
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


class ClubAdActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;
        //Permission code
        private val PERMISSION_CODE = 1001;
    }

    var file_to_upload: String? = null
    var file_format: String? = null
    var club_comment: String? = null
    var club_intro: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_ad)

        val sharedPref = this?.getSharedPreferences("gst.loginInfo", Context.MODE_PRIVATE)
        val sharedIdValue = sharedPref.getString("id_key","no_id")
        if(sharedIdValue != null && sharedIdValue != "no_id"){
            Constants.myID = sharedIdValue
        }

        file_to_upload = null
        file_format = null

        val club_pic_select_btn = findViewById<Button>(R.id.select_club_pic_button)
        val club_post_btn = findViewById<Button>(R.id.club_ad_post_button)
        val club_comment_text = findViewById<EditText>(R.id.club_comment_input)
        val club_intro_text = findViewById<EditText>(R.id.club_intro_input)

        club_pic_select_btn.setOnClickListener {
            file_to_upload = null
            file_format = null

            //check runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    //show popup to request runtime permission
                    requestPermissions(permissions, PERMISSION_CODE);
                }
                else{
                    //permission already granted
                    pickImageFromGallery();
                }
            }
            else{
                //system OS is < Marshmallow
                pickImageFromGallery();
            }
        }

        club_post_btn.setOnClickListener {
            club_comment = club_comment_text.text.toString()
            club_intro = club_intro_text.text.toString()

            if(club_intro == null || club_intro.equals("") || club_intro!!.length < 5){
                Toast.makeText(this, "클럽 소개 제목를 5자 이상 작성하세요", Toast.LENGTH_LONG).show()
            } else if((club_comment == null || club_comment.equals("") || club_comment!!.length < 20) && (file_to_upload == null || file_format == null)){
                Toast.makeText(this, "클럽 소개를 20자 이상 작성하거나.\n클럽 소개 사진을 고르세요.", Toast.LENGTH_LONG).show()
            } else {
                launch {
                    upload_club_info()
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size >0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()

                }
                else{
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            val club_pic_view = findViewById<ImageView>(R.id.club_pic_imageView)
            club_pic_view.setImageURI(data?.data)

            if (data != null && data.data != null) {
                val uri: Uri = data.data!!
                val file_path: String? = getRealPathFromUri(this@ClubAdActivity, uri)

                if(file_path != null){
                    val file_to_upload_tmp = read_image_from_disk(file_path)
                    val file_format_tmp = extract_file_format(file_path)

                    if(file_to_upload_tmp == null || file_format_tmp == null) {
                        club_pic_view.setImageURI(null)
                        Toast.makeText(this@ClubAdActivity, "선택하신 사진을 읽을 수 없습니다", Toast.LENGTH_LONG).show()

                    } else if(file_format_tmp!!.toLowerCase().equals("png") || file_format_tmp!!.toLowerCase().equals("jpg")){
                        file_to_upload = file_to_upload_tmp
                        file_format = file_format_tmp

                    } else{
                        club_pic_view.setImageURI(null)
                        Toast.makeText(this@ClubAdActivity, "JPG 또는 PNG 파일 형식만 지원합니다", Toast.LENGTH_LONG).show()
                    }
                }
                else{
                    club_pic_view.setImageURI(null)
                    Toast.makeText(this@ClubAdActivity, "선택하신 사진을 읽을 수 없습니다", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun upload_club_info(){

        val lessonJSONObject = JSONObject()
        lessonJSONObject.put("action","club_detail_upload")
        lessonJSONObject.put("user_id", Constants.myID)
        lessonJSONObject.put("data_format", file_format)
        lessonJSONObject.put("intro", club_intro)
        lessonJSONObject.put("comment", club_comment)
        lessonJSONObject.put("data", file_to_upload)

        val result: Int = upload_club_info_Helper(lessonJSONObject.toString())

        if(result == Constants.POST_WANTED_SUCCESS) {
            Toast.makeText(this@ClubAdActivity, "동호회 홍보가 등록되었습니다", Toast.LENGTH_LONG).show()
            finish()

        } else{
            Toast.makeText(this@ClubAdActivity, "동호회 홍보를 등록하지 못습니다\n다시 시도하세요", Toast.LENGTH_LONG).show()
            return
        }
    }

    private suspend fun upload_club_info_Helper(id_info: String): Int {
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
                    val register_result: String = registerResultObject.getString("club_detail_result")

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

    private fun read_image_from_disk(real_path: String): String?{

        var image_read_result: String? = null
        var file_byte_array: ByteArray? = null

        try {
            file_byte_array = File(real_path).readBytes()   //java.lang.OutOfMemoryError: Failed to allocate a 3456082 byte allocation with 3130984 free bytes and 2MB until OOM
            //image_read_result = Base64.encodeToString(file_byte_array, Base64.DEFAULT)
            image_read_result = Base64.encodeToString(file_byte_array, Base64.DEFAULT)
        } catch(e: Exception){
            e.printStackTrace()
        }

        return image_read_result
    }


    private fun getRealPathFromUri(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null)
            val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor!!.moveToFirst()
            cursor!!.getString(column_index)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }

    private fun extract_file_format(file_path: String): String? {

        var file_format: String? = null

        var st = StringTokenizer(file_path, ".")
        while(st.hasMoreTokens()){
            file_format = st.nextToken()
        }
//Toast.makeText(this@ClubAdActivity, file_format, Toast.LENGTH_LONG).show()
        return file_format
    }



}


//Toast.makeText(this@ClubAdActivity, uri.toString(), Toast.LENGTH_LONG).show()























