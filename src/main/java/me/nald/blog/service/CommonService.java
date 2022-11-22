package me.nald.blog.service;


import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;
import lombok.RequiredArgsConstructor;
import me.nald.blog.response.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommonService {

    public Response.CommonRes  sendMail() {
        Properties props = new Properties();

        String host = "smtp.nald.me";  //smtp.gmail.com
        String user = "abc@nald.me";
        String password = "password";
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", 465);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        String receiver = "receiveMail@gmail.com"; // 메일 받을 주소
        String title = "테스트 메일입니다.";
        String content = "<h2 style='color:blue'>안녕하세요</h2>";
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress("abc@nald.me", "관리자", "utf-8"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
            message.setSubject(title);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap<String, Object> data = new HashMap<>();

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(data)
                .build();

        return result;
    }

}




