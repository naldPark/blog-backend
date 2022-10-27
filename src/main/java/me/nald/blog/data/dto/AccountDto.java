package me.nald.blog.data.dto;

import lombok.*;
import java.util.Map;

public class AccountDto {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class getUserList {

        int statusCode;
        Map<String, Object> data;

        @Builder
        public getUserList(int statusCode, Map<String, Object> list) {
            this.statusCode = statusCode;
            this.data = list;
        }
    }

    @Getter
    @Setter
    public static class LoginInfo{
        private String accountId;
        private String accountName;
        private String password;
        private int authority;

    }

}
