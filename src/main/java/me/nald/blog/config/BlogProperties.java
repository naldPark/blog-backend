package me.nald.blog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "nald")
public class BlogProperties {

    private long tokenExpiredTime;
    private String privateKey;
    private String defaultAccountId;
    private String defaultAccountName;
    private String defaultAccountPassword;

}
