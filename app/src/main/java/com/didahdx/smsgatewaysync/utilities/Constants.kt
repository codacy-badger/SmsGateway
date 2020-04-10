package com.didahdx.smsgatewaysync.utilities

/**
 * Constants used throughout the app.
 */
const val DATE_FORMAT="dd/MMM/yy hh:mm aaa"
const val MPESA_ID_PATTERN="^[A-Z0-9]*$"
const val RED_COLOR="RED_COLOR"
const val GREEN_COLOR="GREEN_COLOR"
const val NOT_AVAILABLE="N/A"

const val APP_SERVICE_KEY = "APP_SERVICE_KEY"
const val APP_SERVICE_STATE = "APP_SERVICE_STATE"

const val LOG_FILE_NAME="smsLog.txt"
const val APP_NAME="Sms Router"
const val CHANNEL_ID="sms_services_notification"
const val CHANNEL_NAME="$APP_NAME status"
const val MESSAGE_DATABASE="messageDatabase"

/**
 * Intent variables
 * */
const val SMS_SENT_INTENT = "SMS_SENT"
const val SMS_DELIVERED_INTENT = "SMS_DELIVERED_INTENT"
const val SMS_RECEIVED_INTENT = "android.provider.Telephony.SMS_RECEIVED"
const val INPUT_EXTRAS = "INPUT_EXTRAS"
const val SMS_DATE_EXTRA="SMS_DATE_EXTRA"
const val SMS_SENDER_EXTRA="SMS_SENDER_EXTRA"
const val SMS_BODY_EXTRA="SMS_BODY_EXTRA"
const val SMS_UPLOAD_STATUS_EXTRA="SMS_UPLOAD_STATUS_EXTRA"
const val LONGITUDE_EXTRA="LONGITUDE_EXTRA"
const val LATITUDE_EXTRA="LATITUDE_EXTRA"
const val ALTITUDE_EXTRA="ALTITUDE_EXTRA"
const val LOCATION_UPDATE_INTENT="LOCATION_UPDATE_INTENT"
const val SMS_LOCAL_BROADCAST_RECEIVER="SMS_LOCAL_BROADCAST_RECEIVER"

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
const val PREF_SIM_CARD="sim_card"

/**
 *  user permission codes
 */
const val PERMISSION_RECEIVE_SMS_CODE = 1011
const val PERMISSION_READ_SMS_CODE = 2022
const val PERMISSION_SEND_SMS_CODE = 3033
const val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE = 4044
const val PERMISSION_CALL_PHONE_CODE=5055
const val PERMISSION_FOREGROUND_SERVICES_CODE=6066
const val PERMISSION_WRITE_CONTACTS_CODE=7077
const val PERMISSION_ACCESS_FINE_LOCATION_CODE=8088
const val PERMISSION_ACCESS_COARSE_LOCATION_CODE=9099
const val PERMISSION_READ_PHONE_STATE_CODE=2011