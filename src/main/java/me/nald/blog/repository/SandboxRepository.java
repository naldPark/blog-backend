package me.nald.blog.repository;


import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.Sandbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SandboxRepository extends JpaRepository<Sandbox, Long> {

    List<Sandbox> findByAccount(Account account);

}
