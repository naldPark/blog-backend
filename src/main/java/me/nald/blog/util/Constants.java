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
    public static final String USER_NAME = "user_name";
    public static final String USER_EMAIL = "user_email";
    public static final String AUTHORITY = "authority";
    public static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    public static final String LOG_ITEM = "logItem";
    public static final String LOG_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:dd";
    public static final String LOG_DELIMITER = "\t";

    // K8S Config
    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";
    public static final String K8S_SANDBOX_NAMESPACE = "sandbox";
    public static final String K8S_SANDBOX_DEFAULT_LABEL = "default-sandbox";
    public static final String K8S_IMAGE_PULL_SECRET_NAME = "regcred";
    public static final String K8S_CPU_NODE_SERVICE_TYPE = "cpu";
    public static final String K8S_GPU_NODE_SERVICE_TYPE = "gpu";
    public static final String K8S_NAMESPACE_PREFIX = "";
    public static final String K8S_LABEL_KEY_SERVICE_TYPE = "servicetype";
    public static final String K8S_RESOURCE_TYPE_CPU = "cpu";
    public static final String K8S_RESOURCE_TYPE_MEMORY = "memory";
    public static final String K8S_EXTENDED_RESOURCE_TYPE_GPU = "gpu";
    public static final String K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU = "nvidia.com/gpu";
    public static final String K8S_RESOURCE_REQUESTS = "requests";
    public static final String RESOURCE_QUOTA_NAME = "resource-quota-limit-mem-cpu";
    public static final String RESOURCE_QUOTA_FIELD_SELECTOR  = "metadata.name=" + RESOURCE_QUOTA_NAME;
    public static final String KUBE_CONFIG_WINDOWS_PATH = "..\\.kube2";
    public static final String KUBE_CONFIG_LINUX_PATH = "/tmp/.kube";


}
