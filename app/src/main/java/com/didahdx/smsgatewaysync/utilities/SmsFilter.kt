package com.didahdx.smsgatewaysync.utilities

class SmsFilter {

    var name: String = "N/A"
    var phoneNumber: String = "N/A"
    var amount: String = "N/A"
    var date: String = "N/A"
    var time: String = "N/A"
    var mpesaId: String = "N/A"
    var mpesaType:String="N/A"

    //returns the sms format to be printed
    fun checkSmsType(message: String): String {
        try {
            mpesaId = message.split("\\s".toRegex()).first().trim()
            amount =
                message.substring(message.indexOf("Ksh") - 1, message.indexOf(".00") + 3).trim()
            date = message.substring(message.indexOf("/") - 2, message.lastIndexOf("/") + 3).trim()
            time = message.substring(message.indexOf(":") - 2, message.indexOf(":") + 6).trim()
            name = extractName(message)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return messageFormat()
    }

    //format used for print out
    private fun messageFormat(): String {
        return "Payment details:\n Name: ${name.toUpperCase()}  \n Phone No: $phoneNumber \n Amount: $amount " +
                "\n Transaction Date: $date \n Time: $time \n Transaction ID: ${mpesaId.toUpperCase()}\n\n\n"
    }


    private fun extractName(message: String): String {
        var name: String = "N/A"
        val pattern = "^[0-9]{10}$"

        //sending to another user
        if (message.toLowerCase().indexOf("sent to") != -1 && message.indexOf(pattern) != -1) {
            name = message.substring(message.indexOf("sent to") + 4, message.indexOf("07") - 1)
            phoneNumber = message.substring(message.indexOf("07") - 1, message.indexOf("07") + 11)
            mpesaType= SEND_MONEY
        }

        //pay bill
        if (message.toLowerCase().indexOf("sent to") != -1 && message.indexOf("for account") != -1) {
            mpesaType=PAY_BILL
            name = message.substring(
                message.indexOf("sent to") + 7,
                message.indexOf("for account") - 1
            )
            phoneNumber =
                message.substring(message.indexOf("account") + 7, message.indexOf("/") - 5)
        } else if (message.toLowerCase().indexOf("sent to") != -1) {
            name = message.substring(message.indexOf("sent to") + 7, message.indexOf("07") - 1)
            phoneNumber = message.substring(message.indexOf("07"), message.indexOf("/") - 5)
            mpesaType= PAY_BILL
        }

        //widthdraw
        if (message.toLowerCase().indexOf("withdraw") != -1) {
            name = message.substring(message.indexOf("from") + 4, message.indexOf("New") - 1)
            mpesaType= WITHDRAW
        }

        //buy goods and services
        if (message.indexOf("paid to") != -1) {
            name = message.substring(message.indexOf("paid to") + 7, message.indexOf("/") - 5)
            mpesaType= BUY_GOODS_AND_SERVICES
        }

        //receiving from another user
        if (message.indexOf("received") != -1) {
            name = message.substring(message.indexOf("from") + 4, message.indexOf("07") - 1)
            phoneNumber = message.substring(message.indexOf("07") - 1, message.indexOf("07") + 11)
            mpesaType= RECEIVED_PAYMENT
        }


        //deposit to your mpesa account
        if (message.toLowerCase().indexOf("give ksh") != -1 &&
            message.toLowerCase().indexOf("cash to") != -1
        ) {
            name = message.toLowerCase()
                .substring(message.indexOf("cash to") + 4, message.indexOf("New") - 1)
            mpesaType= DEPOSIT
        }

        return name
    }



}