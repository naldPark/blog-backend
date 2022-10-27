package me.nald.blog.data.persistence.entity;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

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

    @Column(name = "authority", nullable = false, length = 50)
    private int authority;

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

