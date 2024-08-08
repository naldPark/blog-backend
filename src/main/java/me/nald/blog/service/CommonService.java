package me.nald.blog.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.ContactRequestDto;
import me.nald.blog.exception.Errors;
import me.nald.blog.response.Response;
import me.nald.blog.service.helper.AccountStore;
import me.nald.blog.util.CommonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static me.nald.blog.exception.ErrorSpec.TooManyRequests;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommonService {

    private final BlogProperties blogProperties;
    private final AccountStore accountStore;


    public Response.CommonRes sendMail(ContactRequestDto contactRequest) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        if(!accountStore.checkMailSentCount(request)){
            throw Errors.of(TooManyRequests, "only 5 times available per day");
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

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(data)
                .build();

        return result;
    }

    public Response.CommonRes getBlogList() {




        List<String> inputList = new ArrayList<>();
        inputList.add("{src: 'gitbook.png', header: 'CURRENT BLOG', title: 'GITBOOK', text: 'blog',  href: 'https://daylog.nald.me'}");
        inputList.add("{src: 'github.png', header: 'Configuration Tool', title: 'GITHUB',  text: 'github',  href: 'https://github.com/naldPark'}");
        inputList.add("{src: 'naverblog.png', header: 'BLOG (Deprecated)', title: 'NAVER BLOG',  text: 'naver',  href: 'https://blog.naver.com/8734747'}");

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(CommonUtils.stringListToHashMapList(inputList))
                .build();

        return result;
    }


    public Response.CommonRes getBadgeList() {

        List<String> badgeList = Arrays.asList(

                //Front

                "{ color: '#0A7390', backgroundColor: '#1A1C1D', name: 'react', src: 'react.svg' }",
                "{ color: 'white', backgroundColor: '#4fc08d', name: 'vue', src: 'vue.svg' }",
                "{ color: 'white', backgroundColor: '#DD0031', name: 'angular', src: 'angular.svg' }",
                "{ color: 'white', backgroundColor: '#007ACC', name: 'typescript', src: 'typescript.svg' }",

                //Backend
                "{ color: 'red', backgroundColor: '#F2F4F9', name: 'java', src: 'java.svg' }",
                "{ color: 'white', backgroundColor: '#6DB33F', name: 'springBoot', src: 'springboot.svg' }",

                // DB
                "{ color: 'white', backgroundColor: '#005e86', name: 'mysql', src: 'mysql.svg' }",
                "{ color: 'white', backgroundColor: '#003545', name: 'mariadb', src: 'mariadb.svg' }",
                "{ color: 'white', backgroundColor: '#000000', name: 'redis', src: 'redis.svg' }",
                "{ color: 'white', backgroundColor: '#47A248', name: 'mongodb', src: 'mongodb.svg' }",

                //Infra
                "{ color: 'white', backgroundColor: '#326CE5', name: 'kubernetes', src: 'kubernetes.svg' }",
                "{ color: 'white', backgroundColor: '#2496ED', name: 'docker', src: 'docker.svg' }",
                "{ color: 'white', backgroundColor: '#FF9900', name: 'aws', src: 'aws.svg' }",
                "{ color: 'white', backgroundColor: '#D24939', name: 'jenkins', src: 'jenkins.svg' }",
                "{ color: 'white', backgroundColor: '#2A0E4E', name: 'argoCD', src: 'argocd.svg' }",
                "{ color: 'orange', backgroundColor: '#000000', name: 'prometheus', src: 'prometheus.svg' }",
                "{ color: 'black', backgroundColor: '#F2F4F9', name: 'grafana', src: 'grafana.svg' }",
                
                //etc
                "{ color: 'black', backgroundColor: '#FCC624', name: 'linux', src: 'linux.svg' }"



        );
        return Response.CommonRes.builder()
                .statusCode(200)
                .data(CommonUtils.stringListToHashMapList(badgeList))
                .build();

    }

    public void mailCountReset(){

        accountStore.mailCountReset();

    }



}




