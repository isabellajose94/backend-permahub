package org.indie.isabella.permahub.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class MailService {
    @Autowired
    private lateinit var javaMailSender: JavaMailSender

    fun send(to: String, subject: String, content: String) {
        val mailMessage = javaMailSender.createMimeMessage()
        val mimeMessageHelper = MimeMessageHelper(mailMessage)
        mimeMessageHelper.setTo(to)
        mimeMessageHelper.setSubject(subject)
        mimeMessageHelper.setText( content, true)

        javaMailSender.send(mailMessage)
    }
}