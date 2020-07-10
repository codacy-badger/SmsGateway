package com.didahdx.smsgatewaysync.utilities

/**
 * Constants used throughout the app.
 */
const val GIGABYTE = 1073741824
const val DATE_FORMAT = "dd/MMM/yy hh:mm:ss aaa"
const val MPESA_ID_PATTERN = "^[A-Z0-9]*$"
const val RED_COLOR = "RED_COLOR"
const val GREEN_COLOR = "GREEN_COLOR"
const val GREY_COLOR = "GREY_COLOR"
const val NOT_AVAILABLE = "NA"

const val SIM_CARD_1="Sim Card 1"
const val SIM_CARD_2="Sim Card 2"
const val LOG_FILE_NAME = "smsLog.txt"
const val APP_NAME = "Sms Router"
const val CHANNEL_ID = "sms_services_notification"
const val CHANNEL_SMS_SERVICE_NAME = "$APP_NAME status"
const val CHANNEL_ID_2 = "update_services"
const val CHANNEL_ID_3 = "important_sms"
const val CHANNEL_CLIENT_NOTIFICATION_NAME = "update messages"
const val CHANNEL_IMPORTANT_SMS_NOTIFICATION = "important sms "
const val MESSAGE_DATABASE = "messageDatabase"
const val ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED"
const val KEY_TASK_MESSAGE = "KEY_TASK_MESSAGE"
const val KEY_EMAIL="KEY_EMAIL"
const val KEY_PHONE_NUMBER="KEY_PHONE_NUMBER"
const val KEY_TASK_PRINT="KEY_TASK_PRINT"
const val KEY_TASK_MESSAGE_API="KEY_TASK_MESSAGE_API"
const val MINIMUM_LOCATION_TIME:Long= 120000
const val MINIMUM_LOCATION_DISTANCE= 0.7f
const val ERROR_CONNECTING_TO_SERVER="Error connecting to server"
const val ANDROID_PHONE="android phone"

/**
 * Service constants
 * */
const val APP_SERVICE_STATE = "APP_SERVICE_STATE"
const val RESTART_SERVICE_STATE="RESTART_SERVICE_STATE"


/**
 * Intent variables
 * */
const val CALL_TYPE_EXTRA = "CALL_TYPE_EXTRA"
const val PHONE_NUMBER_EXTRA = "PHONE_NUMBER_EXTRA"
const val START_TIME_EXTRA = "START_TIME_EXTRA"
const val END_TIME_EXTRA = "END_TIME_EXTRA"
const val SMS_SENT_INTENT = "SMS_SENT"
const val SMS_DELIVERED_INTENT = "SMS_DELIVERED_INTENT"
const val SMS_RECEIVED_INTENT = "android.provider.Telephony.SMS_RECEIVED"
const val PHONE_STATE="android.intent.action.PHONE_STATE"
const val NEW_OUTGOING_CALL="android.intent.action.NEW_OUTGOING_CALL"
const val INPUT_EXTRAS = "INPUT_EXTRAS"
const val SMS_DATE_EXTRA = "SMS_DATE_EXTRA"
const val SMS_SENDER_EXTRA = "SMS_SENDER_EXTRA"
const val SMS_BODY_EXTRA = "SMS_BODY_EXTRA"
const val SMS_UPLOAD_STATUS_EXTRA = "SMS_UPLOAD_STATUS_EXTRA"
const val LONGITUDE_EXTRA = "LONGITUDE_EXTRA"
const val LATITUDE_EXTRA = "LATITUDE_EXTRA"
const val ALTITUDE_EXTRA = "ALTITUDE_EXTRA"
const val LOCATION_UPDATE_INTENT = "LOCATION_UPDATE_INTENT"
const val SMS_LOCAL_BROADCAST_RECEIVER = "SMS_LOCAL_BROADCAST_RECEIVER"
const val CALL_LOCAL_BROADCAST_RECEIVER = "CALL_LOCAL_BROADCAST_RECEIVER"
const val BATTERY_LOCAL_BROADCAST_RECEIVER = "BATTERY_LOCAL_BROADCAST_RECEIVER"
const val BATTERY_PERCENTAGE_EXTRA = "BATTERY_PERCENTAGE_EXTRA"
const val BATTERY_CONDITION_EXTRA = "BATTERY_CONDITION_EXTRA"
const val BATTERY_TEMPERATURE_CELIUS_EXTRA = "BATTERY_TEMPERATURE_EXTRA"
const val BATTERY_TEMPERATURE_EXTRA = "BATTERY_TEMPERATURE_EXTRA"
const val BATTERY_POWER_SOURCE_EXTRA = "BATTERY_POWER_SOURCE_EXTRA"
const val BATTERY_CHARGING_STATUS_EXTRA = "BATTERY_CHARGING_STATUS_EXTRA"
const val BATTERY_TECHNOLOGY_EXTRA = "BATTERY_TECHNOLOGY_EXTRA"
const val BATTERY_VOLTAGE_EXTRA = "BATTERY_VOLTAGE_EXTRA"
const val STATUS_INTENT_BROADCAST_RECEIVER="STATUS_INTENT_BROADCAST_RECEIVER"
const val STATUS_MESSAGE_EXTRA="STATUS_MESSAGE_EXTRA"
const val STATUS_COLOR_EXTRA="STATUS_COLOR_EXTRA"

