package com.didahdx.smsgatewaysync.ui.adapters

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cursoradapter.widget.CursorAdapter
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.didahdx.smsgatewaysync.databinding.SmsInboxContainerBinding
import com.didahdx.smsgatewaysync.model.SmsInboxInfo
import com.didahdx.smsgatewaysync.utilities.DATE_FORMAT
import com.didahdx.smsgatewaysync.utilities.SmsFilter
import java.text.SimpleDateFormat
import java.util.*

class SmsInboxCursorAdapter(context: Context?, c: Cursor?, autoRequery: Boolean, private val clickListener: SmsAdapterListener) :
    CursorAdapter(context, c, autoRequery) {
    var sdf: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT)
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val binding = SmsInboxContainerBinding.inflate(layoutInflater, parent, false)
        return binding.root
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val nameId = cursor?.getColumnIndex("address")
        val messageId = cursor?.getColumnIndex("body")
        val dateId = cursor?.getColumnIndex("date")
        val dateString = dateId?.let { cursor.getString(it) }
        val mpesaId: String =
            messageId?.let { cursor.getString(it)?.split("\\s".toRegex())?.first()?.trim() }!!
        val smsFilter = SmsFilter(cursor.getString(messageId),false)

        val smsinbox= nameId?.let { cursor.getString(it) }?.let {
            dateString?.toLong()?.let { it1 ->
                SmsInboxInfo(
                    messageId,
                    cursor.getString(messageId),
                    sdf.format(dateString?.toLong()?.let { it1 -> Date(it1) }).toString(),
                    it,
                    mpesaId,
                    smsFilter.phoneNumber,
                    smsFilter.amount,
                    smsFilter.accountNumber,
                    smsFilter.name,
                    it1, true, "", ""
                )
            }
        }

        val binding: SmsInboxContainerBinding? = view?.let { DataBindingUtil.getBinding(it) }
        binding?.messageText = smsinbox
//        binding?.clickListener = clickListener
    }




    class SmsViewHolder private constructor(val binding: SmsInboxContainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SmsInboxInfo, clickListener: SmsAdapterListener) {
            binding.messageText = item
//            binding.clickListener = clickListener
        }


    }


}
class SmsAdapterListener(val clickListener: (messageId: SmsInboxInfo) -> Unit) {
    fun onClick(mMessage: SmsInboxInfo) = clickListener(mMessage)
}