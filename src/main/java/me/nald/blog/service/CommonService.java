package me.nald.blog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.model.ContactRequest;
import me.nald.blog.exception.Errors;
import me.nald.blog.response.Response;
import me.nald.blog.service.store.AccountStore;
import me.nald.blog.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import static me.nald.blog.exception.ErrorSpec.TooManyRequests;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommonService {

    private static BlogProperties blogProperties;
    private final AccountStore accountStore;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }

    public Response.CommonRes sendMail(ContactRequest contactRequest) {

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
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        String receiver = blogProperties.getContactEmail();
        String title = "[Blog 문의] " + contactRequest.getTitle();
        String content = "<p>회신요청 이메일: " + contactRequest.getEmail() + "</p><br><pre>" + contactRequest.getContent() + "</pre>";
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(blogProperties.getContactEmail(), contactRequest.getName(), "utf-8"));
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

    public Response.CommonRes getBlogList() {

        List<String> inputList = new ArrayList<>();
        inputList.add("{src: 'gitbook.png', header: 'CURRENT BLOG', title: 'GITBOOK', text: 'blog',  href: 'https://daylog.nald.me'}");
        inputList.add("{src: 'github.png', header: 'Configuration Tool', title: 'GITHUB',  text: 'github',  href: 'https://github.com/naldPark'}");
        inputList.add("{src: 'naverblog.png', header: 'BLOG (Deprecated)', title: 'NAVER BLOG',  text: 'naver',  href: 'https://blog.naver.com/8734747'}");

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(Util.stringListToHashMapList(inputList))
                .build();

        return result;
    }


    public Response.CommonRes getBadgeList() {
        List<String> badgeList = Arrays.asList(

                //Front
                "{ color: 'white', backgroundColor: '#4fc08d', name: 'vue', src: 'vue.svg' }",
                "{ color: 'white', backgroundColor: '#DD0031', name: 'angular', src: 'angular.svg' }",
                "{ color: '#0A7390', backgroundColor: '#1A1C1D', name: 'react', src: 'react.svg' }",
                "{ color: 'white', backgroundColor: '#009639', name: 'nginx', src: 'nginx.svg' }",
                "{ color: 'white', backgroundColor: '#007ACC', name: 'typescript', src: 'typescript.svg' }",

                //Backend
                "{ color: 'red', backgroundColor: '#F2F4F9', name: 'java', src: 'java.svg' }",
                "{ color: 'white', backgroundColor: '#6DB33F', name: 'springBoot', src: 'springboot.svg' }",
                "{ color: 'green', backgroundColor: '#F2F4F9', name: 'JPA', src: 'jpa.svg' }",
                "{ color: 'black', backgroundColor: '#F2F4F9', name: 'tomcat', src: 'tomcat.svg' }",

                // DB
                "{ color: 'white', backgroundColor: '#000000', name: 'mybatis', src: 'mybatis.svg' }",
                "{ color: 'white', backgroundColor: '#2300f', name: 'mysql', src: 'mysql.svg' }",
                "{ color: 'white', backgroundColor: '#003545', name: 'mariadb', src: 'mariadb.svg' }",
                "{ color: 'white', backgroundColor: '#000000', name: 'redis(x)', src: 'redis.svg' }",
                "{ color: 'white', backgroundColor: '#47A248', name: 'mongodb(x)', src: 'mongodb.svg' }",

                //Infra
                "{ color: 'white', backgroundColor: '#326CE5', name: 'kubernetes', src: 'kubernetes.svg' }",
                "{ color: 'white', backgroundColor: '#2496ED', name: 'docker', src: 'docker.svg' }",
                "{ color: 'white', backgroundColor: '#FF9900', name: 'aws', src: 'aws.svg' }",
                "{ color: 'white', backgroundColor: '#D24939', name: 'jenkins', src: 'jenkins.svg' }",
                "{ color: 'white', backgroundColor: '#2A0E4E', name: 'argoCD', src: 'argocd.svg' }",
                "{ color: 'orange', backgroundColor: '#000000', name: 'prometheus(△)', src: 'prometheus.svg' }",
                "{ color: 'black', backgroundColor: '#F2F4F9', name: 'grafana(△)', src: 'grafana.svg' }",
                "{ color: 'white', backgroundColor: '#005571', name: 'elastic-search(x)', src: 'elastic-search.svg' }",

                "{ color: 'black', backgroundColor: '#3D4245', name: 'kafka(x)', src: 'kafka.svg' }",
                "{ color: 'black', backgroundColor: '#FFFFFF', name: 'ansible(x)', src: 'ansible.svg' }",
                "{ color: 'white', backgroundColor: '#212425', name: 'terraform(x)', src: 'terraform.svg' }",
                "{ color: 'white', backgroundColor: '#0C2232', name: 'sonarqube(x)', src: 'sonarqube.svg' }",

                //etc
                "{ color: 'black', backgroundColor: '#FCC624', name: 'linux', src: 'linux.svg' }"



        );
        return Response.CommonRes.builder()
                .statusCode(200)
                .data(Util.stringListToHashMapList(badgeList))
                .build();

    }

    public void mailCountReset(){

        accountStore.mailCountReset();

    }



}




