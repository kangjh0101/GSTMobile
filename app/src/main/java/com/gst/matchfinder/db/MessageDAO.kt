package com.gst.matchfinder.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDAO {

    @Query("select message_id from messageEntry where message_id = :msg_id")
    fun checkMessagesID(msg_id: Long): List<Long>

    @Query("select * from messageEntry order by receiver_id")
    fun getAllMessages(): List<MessageEntry>

    @Query("select receiver_id from messageEntry group by receiver_id")
    fun selectReceiverIDs(): List<String>

    @Query("select * from messageEntry where receiver_id in (:receiver_ids)")
    fun selectAllBySender(receiver_ids: List<String>): List<MessageEntry>

    @Query("update messageEntry set message_read = 'y' where receiver_id = :receiver_id")
    fun updateReadBySender(receiver_id: String)

    @Query("delete from messageEntry where receiver_id = :exit_receiver")
    fun deleteMessageBySender(exit_receiver: String)

    @Insert
    fun insertAll(vararg messagedb: MessageEntry)

    @Delete
    fun delete(messageEntry: MessageEntry)
}