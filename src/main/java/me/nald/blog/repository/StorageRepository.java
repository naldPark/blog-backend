package me.nald.blog.repository;


import me.nald.blog.data.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface StorageRepository extends JpaRepository<Storage, Long> {


  List<Storage> findAllByStorageIdIn(List<Long> idList);
}