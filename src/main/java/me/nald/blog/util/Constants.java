package me.nald.blog.util;

import java.text.SimpleDateFormat;

public class Constants {


    public static final String ANONYMOUS_YN = "isAnonymous";
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "user_name";
    public static final String USER_EMAIL = "user_email";
    public static final String AUTHORITY = "authority";
    public static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    public static final String LOG_ITEM = "logItem";
    public static final String LOG_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:dd";
    public static final String LOG_DELIMITER = "\t";

    public static final String K8S_SANDBOX_NAMESPACE = "sandbox";

    public static final String ERROR = "error";
    public static final String STATUS_CODE = "status_code";

    public static final String K8S_NAMESPACE_PREFIX = "namespace-prefix-";
    public static final String K8S_SANDBOX_DEFAULT_LABEL = "default-label";
    public static final String K8S_IMAGE_PULL_SECRET_NAME = "image-pull-secret";

    public static final String EXCEPTION = "exception";
    public static final String[] LIST_NODE_COLUMN = {"name", "usageCpu", "percentCpu", "usageMemory", "percentMemory"};


    public static final String KEY_SUCCESS = "success";
}
