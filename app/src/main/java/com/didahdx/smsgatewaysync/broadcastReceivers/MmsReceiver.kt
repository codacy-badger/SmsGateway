package com.didahdx.smsgatewaysync.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.didahdx.smsgatewaysync.utilities.ACTION_MMS_RECEIVED
import timber.log.Timber

class MmsReceiver : BroadcastReceiver() {

    private val MMS_DATA_TYPE = "application/vnd.wap.mms-message"

    override fun onReceive(context: Context?, intent: Intent?) {

        val action = intent?.action
        val type = intent?.type

        if (action.equals(ACTION_MMS_RECEIVED) && type.equals(MMS_DATA_TYPE)) {

            val bundle = intent?.extras
            Timber.d("bundle $bundle");
            var msgs = {}
            var str = ""
            var contactId = -1;
            var address: String? = null

            if (bundle != null) {
                var buffer = bundle.getByteArray("data");
                Timber.d("buffer $buffer");
                var incomingNumber = buffer?.let { String(it) };
                var indx = incomingNumber?.indexOf("/TYPE");
                if (indx != null) {
                    if (indx > 0 && (indx - 15) > 0) {
                        var newIndx = indx - 15;
                        incomingNumber = incomingNumber?.substring(newIndx, indx);
                        indx = incomingNumber?.indexOf("+");
                        if (indx != null) {
                            if (indx > 0) {
                                incomingNumber = incomingNumber?.substring(indx);
                                Timber.d("Mobile Number: $incomingNumber");
                            }
                        }
                    }
                }

                var transactionId = bundle.getInt("transactionId");
                Timber.d("transactionId $transactionId");

                var pduType = bundle.getInt("pduType");
                Timber.d("pduType $pduType");

                var buffer2 = bundle.getByteArray("header");
                val header = buffer2?.let { String(it) };
                Timber.d("header $header");

                if (contactId != -1) {
//                    showNotification(contactId, str);
                }

                // ---send a broadcast intent to update the MMS received in the
                // activity---
                val broadcastIntent = Intent();
                broadcastIntent.action = "MMS_RECEIVED_ACTION";
                broadcastIntent.putExtra("mms", str);
                context?.sendBroadcast(broadcastIntent);

            }
        }
    }
}