package me.nald.blog.data.persistence.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "account_log")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_seq")
    private Long id;

    @Column(name = "ipAddr")
    private String ipAddr;

    @Column(name = "created_dt", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private Timestamp createdDt;

    @JsonBackReference
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    public static AccountLog createLog(Account account, String ipAddr){
        AccountLog accountLog =new AccountLog();
        accountLog.setAccount(account);
        accountLog.setIpAddr(ipAddr);

        return accountLog;
    }


}
