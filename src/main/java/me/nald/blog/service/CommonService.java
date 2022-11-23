package me.nald.blog.service;


import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;
import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.model.ContactRequest;
import me.nald.blog.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommonService {

    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }

    public Response.CommonRes  sendMail(ContactRequest contactRequest) {
        Properties props = new Properties();
        HashMap<String, Object> data = new HashMap<>();

        String host = blogProperties.getContactHost();
        String user = blogProperties.getContactUser();
        String password = blogProperties.getContactPassword();
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

        String receiver = blogProperties.getContactEmail();
        String title = "[Blog 문의] "+contactRequest.getTitle();
        String content = "<p>회신요청 이메일: "+contactRequest.getEmail()+"</p><br><pre>"+contactRequest.getContent()+"</pre>";
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress( blogProperties.getContactEmail(), contactRequest.getName(), "utf-8"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
            message.setSubject(title);
            message.setContent(content, "text/html; charset=utf-8");
            Transport.send(message);
            data.put("message", "succeeded");
        } catch (Exception e) {
            e.printStackTrace();
            data.put("error", "error");
        }

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(data)
                .build();

        return result;
    }

}




