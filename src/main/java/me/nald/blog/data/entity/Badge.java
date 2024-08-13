package me.nald.blog.data.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Badge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "badge_seq")
  private Long seq;

  @Column(nullable = false, length = 20)
  private String name;

  @Column(nullable = false, length = 10)
  private String color;

  @Column(nullable = false, length = 50)
  private String src;

  @Column(nullable = false, length = 10)
  private String backgroundColor;

}
