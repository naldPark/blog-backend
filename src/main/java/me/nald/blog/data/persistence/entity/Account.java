package me.nald.blog.data.persistence.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_seq")
    private Long seq;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "account_name")
    private String accountName;

    private String email;

    @Embedded
    private Password password;

    @Column(name = "status", nullable = false, length = 20)
    private int status;

    //0 : super, 1: all, 2: buddy, 3: biz, 4: viewer
    @Column(name = "authority", nullable = false, length = 50)
    private int authority;

    @Column(name = "created_dt", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private Timestamp createdDt;

    @Column(name = "recent_login_dt", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp recentLoginDt;

    @Column(name = "login_fail_cnt", nullable = false, length = 50)
    @ColumnDefault("0")
    private int loginFailCnt;

    @JsonIgnoreProperties("account")
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<AccountLog> accountLogs = new ArrayList<>();

    public static Account createAccount(String accountId,
                                        String accountName,
                                        String email,
                                        int authority,
                                        Password password,
                                        int status) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setAccountName(accountName);
        account.setEmail(email);
        account.setPassword(password);
        account.setStatus(status);
        account.setAuthority(authority);
        return account;
    }


}

