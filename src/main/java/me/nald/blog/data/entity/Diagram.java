package me.nald.blog.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import me.nald.blog.data.vo.YN;

@Entity
@Getter
public class Diagram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagram_seq")
    private Long seq;

    @Column
    private Long parent;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length =30)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column( nullable = false, length = 1)
    private YN isGroup;

    @Column(nullable = false, length = 10)
    private String description;

    @Column(nullable = true, length = 100)
    private int groupSeq;
}