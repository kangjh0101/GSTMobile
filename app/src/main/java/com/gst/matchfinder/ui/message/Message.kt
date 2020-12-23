package com.gst.matchfinder.ui.message

class Message(
    val receiver_id: String,
    val last_msg_id: Long,
    val messages: ArrayList<IndividualMessage>?) {
    class IndividualMessage(
        val msg_time: String?,
        val msg_id: Long?,
        val direction: String?,
        val message: String?,
        var read: String?){}
}