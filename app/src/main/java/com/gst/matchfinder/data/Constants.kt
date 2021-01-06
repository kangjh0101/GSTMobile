package com.gst.matchfinder.data

import com.gst.matchfinder.db.MessageDB
import com.gst.matchfinder.ui.club.ClubAd
import com.gst.matchfinder.ui.hand_over.HandOver
import com.gst.matchfinder.ui.lesson.LessonAd
import com.gst.matchfinder.ui.main.WantedAd
import com.gst.matchfinder.ui.message.Message
import com.gst.matchfinder.ui.wanted.MyWantedAd

/*private const val ssl_key = "-----BEGIN CERTIFICATE-----\n" +
                              "MIIDZTCCAk0CAhI1MA0GCSqGSIb3DQEBCwUAMIGLMQswCQYDVQQGEwJLUjEOMAwG\n" +
                              "A1UECAwFU2VvdWwxDjAMBgNVBAcMBVNlb3VsMQwwCgYDVQQKDANHU1QxDzANBgNV\n" +
                              "BAsMBkdTVERldjEWMBQGA1UEAwwNQ2hpIEh3YW4gS2FuZzElMCMGCSqGSIb3DQEJ\n" +
                              "ARYWa2FuZ2poMDEwMUBob3RtYWlsLmNvbTAeFw0yMDA3MDEwMzU5MTRaFw0zMDA2\n" +
                              "MjkwMzU5MTRaMGQxCzAJBgNVBAYTAktSMQ4wDAYDVQQIEwVTZW91bDEOMAwGA1UE\n" +
                              "BxMFU2VvdWwxDDAKBgNVBAoTA0dTVDEPMA0GA1UECxMGR1NURGV2MRYwFAYDVQQD\n" +
                              "Ew1DaGkgSHdhbiBLYW5nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
                              "8wkaN+H/wY5gWqVVxZaFeZlOPd7wnvf/5RT7aXCM5NvbB2hJ8QnBOl6Vw4lj2XNS\n" +
                              "58wHCcuaVDxN8vZhWabNRcz82JtW7pYk0844/vDQpY6BvHIW68SN0y6hBVlcq0b5\n" +
                              "nSDrLp8qnimheTaTXhFUcvKo9Xz94iOkRxHyIpW4gFfnsbeXSomvrmOMqru43SBG\n" +
                              "cl/qJEZ20FY1QBbBXju7Kc02psRiPN6MCFY9SfW5oQn7oZh5gdBRiGARwvv6k+U+\n" +
                              "b+M5cjd3/RWuNZiKXoxqE9byXX0o1RrIFrvJ0S89MFySs6nk1rCeMWQ04OkvKESt\n" +
                              "h5aiqvxk45EgawWRiMi5ZwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBU9mv6QGAp\n" +
                              "yrXHzv3VJgFAF1MrjHBMuAbKojTY+MykSpg/x+xK9cgqjqJoUXyDCFhWLV03x6ki\n" +
                              "vetM/Eq31sIenrD2uyGhjCL/ZI1uDze+V4E6qFsmejz+ZXoOUKldJQ/67qEI1T8N\n" +
                              "H6elVh9M9Nyrx8tCRt/F8604LIkkw7vY33UpSItSmGrxEngujrAhSO7w3/fYc3Ba\n" +
                              "DF0niJ8lMmgFzLKzIpFrPRA5LO0MfazZ+MBnpc2QBoY0ck8MGBYU9TwBVFzImG8G\n" +
                              "vbXOLoZ2iGrbMf6mJ6s0Wudx7G607lkZtjYK2ctiD5gUgIb7dPPtEsOOuVF4E/VG\n" +
                              "xNmw1qpn0vUH\n" +
                              "-----END CERTIFICATE-----"*/
