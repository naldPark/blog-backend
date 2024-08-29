package me.nald.blog.data.vo;


import lombok.Getter;
@Getter
public enum Authority {
  //0 : super, 1: all, 2: buddy, 3: biz, 4: viewer
  SUPER("SUPER", "super", 0),
  ALL("ALL", "all", 1),
  BUDDY("BUDDY", "buddy", 2),
  BIZ("BIZ", "biz", 3),
  VIEWER("VIEWER", "viewer", 4);

  private String authority;
  private String category;
  private int num;

  Authority(String authority, String category, int num) {
    this.authority = authority;
    this.category = category;
    this.num = num;
  }

  public static Authority from(int ordinal) {
    switch (ordinal) {
      case 0:
        return SUPER;
      case 1:
        return ALL;
      case 2:
        return BUDDY;
      case 3:
        return BIZ;
      case 4:
      default:
        return VIEWER;
    }
  }
}