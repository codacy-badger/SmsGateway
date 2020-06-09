package com.didahdx.smsgatewaysync.utilities

import java.util.regex.Matcher
import java.util.regex.Pattern

class SmsFilter() {

    var otpCode: String = NOT_AVAILABLE
    var otpWebsite: String = NOT_AVAILABLE
    var name: String = NOT_AVAILABLE
    var phoneNumber: String = NOT_AVAILABLE
    var amount: String = NOT_AVAILABLE
    var date: String = NOT_AVAILABLE
    var time: String = NOT_AVAILABLE
    var mpesaId: String = NOT_AVAILABLE
    var mpesaType: String = NOT_AVAILABLE
    var accountNumber: String = NOT_AVAILABLE


    constructor(messageBody: String, maskedPhoneNumber: Boolean) : this() {
        checkSmsType(messageBody.trim(), maskedPhoneNumber)
    }

    //returns the sms format to be printed
    fun checkSmsType(message: String, maskedPhoneNumber: Boolean): String {

        try {
            getOTPValues(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mpesaId = message.trim().split("\\s".toRegex()).first().trim()

            if (!MPESA_ID_PATTERN.toRegex().matches(mpesaId)) {
                mpesaId = NOT_AVAILABLE
            }

            amount = extractAmount(message)
            date = message.substring(message.indexOf("/") - 2, message.lastIndexOf("/") + 3).trim()
            time = message.substring(message.indexOf(":") - 2, message.indexOf(":") + 6).trim()
            name = extractName(message)

        } catch (e: Exception) {
            e.printStackTrace()
        }


        return messageFormat(maskedPhoneNumber)
    }

    private fun extractAmount(message: String): String {
        var amount = NOT_AVAILABLE
        val pattern: Pattern = Pattern.compile("^Ksh[0-9,]+(\\.[0-9]{1,2})?\\\$")
        val matcher: Matcher = pattern.matcher(message)
        if (matcher.find()) {
            amount = matcher.group(1)
        }


        return amount
    }

    /**
     * format used for print out
     ***********/
    private fun messageFormat(maskedPhoneNumber: Boolean): String {
        var mNumber = phoneNumber
        if (maskedPhoneNumber) {
            mNumber = getMaskedPhoneNumber(mNumber)
        }

        return "\n\nPAYMENT DETAILS:\n" +
                "-------------------------------" +
                "\n Name: ${name.toUpperCase().trim()} \n\n Phone No: $mNumber " +
                "\n\n Amount: $amount \n\n Transaction Date: $date \n\n Time: $time " +
                "\n\n Transaction ID: ${mpesaId.toUpperCase()} \n\n " +
                "-------------------------------\n" +
                "******** END OF RECEIPT ******* \n\n."
    }


    private fun extractName(message: String): String {
        var name: String = NOT_AVAILABLE
        val pattern = "^[0-9]{10}$"

        /**
         * sending money to another user
         ******/
        if (message.toLowerCase().indexOf("sent to") != -1
            && message.indexOf(pattern) != -1
        ) {
            name = message.substring(message.indexOf("sent to") + 4, message.indexOf("07") - 1)
            phoneNumber = message.substring(message.indexOf("07") - 1, message.indexOf("07") + 11)
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("sent to") - 1).trim()
            mpesaType = NOT_AVAILABLE
        } else if (message.toLowerCase().indexOf("sent to") != -1
            && message.indexOf(pattern) != -1
            && message.indexOf("+254") != -1
        ) {
            name = message.substring(message.indexOf("sent to") + 4, message.indexOf("+254") - 1)
            phoneNumber =
                message.substring(message.indexOf("+254") - 1, message.indexOf("+254") + 14)
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("sent to") - 1).trim()
            mpesaType = NOT_AVAILABLE
        }

