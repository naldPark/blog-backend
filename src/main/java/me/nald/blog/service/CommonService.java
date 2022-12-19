package me.nald.blog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.model.ContactRequest;
import me.nald.blog.response.Response;
import me.nald.blog.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
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

    public Response.CommonRes sendMail(ContactRequest contactRequest) {
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
        inputList.add("{src: 'gitbook.png', header: 'CURRENT BLOG', title: 'GITBOOK', text: 'blog',  href: 'https://blog.nald.me'}");
        inputList.add("{src: 'github.png', header: 'Configuration Management Tool', title: 'GITHUB',  text: 'github',  href: 'https://github.com/naldPark'}");
        inputList.add("{src: 'naverblog.png', header: 'BLOG (Deprecated)', title: 'NAVER BLOG',  text: 'naver',  href: 'https://blog.naver.com/8734747'}");

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(Util.stringListToHashMapList(inputList))
                .build();

        return result;
    }


    public Response.CommonRes getBadgeList() {
        List<String> badgeList = Arrays.asList(
                "{ color: 'white', backgroundColor: '#4fc08d', name: 'vue', src: 'vue.svg' }",
                "{ color: 'white', backgroundColor: '#DD0031', name: 'angular', src: 'angular.svg' }",
                "{ color: 'white', backgroundColor: '#007ACC', name: 'typescript', src: 'typescript.svg' }",
                "{ color: 'white', backgroundColor: '#339933', name: 'node-js', src: 'node-js.svg' }",
                "{ color: 'white', backgroundColor: '#73398D', name: 'amcharts', src: 'amchart.svg' }",
                "{ color: 'white', backgroundColor: '#6DB33F', name: 'springBoot', src: 'springboot.svg' }",
                "{ color: 'black', backgroundColor: '#F2F4F9', name: 'tomcat', src: 'tomcat.svg' }",
                "{ color: 'green', backgroundColor: '#F2F4F9', name: 'JPA', src: 'jpa.svg' }",
                "{ color: 'white', backgroundColor: '#000000', name: 'mybatis', src: 'mybatis.svg' }",
                "{ color: 'orange', backgroundColor: '#000000', name: 'prometheus', src: 'prometheus.svg' }",
                "{ color: 'black', backgroundColor: '#F2F4F9', name: 'grafana', src: 'grafana.svg' }",
                "{ color: 'white', backgroundColor: '#005571', name: 'elastic-search', src: 'elastic-search.svg' }",
                "{ color: 'white', backgroundColor: '#2300f', name: 'mysql', src: 'mysql.svg' }",
                "{ color: 'white', backgroundColor: '#47A248', name: 'mongodb', src: 'mongodb.svg' }",
                "{ color: 'white', backgroundColor: '#003545', name: 'mariadb', src: 'mariadb.svg' }",
                "{ color: 'black', backgroundColor: '#ffca28', name: 'firebase', src: 'firebase.svg' }",
                "{ color: 'white', backgroundColor: '#326CE5', name: 'kubernetes', src: 'kubernetes.svg' }",
                "{ color: 'white', backgroundColor: '#2496ED', name: 'docker', src: 'docker.svg' }",
                "{ color: 'white', backgroundColor: '#FF9900', name: 'aws', src: 'aws.svg' }",
                "{ color: 'white', backgroundColor: '#D24939', name: 'jenkins', src: 'jenkins.svg' }",
                "{ color: 'white', backgroundColor: '#009639', name: 'nginx', src: 'nginx.svg' }",
                "{ color: 'black', backgroundColor: '#FCC624', name: 'linux', src: 'linux.svg' }"
        );
        return Response.CommonRes.builder()
                .statusCode(200)
                .data(Util.stringListToHashMapList(badgeList))
                .build();

    }

    public Response.CommonRes getDiagramList() {

        List<String> diagramList = Arrays.asList(
                "{ key: 0, name: 'Nald', icon: 'nald', description: 'nald.me' }",
                "{ key: 1, parent: 0, name: 'Infra', icon: 'infra', description: 'infra' }",
                "{ key: 2, parent: 0, name: 'Frontend', icon: 'frontend', description: 'frontend' }",
                "{ key: 3, parent: 0, name: 'Backend', icon: 'backend', description: 'backend' }",
                "{ key: 101, parent: 0, name: 'Infra',  isGroup: true }",
                "{ key: 102, parent: 0, name: 'Frontend', isGroup: true }",
                "{ key: 103, parent: 0, name: 'Backend',  isGroup: true }",
                "{ key: 4, parent: 1, name: 'Jenkins', icon: 'jenkins', group: 101 }",
                "{ key: 5, parent: 1, name: 'Argocd', icon: 'argocd', group: 101 }",
                "{ key: 6, parent: 3, name: 'Maven', icon: 'maven',  group: 103 }",
                "{ key: 7, parent: 3, name: 'Java', icon: 'java', asd: 'backend language', group: 103 }",
                "{ key: 8, parent: 2, name: 'Vue', icon: 'vue',  group: 102 }",
                "{ key: 9, parent: 3, name: 'JPA', icon: 'jpa', asd: 'JPA', group: 103 }",
                "{ key: 10, parent: 3, name: 'Typescript', icon: 'typescript', group: 102 }",
                "{ key: 12, parent: 1, name: 'Kubernetes', icon: 'kubernetes', group: 101 }",
                "{ key: 13, parent: 1, name: 'Docker', icon: 'docker',group: 101 }",
                "{ key: 14, parent: 2, name: 'Nginx', icon: 'nginx',  group: 102 }",
                "{ key: 15, parent: 1, name: 'Ubuntu', icon: 'ubuntu', group: 101 }",
                "{ key: 16, parent: 3, name: 'SpringBoot', icon: 'springBoot',  group: 103 }",
                "{ key: 18, parent: 1, name: 'Nexus', icon: 'nexus', group: 101 }",
                "{ key: 19, parent: 2, name: 'JavaScript', icon: 'javaScript', group: 102 }",
                "{ key: 20, parent: 3, name: 'Mariadb', icon: 'mariadb', group: 103 }",
                "{ key: 21, parent: 1, name: 'Nas', icon: 'nas', group: 101 }",
                "{ key: 22, parent: 1, name: 'Github', icon: 'git', group: 101 }",
                "{ key: 23, parent: 1, name: 'Helm', icon: 'helm',  group: 101 }"
        );
        return Response.CommonRes.builder()
                .statusCode(200)
                .data(Util.stringListToHashMapList(diagramList))
                .build();
    }

}




