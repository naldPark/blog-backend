package me.nald.blog.util;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.StringPath;

import java.util.Objects;

public class BooleanBuilderUtils {

  /**
   * @param <T>     제네릭 타입, 엔티티 타입.
   * @param builder BooleanBuilder 객체.
   * @param search  검색어.
   * @param field   entity의 StringPath 객체.
   * */
  public static <T> void checkContainCondition(BooleanBuilder builder, String search, StringPath field) {
    if (Objects.nonNull(search) && !search.isEmpty()) {
      builder.and(field.contains(search));
    }
  }

  public static <T> void checkEqualCondition(BooleanBuilder builder, String search, StringPath field) {
    if (Objects.nonNull(search) && !search.isEmpty()) {
      builder.and(field.eq(search));
    }
  }

}