        /**
         * pay bill for client side
         ******/
        if (message.toLowerCase().indexOf("sent to") != -1
            && message.indexOf("for account") != -1
        ) {
            mpesaType = NOT_AVAILABLE
            name = message.substring(
                message.indexOf("sent to") + 7,
                message.indexOf("for account") - 1
            )
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("sent to") - 1).trim()
            phoneNumber =
                message.substring(message.indexOf("account") + 7, message.indexOf("/") - 5)
        } else if (message.toLowerCase().indexOf("sent to") != -1
            && message.indexOf("07") != -1
        ) {
            name = message.substring(message.indexOf("sent to") + 7, message.indexOf("07") - 1)
            phoneNumber = message.substring(message.indexOf("07"), message.indexOf("/") - 5)
            mpesaType = NOT_AVAILABLE
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("sent to") - 1).trim()
        } else if (message.toLowerCase().indexOf("sent to") != -1
            && message.indexOf("+254") != -1
        ) {
            name = message.substring(message.indexOf("sent to") + 7, message.indexOf("+254") - 1)
            phoneNumber = message.substring(message.indexOf("+254"), message.indexOf("/") - 5)
            mpesaType = NOT_AVAILABLE
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("sent to") - 1).trim()
        }

        /**
         * widthdraw from agent
         ******/
        if (message.toLowerCase().indexOf("withdraw") != -1) {
            name = message.substring(message.indexOf("from") + 4, message.indexOf("New") - 1)
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("from") - 1).trim()
            mpesaType = NOT_AVAILABLE
        }

        /**
         * buy goods and services for client side
         ******/
        if (message.indexOf("paid to") != -1) {
            name = message.substring(message.indexOf("paid to") + 7, message.indexOf("/") - 5)
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("paid to") - 1).trim()
            mpesaType = NOT_AVAILABLE
        }

        /**
         * receiving money from another user
         ******/
        try {
            if (message.indexOf("received") != -1 && message.indexOf("07") != -1 && message.indexOf(
                    "from"
                ) != -1
            ) {
                name = message.substring(message.indexOf("from") + 4, message.indexOf("07") - 1)
                phoneNumber =
                    message.substring(message.indexOf("07") - 1, message.indexOf("07") + 11)
                mpesaType = DIRECT_MPESA
                amount =
                    message.substring(message.indexOf("Ksh") - 1, message.indexOf("from") - 1)
                        .trim()
            } else if (message.indexOf("received") != -1 && message.indexOf("+254") != -1 && message.indexOf(
                    "from"
                ) != -1
            ) {
                name = message.substring(message.indexOf("from") + 4, message.indexOf("+254") - 1)
                phoneNumber =
                    message.substring(message.indexOf("+254") - 1, message.indexOf("+254") + 14)
                amount =
                    message.substring(message.indexOf("Ksh") - 1, message.indexOf("from") - 1)
                        .trim()
                mpesaType = DIRECT_MPESA
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (message.indexOf("received") != -1 && message.indexOf("from") != -1 &&
            message.toLowerCase().indexOf("via") != -1
        ) {
            try {
                phoneNumber = NOT_AVAILABLE
                mpesaType = DIRECT_MPESA
                name = message.substring(message.indexOf("from") + 4,
                    message.indexOf("in") - 1).trim()
                amount = message.substring(message.indexOf("Ksh") - 1,
                    message.indexOf("from") - 1)
                        .trim()
                accountNumber = message.substring(
                    message.indexOf("in") + 2,
                    message.toLowerCase().indexOf("via") - 1
                ).trim()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        if (message.indexOf("received") != -1 && message.indexOf("from") != -1 &&
            message.toLowerCase().indexOf("via") != -1 &&
            message.toLowerCase().indexOf("buy goods") != -1
        ) {
            mpesaType = DIRECT_MPESA
            phoneNumber = NOT_AVAILABLE
            amount = message.substring(message.indexOf("Ksh") - 1,
                message.indexOf("from") - 1).trim()
            accountNumber = message.substring(message.toLowerCase().indexOf("via") + 3,
                message.toLowerCase().indexOf("/") - 6).trim()
            name = message.substring(message.indexOf("from") + 4,
                message.toLowerCase().indexOf("via") - 1).trim()

        }

        if (message.indexOf("received") != -1 && message.indexOf("from") != -1 &&
            message.toLowerCase().indexOf("congratulations!") != -1
        ) {
            mpesaType = DIRECT_MPESA
            phoneNumber = NOT_AVAILABLE
            amount = message.substring(
                message.indexOf("Ksh") - 1,
                message.indexOf("from") - 1
            ).trim()
            mpesaId = message.split(" ")[1]
            name = message.substring(
                message.indexOf("from") + 4,
                message.indexOf("/") - 6
            ).trim()
        }

        /**
         * deposit to your mpesa account
         ******/
        if (message.toLowerCase().indexOf("give ksh") != -1 &&
            message.toLowerCase().indexOf("cash to") != -1
        ) {
            name = message.toLowerCase()
                .substring(message.indexOf("cash to") + 7, message.indexOf("New") - 1)
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf("cash to") - 1).trim()
            mpesaType = NOT_AVAILABLE
        }


        /**
         * pay bill seller side
         ******/
        if (message.indexOf("received from") != -1 && message.indexOf("254") != -1) {
            name = message.substring(message.indexOf("from") + 4, message.indexOf("254") - 1)
            phoneNumber = message.substring(message.indexOf("254") - 1, message.indexOf("254") + 12)
            accountNumber = message.substring(
                message.toLowerCase().indexOf("account number") + 14,
                message.toLowerCase().indexOf("new utility") - 1
            )

            amount = message.substring(message.indexOf("Ksh") - 1, message.indexOf("received") - 1)
                .trim()
            mpesaType = PAY_BILL
        }

        if (name.indexOf(".") != -1) {
            name.replace(".", "")
        }

        return name
    }

    //generates masked phone numbers
    private fun getMaskedPhoneNumber(phoneNumber: String): String {
        var numberLength = phoneNumber.length - 7
        var unknown = ""
        val lastCount = phoneNumber.length - 3

        while (numberLength > 0) {
            unknown += "X"
            numberLength--
        }

        return if (phoneNumber.length < 7) {
            "XXXXXXXXXX"
        } else {
            phoneNumber.substring(0, 4) + unknown + phoneNumber.substring(lastCount, lastCount + 3)
        }
    }


    private fun getOTPValues(messageBody: String) {
        otpWebsite = messageBody.substring(
            messageBody.indexOf("@") + 1,
            messageBody.indexOf("#")
        ).trim()
        otpCode = messageBody.substring(messageBody.indexOf("#") + 1)

    }

}