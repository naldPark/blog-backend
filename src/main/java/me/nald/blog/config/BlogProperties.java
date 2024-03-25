package me.nald.blog.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "nald")
public class BlogProperties {

//    @Value("${jwt.public.key}")
//    private String jwtPublicKey;
//    이렇게 개별로 처리해도 될듯
    
    private long tokenExpiredTime;
    private String privateKey;
    private String publicKey;
    private String defaultAccountId;
    private String defaultAccountName;
    private String defaultAccountPassword;
    private String contactHost;
    private String contactEmail;
    private String contactUser;
    private String contactPassword;
    private String commonPath;
    private String ffmpegPath;

}
