package me.nald.blog.data.persistence.entity;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Storage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_seq")
    private Long storageId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_src")
    private String fileSrc;

    @Column(name = "file_size")
    private Long fileSize;

}

