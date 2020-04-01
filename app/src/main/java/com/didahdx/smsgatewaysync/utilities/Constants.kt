package com.didahdx.smsgatewaysync.utilities

/**
 * Constants used throughout the app.
 */
const val SMS_DATE="SMS_DATE"
const val SMS_SENDER="SMS_SENDER"
const val SMS_BODY="SMS_BODY"
const val DATE_FORMAT="dd/MMM/yy hh:mm aaa"
const val MPESA_ID_PATTERN="^[A-Z0-9]*$"
const val SENT = "SMS_SENT"
const val DELIVERED = "SMS_DELIVERED"
const val RED_COLOR="RED_COLOR"
const val GREEN_COLOR="GREEN_COLOR"
const val NOT_AVAILABLE="N/A"

const val APP_SERVICE_KEY = "APP_SERVICE_KEY"
const val APP_SERVICE_STATE = "APP_SERVICE_STATE"

const val INPUT_EXTRAS = "inputExtras"
const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
const val LOG_FILE_NAME="smsLog.txt"
const val APP_NAME="Sms Router"
const val CHANNEL_ID="sms_services_notification"
const val CHANNEL_NAME="$APP_NAME status"

/**
 *  Queues
 * **/
const val NOTIFICATION="notification"
const val PUBLISH_FROM_CLIENT="current"

/**
 * MPESA TYPES
 * */
const val PAY_BILL="Pay Bill"
const val BUY_GOODS_AND_SERVICES="Buy Goods and Services"
const val DIRECT_MPESA="Direct Mpesa"
const val ALL="All"

/**
 * PREFERENCES String
 * */
const val PREF_HOST_URL="host_url"
const val PREF_MPESA_TYPE="mpesa_types"
const val PREF_AUTO_UPLOAD_SMS="auto_sms_upload"
const val PREF_CONNECT_PRINTER="connect_printer"
const val PREF_AUTO_PRINT="auto_print"
const val PREF_PHONE_NUMBER="forward_sms_number"
const val PREF_ENABLE_FORWARD_SMS="enable_forward_sms"
const val PREF_SERVICES_KEY="services_key"
const val PREF_FEEDBACK="feedback"

/**
 *  user permission codes
 */
const val PERMISSION_RECEIVE_SMS_CODE = 100
const val PERMISSION_READ_SMS_CODE = 200
const val PERMISSION_SEND_SMS_CODE = 300
const val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE = 400
const val PERMISSION_CALL_PHONE_CODE=500
const val PERMISSION_FOREGROUND_SERVICES_CODE=600
const val PERMISSION_WRITE_CONTACTS_CODE=700