private const val ssl_key: String = "-----BEGIN CERTIFICATE-----\n" +
        "MIIDijCCAnKgAwIBAgIEeufyjjANBgkqhkiG9w0BAQsFADBtMQswCQYDVQQGEwJL\n" +
        "UjEOMAwGA1UECBMFU2VvdWwxDjAMBgNVBAcTBVNlb3VsMRgwFgYDVQQKEw9HcmFu\n" +
        "ZFNsYW1UZW5uaXMxEDAOBgNVBAsTB0RldlRlYW0xEjAQBgNVBAMTCURldmVsb3Bl\n" +
        "cjAeFw0yMDExMTYxMjI2NTFaFw0yMTAyMTQxMjI2NTFaMG0xCzAJBgNVBAYTAktS\n" +
        "MQ4wDAYDVQQIEwVTZW91bDEOMAwGA1UEBxMFU2VvdWwxGDAWBgNVBAoTD0dyYW5k\n" +
        "U2xhbVRlbm5pczEQMA4GA1UECxMHRGV2VGVhbTESMBAGA1UEAxMJRGV2ZWxvcGVy\n" +
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj5+a8L/hf4oJFJ8DZmAW\n" +
        "jKo3w8E1KKpeHptZSzZysTJIca2s4NKDcFjgW9f7OOGJENh/lUxfhsOvpPVHFl3l\n" +
        "Rni5Wmq3zmkXjYk9xA2L+wTcEKo7MkhYRbQY5JPyv3DihhA+FAoEh6Jm9EjrSVUp\n" +
        "G5bBl3eX/WKoshEcBHUTuGNXema7pNSEJ16QQ8hMjRZ4uePf9JqjJ5iL7ScEyBmq\n" +
        "PkBxJMGPIC3llc6Aqs6xH3GrPAhJlcig1yYMIGe1axa5Hty9bG91AqoLudQYiZKB\n" +
        "eWds2ssC+QhURQkYIlU2xjGofFLg41sED0nH5Njgq1YIgP2GAuzO6Hne2/BAUpor\n" +
        "6wIDAQABozIwMDAPBgNVHREECDAGhwQPpFB5MB0GA1UdDgQWBBRUxo9/RlVWNati\n" +
        "LuROLd7cZz1MYjANBgkqhkiG9w0BAQsFAAOCAQEAeGstqs/rg6vmstSgrYr87iUV\n" +
        "O0QwtWRu9u1G3i3gkZNa+q8Jq9S/2A7/tjkh4jlSy7duMoYzeG0qqzTqpKcbzeNY\n" +
        "GmtIgrOM0dUdWr1b7NmSoarhhE28t0KA4K5GXqpSOTYdNPBvLb2QPgW0cCr2JLr+\n" +
        "Zr/CtS18ID6RA3YKAkzCFENaiJhGzTd7g+re5b3HbHZg2EdmKVWWAsGfyy0pIs5b\n" +
        "kig2iuvgaENfp9LmsetmDLF0Th7ArsVOlG98ip6dOO8KbMfxtuysoGN7PCguBUwV\n" +
        "pRmO9WT80UaHy76D366AHjGM438+L/raInQkPSyZwdICTq8FUrMRQCMK/hgDUg==\n" +
        "-----END CERTIFICATE-----"

private var check_id_done: Boolean = false

