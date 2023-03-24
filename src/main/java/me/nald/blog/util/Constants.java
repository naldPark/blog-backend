package me.nald.blog.util;

import lombok.AllArgsConstructor;
import me.nald.blog.config.BlogProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

public class Constants {

    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }

    public static final String REQ_HEADER_LANG = "language";
    public static final String DEFAULT_ACCOUNT_ID = blogProperties.getDefaultAccountId();
    public static final String DEFAULT_ACCOUNT_PASSWORD = blogProperties.getDefaultAccountPassword();
    public static final String DEFAULT_ACCOUNT_NAME = blogProperties.getDefaultAccountPassword();
    public static final String DEFAULT_ACCOUNT_EMAIL = "noreply@nald.me";
    public static final String STORAGE_FILE_PATH = "/nfs/movie/";
    public static final String ANONYMOUS_YN = "isAnonymous";
    public static final String USER_ID = "user_id";
    public static final String AUTHORITY = "authority";
    public static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    public static final String LOG_ITEM = "logItem";
    public static final String LOG_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:dd";
    public static final String LOG_DELIMITER = "\t";


}
