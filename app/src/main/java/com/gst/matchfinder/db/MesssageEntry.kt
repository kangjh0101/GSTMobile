package com.gst.matchfinder.db

import android.speech.tts.TextToSpeech
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.w3c.dom.Text
import java.sql.Timestamp

@Entity
data class MessageEntry (
    @PrimaryKey @ColumnInfo(name = "message_id") val message_id: Long,
    @ColumnInfo(name = "msg_time") val msg_time: String?,
    @ColumnInfo(name = "receiver_id") val receiver_id: String,
    @ColumnInfo(name = "direction") val direction: String,
    @ColumnInfo(name = "message_text") val message_text: String?,
    @ColumnInfo(name = "message_read") val message_read: String
)