class Constants {
    companion object { // this is how global variables are declared

        lateinit var myID: String

        const val app_package_name = "com.gst.matchfinder"
        const val app_version = "2.0.0"

        const val MY_ID = "my.id"
        const val POST_ID = "post.id"
        const val RECEIVER_ID = "receiver.id"

        const val HTTP_TIMEOUT: Int = 10000
        const val HTTP_PROTOCOL = "https"
        const val GST_SERVER = "15.164.80.121"
        const val GST_PORT = "50001"
        const val GST_SUB_URL = "/GSTWebAPI/Req_Service"

        const val FIND_ID_PW_SUCCESS: Int = 1
        const val FIND_ID_PW_FAIL: Int = 2
        const val FIND_ID_PW_NETWORK_ERROR: Int = 3
        const val FIND_ID_PW_SERVER_ERROR: Int = 4
        const val FIND_ID_PW_NO_NAME_FOUND: Int = 5
        const val FIND_ID_PW_NO_ID_FOUND: Int = 6
        const val FIND_ID_PW_EMAIL_FAIL: Int = 7

        const val LOGIN_SUCCESS: Int = 1
        const val LOGIN_FAIL: Int = 2
        const val LOGIN_NETWORK_ERROR: Int = 3
        const val LOGIN_SERVER_ERROR: Int = 4
        const val LOGIN_UPDATE_REQUIRED: Int = 5

        const val WANTED_LIST_SUCCESS: Int = 1
        const val WANTED_LIST_FAIL: Int = 2
        const val WANTED_LIST_NETWORK_ERROR: Int = 3
        const val WANTED_LIST_SERVER_ERROR: Int = 4

        //const val SEND_MESSAGE_SUCCESS: Int = 1
        const val SEND_MESSAGE_FAIL: Long = -1
        const val SEND_MESSAGE_NETWORK_ERROR: Long = -2
        const val SEND_MESSAGE_SERVER_ERROR: Long = -3
        const val SEND_MESSAGE_BLOCKED: Long = -4

        const val EXIT_MESSAGE_SUCCESS: Int = 0
        const val EXIT_MESSAGE_FAIL: Int = 1
        const val EXIT_MESSAGE_NETWORK_ERROR: Int = 2
        const val EXIT_MESSAGE_SERVER_ERROR: Int = 3

        const val BLOCK_USER_SUCCESS: Int = 0
        const val BLOCK_USER_FAIL: Int = 1
        const val BLOCK_USER_NETWORK_ERROR: Int = 2
        const val BLOCK_USER_SERVER_ERROR: Int = 3

        const val WITHDRAW_USER_SUCCESS: Int = 0
        const val WITHDRAW_USER_FAIL: Int = 1
        const val WITHDRAW_USER_NETWORK_ERROR: Int = 2
        const val WITHDRAW_USER_SERVER_ERROR: Int = 3

        const val UNBLOCK_USER_SUCCESS: Int = 0
        const val UNBLOCK_USER_FAIL: Int = 1
        const val UNBLOCK_USER_NETWORK_ERROR: Int = 2
        const val UNBLOCK_USER_SERVER_ERROR: Int = 3

        const val CHATTING_LIST_SUCCESS: Int = 1
        const val CHATTING_LIST_FAIL: Int = 2

        const val POST_WANTED_SUCCESS: Int = 1
        const val POST_WANTED_FAIL: Int = 2
        const val POST_WANTED_NETWORK_ERROR: Int = 3
        const val POST_WANTED_SERVER_ERROR: Int = 4

        const val SEND_REPORT_SUCCESS: Int = 1
        const val SEND_REPORT_FAIL: Int = 2
        const val SEND_REPORT_NETWORK_ERROR: Int = 3
        const val SEND_REPORT_SERVER_ERROR: Int = 4

        const val CHECK_ID_VALID: Int = 1
        const val CHECK_ID_INVALID: Int = 2
        const val CHECK_ID_ERROR: Int = 3

        const val CHECK_USER_BLOCK_WAITING: Int = 0
        const val CHECK_USER_BLOCK_BLOCKED: Int = 1
        const val CHECK_USER_BLOCK_UNBLOCKED: Int = 2
        const val CHECK_USER_BLOCK_FAIL: Int = 3
        const val CHECK_USER_BLOCK_NETWORK_ERROR: Int = 4
        const val CHECK_USER_BLOCK_SERVER_ERROR: Int = 5

        const val REGISTER_SUCCESS: Int = 1
        const val REGISTER_FAIL: Int = 2
        const val REGISTER_NETWORK_ERROR: Int = 3
        const val REGISTER_SERVER_ERROR: Int = 4

        const val OP_SUCC = 0
        const val OP_FAIL = 1

        const val PICK_ADDRESS_REQUEST = 101

        const val MAP_ADDRESS_RESULT_KEY = "map_address_result"

        const val LESSON_LOCATION_SEOUL = 1
        const val LESSON_LOCATION_DAEGU = 2
        const val LESSON_LOCATION_BUSAN = 3
        const val LESSON_LOCATION_ULSAN = 4
        const val LESSON_LOCATION_INCHON = 5
        const val LESSON_LOCATION_DAEJON = 6
        const val LESSON_LOCATION_GWANGJU = 7
        const val LESSON_LOCATION_JEJU = 8
        const val LESSON_LOCATION_GYUNGGI = 9
        const val LESSON_LOCATION_GANGWON = 10
        const val LESSON_LOCATION_CHUNGBUK = 11
        const val LESSON_LOCATION_CHUNGNAM = 12
        const val LESSON_LOCATION_JUNBUK = 13
        const val LESSON_LOCATION_JUNNAM = 14
        const val LESSON_LOCATION_GYUNGBUK = 15
        const val LESSON_LOCATION_GYUNGNAM = 16

        var long_sleep: Boolean = false

        val wantedListHeader: Array<String> = arrayOf("NTRP", "성별", "구력", "코트 예약", "장소")

        var wantedAdList: ArrayList<String> = arrayListOf("모집공고 불러오기를 눌러서 공고를 불러오십시요", "")
        var wantedAdStruc: ArrayList<WantedAd> = arrayListOf(WantedAd("","", "", "", "", "", "", ""))
        //var wantedAdDetail: WantedAd = WantedAd("", "", "", "", "", "", "", "")

        var myWantedAdList: ArrayList<String> = arrayListOf("내 공고 확인하기", "")
        var myWantedAdStruc: ArrayList<MyWantedAd> = arrayListOf(MyWantedAd("","", "", "", "", "", "", "", ""))
        //lateinit var myWantedAdDetail: MyWantedAd

        var lessonAdList: ArrayList<String> = arrayListOf("레슨 공고 확인하기", "")
        var lessonAdStruc: ArrayList<LessonAd> = arrayListOf(LessonAd("","", "", "", "", "", ""))

        var clubAdList: ArrayList<String> = arrayListOf("동호회 모집 공고 확인하기", "")
        var clubAdStruc: ArrayList<ClubAd> = arrayListOf(ClubAd("","", "", "", ""))

        var handoverAdList: ArrayList<String> = arrayListOf("코트 양도 공고 확인하기", "")
        var handoverAdStruc: ArrayList<HandOver> = arrayListOf(HandOver("","", "", ""))

        var myMessageList: ArrayList<String> = arrayListOf("내 메세지 확인하기", "")
        var myMessageStruc: ArrayList<Message> = arrayListOf(Message("", 0, null))
        //lateinit var myWantedAdDetail: MyWantedAd

        var myChatting: ArrayList<String> = arrayListOf("내 메세지")

        var msg_db: MessageDB? = null

        var device_token: String = ""

        var last_active_time: Long = 0

        //var broadcastTriggered: Boolean = false

        fun getCert(): String {
            return ssl_key
        }

        fun getCheckID(): Boolean{
            return check_id_done
        }

        fun setCheckID(isChecked: Boolean){
            check_id_done = isChecked
        }
    }

}