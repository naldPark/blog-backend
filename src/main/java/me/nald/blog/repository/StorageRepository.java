package me.nald.blog.repository;


import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StorageRepository extends JpaRepository<Storage, Long> {


}
