package me.nald.blog.repository;


import me.nald.blog.data.entity.AccountLog;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AccountLogRepository extends JpaRepository<AccountLog, Long> {

}
