package me.nald.blog.data.dto;

import lombok.*;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Storage;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountDto {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class getUserList {

        int statusCode;
        List<UserInfo> list = new ArrayList<>();
        int total;

        @Builder
        public getUserList(int statusCode, List<UserInfo> list, int total) {
            this.list = list;
            this.total = total;
        }
    }

    @Getter
    @Setter
    public static class UserInfo{
        private String accountId;
        private String accountName;
        private int authority;
        private String recentLoginDt;
        private int status;
        private String email;
        private String createdDt;
        private int loginFailCnt;

        public UserInfo(Account account) {
            SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd hh:mm");
            accountId = account.getAccountId();
            accountName = account.getAccountName();
            authority = account.getAuthority();
            recentLoginDt = sdf.format(account.getCreatedDt());
            status = account.getStatus();
            email = account.getEmail();
            createdDt = sdf.format(account.getCreatedDt());
            loginFailCnt = account.getAuthority();
        }

    }

}
