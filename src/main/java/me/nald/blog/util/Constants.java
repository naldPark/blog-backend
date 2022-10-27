package me.nald.blog.util;

import me.nald.blog.config.BlogProperties;

public class Constants {

    private static BlogProperties blogProperties;

    public static final String REQ_HEADER_LANG = "language";
    public static final String DEFAULT_ACCOUNT_ID = blogProperties.getDefaultAccountId();
    public static final String DEFAULT_ACCOUNT_PASSWORD = blogProperties.getDefaultAccountPassword();
    public static final String DEFAULT_ACCOUNT_NAME = blogProperties.getDefaultAccountPassword();
    public static final String DEFAULT_ACCOUNT_EMAIL = "noreply@nald.me";


}
