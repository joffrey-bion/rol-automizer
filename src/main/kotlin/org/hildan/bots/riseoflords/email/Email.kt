package org.hildan.bots.riseoflords.email

import java.util.*

data class EmailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
) {
    companion object {
        val GMAIL_PERSO = EmailConfig(
            host = "smtp.gmail.com",
            port = 587,
            username = "joffrey.bion",
            password = System.getenv("GMAIL_PASSWORD")
        )
    }
}

fun EmailConfig.toProperties() = Properties().apply {
    put("mail.from", "")
    put("mail.user", username)
    put("mail.smtp.host", host)
    put("mail.smtp.port", port)
}

fun emailConfig(host: String, port: Int, username: String) = Properties().apply {
    put("mail.from", "")
    put("mail.user", username)
    put("mail.smtp.host", host)
    put("mail.smtp.port", port)
}

fun sendEmail(body: String, toAddress: String, fromAddress: String = "noreply@rol-automizer.org") {

}