/**
 *  Queues
 * **/
const val NOTIFICATION = "notification"
const val PUBLISH_FROM_CLIENT = "current"

/**
 * MPESA TYPES
 * */
const val PAY_BILL = "Pay Bill"
const val BUY_GOODS_AND_SERVICES = "Buy Goods and Services"
const val DIRECT_MPESA = "Direct Mpesa"
const val ALL = "All"

/**
 * PREFERENCES String
 * */
const val PREF_HOST_URL = "host_url"
const val PREF_HOST_URL_ENABLED = "push_url"
const val PREF_MPESA_TYPE = "mpesa_types"
const val PREF_AUTO_UPLOAD_SMS = "auto_sms_upload"
const val PREF_CONNECT_PRINTER = "connect_printer"
const val PREF_AUTO_PRINT = "auto_print"
const val PREF_PHONE_NUMBER = "forward_sms_number"
const val PREF_ENABLE_FORWARD_SMS = "enable_forward_sms"
const val PREF_SERVICES_KEY = "services_key"
const val PREF_FEEDBACK = "feedback"
const val PREF_SIM_CARD = "sim_card"
const val PREF_PRINT_TYPE = "print_types"
const val PREF_MASKED_NUMBER = "masked_phone_number"
const val PREF_IMPORTANT_SMS_NOTIFICATION = "mpesa_types_notification"
const val PREF_HANG_UP = "hang_up"
const val PREF_PRINTER_NAME = "PREF_PRINTER_NAME"
const val PREF_STATUS_MESSAGE="PREF_STATUS_MESSAGE"
const val PREF_STATUS_COLOR="PREF_STATUS_COLOR"
const val PREF_USER_EMAIL="PREF_USER_EMAIL"
const val PREF_RABBITMQ_CONNECTION="PREF_RABBITMQ_CONNECTION"

/**
 * preference type
 * */
const val PREF_TYPE_STRING="PREF_TYPE_STRING"
const val PREF_TYPE_BOOLEAN="PREF_TYPE_BOOLEAN"
const val PREF_TYPE_INT="PREF_TYPE_INT"

/**
 *  user permission codes
 */
const val PERMISSION_REQUEST_ALL_CODE = 1111
const val PERMISSION_RECEIVE_SMS_CODE = 1011
const val PERMISSION_READ_SMS_CODE = 2022
const val PERMISSION_SEND_SMS_CODE = 3033
const val PERMISSION_WRITE_EXTERNAL_STORAGE_CODE = 4044
const val PERMISSION_CALL_PHONE_CODE = 5055
const val PERMISSION_FOREGROUND_SERVICES_CODE = 6066
const val PERMISSION_WRITE_CONTACTS_CODE = 7077
const val PERMISSION_ACCESS_FINE_LOCATION_CODE = 8088
const val PERMISSION_ACCESS_COARSE_LOCATION_CODE = 9099
const val PERMISSION_READ_PHONE_STATE_CODE = 2011