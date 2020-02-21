package com.didahdx.smsgatewaysync.utilities

/**
 * Constants used throughout the app.
 */

const val INPUT_EXTRAS = "inputExtras"
const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
const val CHANNEL_ID="SmsServiceChannel"
const val LOG_FILE_NAME="smsLog.txt"
const val APP_NAME="Sms Gateway Sync"
const val ACTIVEMQ_TAG = "ActiveMQ"
const val clientId = "any_client_name"
const val serverURI = "tcp://197.156.190.141:1883" //replace with your ip
const val publishTopic = "outbox"
const val subscribeTopic = "inbox"

/**
 * MPESA TYPES
 * */
const val PAY_BILL="PAY_BILL"
const val BUY_GOODS_AND_SERVICES="BUY_GOODS_AND_SERVICES"
const val WITHDRAW="WITHDRAW"
const val SEND_MONEY="SEND_MONEY"
const val DEPOSIT="DEPOSIT"
const val RECEIVED_PAYMENT="RECEIVED_PAYMENT"


/**
 * PREFERENCES String
 * */
const val PREF_HOST_URL="host_url"
const val PREF_MPESA_TYPE="mpesa_types"
const val PREF_AUTO_UPLOAD_SMS="auto_sms_upload"
const val PREF_CONNECT_PRINTER="connect_printer"
const val PREF_AUTO_PRINT="auto_print"

/**
 *  user permission codes
 */
const val PERMISSION_RECEIVE_SMS_CODE = 2
const val PERMISSION_READ_SMS_CODE = 100
const val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE = 500
const val PERMISSION_CALL_PHONE_CODE=1200
const val PERMISSION_FOREGROUND_SERVICES_CODE=1300