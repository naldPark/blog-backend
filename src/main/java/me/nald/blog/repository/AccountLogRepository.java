package me.nald.blog.repository;


import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.AccountLog;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AccountLogRepository extends JpaRepository<AccountLog, Long> {

}
