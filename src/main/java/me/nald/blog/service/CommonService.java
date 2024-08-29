package me.nald.blog.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.ContactRequestDto;
import me.nald.blog.data.entity.Badge;
import me.nald.blog.exception.MethodNotAllowedException;
import me.nald.blog.repository.BadgeRepository;
import me.nald.blog.response.ResponseCode;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.helper.AccountStore;
import me.nald.blog.util.CommonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;



@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommonService {

    private final BlogProperties blogProperties;
    private final AccountStore accountStore;
    private final BadgeRepository badgeRepository;

    public ResponseObject sendMail(ContactRequestDto contactRequest) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        if(!accountStore.checkMailSentCount(request)){
            new MethodNotAllowedException(log, ResponseCode.TOO_MANY_MAIL_SEND_REQUESTS);
        }
        Properties props = new Properties();
        HashMap<String, Object> data = new HashMap<>();

        String host = blogProperties.getContactHost();
        String user = blogProperties.getContactUser();
        String password = blogProperties.getContactPassword();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", 465);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", host);
        Session session = Session.getDefaultInstance(props);

        String receiver = blogProperties.getContactEmail();
        String title = "[Blog 문의] " + contactRequest.getTitle();
        String content = "<p>회신요청 이메일: " + contactRequest.getEmail() + "</p><br><pre>" + contactRequest.getContent() + "</pre>";

        try (Transport transport = session.getTransport()) {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(blogProperties.getContactEmail(), contactRequest.getName(),"utf-8"));
            message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(String.join(",", receiver)));
            message.setSubject(title, StandardCharsets.UTF_8.name());
            message.setContent(content, "text/html; charset=UTF-8");
            transport.connect(host, user, password);
            transport.sendMessage(message, message.getAllRecipients());
        } catch (MessagingException | UnsupportedEncodingException e) {
//            throw new ServiceUnavailableException(log, ResponseCode.MAIL_SEND_FAIL_ERROR);
        }

        ResponseObject result = new ResponseObject();
        result.putData(data);

        return result;
    }

    public ResponseObject getBlogList() {

        List<String> inputList = new ArrayList<>();
        inputList.add("{src: 'gitbook.png', header: 'CURRENT BLOG', title: 'GITBOOK', text: 'blog',  href: 'https://daylog.nald.me'}");
        inputList.add("{src: 'github.png', header: 'Configuration Tool', title: 'GITHUB',  text: 'github',  href: 'https://github.com/naldPark'}");
        inputList.add("{src: 'naverblog.png', header: 'BLOG (Deprecated)', title: 'NAVER BLOG',  text: 'naver',  href: 'https://blog.naver.com/8734747'}");

        ResponseObject result = new ResponseObject();
        result.putData(CommonUtils.stringListToHashMapList(inputList));

        return result;
    }


    public ResponseObject getBadgeList() {
        List<Badge> badgeList = badgeRepository.findAll();
        ResponseObject result = new ResponseObject();
        result.putData(badgeList);
        return result;

    }

    public void mailCountReset(){

        accountStore.mailCountReset();

    }



}




