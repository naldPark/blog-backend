package me.nald.blog.data.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

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

  @Column
  private String accountId;

  @Column
  private String accountName;

  private String email;

  @Embedded
  private Password password;

  //0 : active, 1: locked, 2: deleted
  @Column(nullable = false, length = 20)
  private int status;

  //0 : super, 1: all, 2: buddy, 3: biz, 4: viewer
  @Column(nullable = false, length = 50)
  private int authority;

  /** yml에 있는 physical-strategy설정에 의해 colume이름은 create_dt로 표기됨 **/
  @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  @CreationTimestamp
  private Timestamp createdDt;

  @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Timestamp recentLoginDt;

  @Column(nullable = false, length = 50)
  @ColumnDefault("0")
  private int loginFailCnt;

  @JsonIgnoreProperties("account")
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  //oneToMany는 LAZY붙이지 않아도 LAZY가 default
  private List<AccountLog> accountLogs = new ArrayList<>();

  @JsonIgnoreProperties("account")
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  //oneToMany는 LAZY붙이지 않아도 LAZY가 default
  private List<Sandbox> sandbox = new ArrayList<>();

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

