package com.didahdx.smsgatewaysync.ui.fragments

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.didahdx.smsgatewaysync.R
import com.didahdx.smsgatewaysync.data.db.MessagesDatabase
import com.didahdx.smsgatewaysync.data.db.entities.MpesaMessageInfo
import com.didahdx.smsgatewaysync.model.SmsInfo
import com.didahdx.smsgatewaysync.ui.adapters.MessageAdapter
import com.didahdx.smsgatewaysync.utilities.*
import kotlinx.android.synthetic.main.fragment_sms_inbox.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class SmsInboxFragment : Fragment(R.layout.fragment_sms_inbox){}
//    , MessageAdapter.OnItemClickListener {
//
//    private lateinit var sharedPreferences: SharedPreferences
//    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
//    var mMessageAdapter: MessageAdapter? = null
//    private var messageList: ArrayList<MpesaMessageInfo> = ArrayList<MpesaMessageInfo>()
//    lateinit var navController: NavController
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        refresh_layout_home2?.setOnRefreshListener { backgroundCoroutineCall() }
//        backgroundCoroutineCall()
//        navController = Navigation.findNavController(view)
//
//    }
//
//    //used to get sms from the phone
//    private fun backgroundCoroutineCall() {
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            refresh_layout_home2?.isRefreshing = true
//            text_loading2?.text = getString(R.string.loading_messages, 0)
//            //coroutine background job
//            CoroutineScope(IO).launch {
//                getDbSmsMessages()
//            }
//        } else {
//            progress_bar2?.hide()
//            text_loading2?.hide()
//            setUpAdapter()
//        }
//
//    }
//
//    private fun setUpAdapter() {
//        progress_bar2?.hide()
//        text_loading2?.hide()
//        recycler_view_message_list2?.layoutManager = LinearLayoutManager(activity)
//        mMessageAdapter = MessageAdapter(messageList, this)
//        recycler_view_message_list2?.adapter = mMessageAdapter
//        refresh_layout_home2?.isRefreshing = false
//
//        if (messageList.size <= 0) {
//            text_loading2?.show()
//            text_loading2?.text = "No messages available at the moment"
//        }
//    }
//
//
//    private suspend fun getDbSmsMessages() {
//
//        val messageArrayList = ArrayList<MpesaMessageInfo>()
//
//        val cursor = activity?.contentResolver?.query(
//            Uri.parse("content://sms/inbox"),
//            null,
//            null,
//            null,
//            null
//        )
//
//        var messageCount = 0
//
//        if (cursor != null && cursor.moveToNext()) {
//            val nameId = cursor.getColumnIndex("address")
//            val messageId = cursor.getColumnIndex("body")
//            val dateId = cursor.getColumnIndex("date")
//            val mpesaType = sharedPreferences.getString(PREF_MPESA_TYPE, DIRECT_MPESA)
//
//
//            Log.d("TAG", "mpesa sms $mpesaType ")
//
//            do {
//                val dateString = cursor.getString(dateId)
//
////                if (cursor.getString(nameId) == "MPESA") {
//
//                var mpesaId: String =
//                    cursor.getString(messageId).split("\\s".toRegex()).first().trim()
//                if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
//                    mpesaId = NOT_AVAILABLE
//                }
//
//                var smsFilter = SmsFilter(cursor.getString(messageId))
//
//
//                when (mpesaType) {
//                    PAY_BILL -> {
//                        if (smsFilter.mpesaType == PAY_BILL) {
//                            messageArrayList.add(
//                                MpesaMessageInfo(
//
//                                    cursor.getString(messageId),
//                                    sdf.format(Date(dateString.toLong())).toString(),
//                                    cursor.getString(nameId),
//                                    mpesaId,
//                                    "",
//                                    smsFilter.amount,
//                                    "",
//                                    smsFilter.name,
//                                    dateString.toLong(), true, "", ""
//                                )
//                            )
//                            updateCounter(messageCount)
//                            messageCount++
//                        }
//                    }
//                    DIRECT_MPESA -> {
//                        if (smsFilter.mpesaType == DIRECT_MPESA) {
//                            messageArrayList.add(
//                                MpesaMessageInfo(
//                                    messageId,
//                                    cursor.getString(messageId),
//                                    sdf.format(Date(dateString.toLong())).toString(),
//                                    cursor.getString(nameId),
//                                    mpesaId,
//                                    "",
//                                    smsFilter.amount,
//                                    "",
//                                    smsFilter.name,
//                                    dateString.toLong(), true, "", ""
//                                )
//                            )
//                            updateCounter(messageCount)
//                            messageCount++
//                        }
//                    }
//
//                    BUY_GOODS_AND_SERVICES -> {
//                        if (smsFilter.mpesaType == BUY_GOODS_AND_SERVICES) {
//                            messageArrayList.add(
//                                MpesaMessageInfo(
//                                    messageId,
//                                    cursor.getString(messageId),
//                                    sdf.format(Date(dateString.toLong())).toString(),
//                                    cursor.getString(nameId),
//                                    mpesaId,
//                                    "",
//                                    smsFilter.amount,
//                                    "",
//                                    smsFilter.name,
//                                    dateString.toLong(), true, "", ""
//                                )
//                            )
//                            updateCounter(messageCount)
//                            messageCount++
//                        }
//                    }
//
//                    else -> {
//                        messageArrayList.add(
//                            MpesaMessageInfo(
//                                messageId,
//                                cursor.getString(messageId),
//                                sdf.format(Date(dateString.toLong())).toString(),
//                                cursor.getString(nameId),
//                                mpesaId,
//                                "",
//                                smsFilter.amount,
//                                "",
//                                smsFilter.name,
//                                dateString.toLong(), true, "", ""
//                            )
//                        )
//                        updateCounter(messageCount)
//                        messageCount++
//                    }
//                }
////                }
//            } while (cursor.moveToNext())
//
//            cursor.close()
//        }
//
//        passMessagesToMain(messageArrayList)
//    }
//
//    //updates the counter on the screen
//    private suspend fun updateCounter(messageCount: Int) {
//        withContext(Main) {
//            text_loading2?.text = getString(R.string.loading_messages, messageCount)
//        }
//    }
//
//    //adds message to the screen
//    private suspend fun passMessagesToMain(list: ArrayList<MpesaMessageInfo>) {
//        withContext(Main) {
//            messageList.clear()
//            messageList = ArrayList<MpesaMessageInfo>(list)
//            setUpAdapter()
//        }
//    }
//
//    override fun onItemClick(position: Int) {
//        val messageInfo: MpesaMessageInfo = messageList[position]
//
//        val date = messageInfo.date
//        var smsStatus: String
//
//        CoroutineScope(IO).launch {
//            context?.let {
//                val messagesList = MessagesDatabase(it).getIncomingMessageDao().getMessage(date)
//
//
//
//                CoroutineScope(Main).launch {
//
//                    smsStatus = if (!messagesList.isNullOrEmpty() && messagesList[0].status) {
//                        if (messageInfo.status) {
//                            "Uploaded"
//                        } else {
//                            "pending"
//                        }
//                    } else {
//                        NOT_AVAILABLE
//                    }
//
//
//                    val smsInfo = SmsInfo(
//                        messageInfo.messageBody,
//                        messageInfo.time,
//                        messageInfo.sender,
//                        smsStatus,
//                        messageInfo.longitude,
//                        messageInfo.latitude)
//
//                    val bundle= bundleOf("SmsInfo" to smsInfo)
//                    navController.navigate(R.id.action_smsInboxFragment_to_smsDetailsFragment,bundle)
//
//
//                }
//
//            }
//
//        }
//
//
//    }
//
//    override fun onPrintPdf(position: Int) {
//
//    }
//}
