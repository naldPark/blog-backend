package me.nald.blog.repository;


import me.nald.blog.data.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AccountRepository extends JpaRepository<Account, Long> {

    Account findByAccountId(String accountId);

}
