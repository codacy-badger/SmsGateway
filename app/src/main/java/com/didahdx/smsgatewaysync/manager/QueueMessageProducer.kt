package com.didahdx.smsgatewaysync.manager

import com.didahdx.smsgatewaysync.utilities.MESSAGE_SIZE
import com.didahdx.smsgatewaysync.utilities.SERVER_URI
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.*

class QueueMessageProducer(
    private val username: String,
    private val password: String
) {
    fun sendDummyMessages(queueName: String?) {
        println("QueueMessageProducer started ")
        var connFactory: ConnectionFactory? = null
        var connection: Connection? = null
        var session: Session? = null
        var msgProducer: MessageProducer? = null
        try {
            connFactory = ActiveMQConnectionFactory(username, password, SERVER_URI)
            connection = connFactory.createConnection()
            connection.start()
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            msgProducer = session.createProducer(session.createQueue(queueName))
            for (i in 0 until MESSAGE_SIZE) {
                val textMessage: TextMessage =
                    session.createTextMessage("Test $i")
                msgProducer.send(textMessage)
                Thread.sleep(30000)
            }
            println("QueueMessageProducer completed")
        } catch (e: JMSException) {
            println("Caught exception: " + e.message)
        } catch (e: InterruptedException) {
            println("Caught exception: " + e.message)
        }
        try {
            msgProducer?.close()
            session?.close()
            connection?.close()
        } catch (ignore: Throwable) {
        }
    }

}