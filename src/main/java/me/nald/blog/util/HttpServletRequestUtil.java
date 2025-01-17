package me.nald.blog.util;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpServletRequestUtil {

  public static <T> T extractFromQueryParam(HttpServletRequest request, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Map<String, String[]> paramMap = request.getParameterMap();

    T t = clazz.getConstructor().newInstance();

    for (Field f : clazz.getFields()) {
      f.setAccessible(true);
      String[] paramValues = paramMap.get(f.getName());

      if (Objects.isNull(paramValues) || paramValues.length == 0) {
        continue;
      }

      f.set(t, f.getType().isArray() ? Stream.of(paramValues).map(f.getType()::cast).toArray() : f.getType().cast(paramValues[0]));

    }

    return t;
  }


  public static <T> T extractFromHeader(HttpServletRequest request, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

    T t = clazz.getConstructor().newInstance();

    for (Field f : clazz.getFields()) {
      f.setAccessible(true);
      String headerValue = request.getHeader(f.getName());

      if (StringUtils.isEmpty(headerValue)) {
        continue;
      }

      f.set(t, headerValue);
    }

    return t;
  }


  public static String getBody(HttpServletRequest request) throws IOException {
    return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
  }

  public static String getQueryParamsAsString(HttpServletRequest request) {
    return Optional.ofNullable(request)
            .map(req -> req.getParameterMap().keySet()
                    .stream()
                    .map(key -> Stream.of(request.getParameterValues(key))
                            .map(value -> key + "=" + value)
                            .collect(Collectors.joining("&"))
                    ).collect(Collectors.joining("&"))
            )
            .orElse("");
  }

  public static String getRemoteIP(HttpServletRequest request) {
    String ip = request.getHeader("X-FORWARDED-FOR");

    System.out.println("ip1+"+ip);

    //proxy 환경일 경우
    if (ip == null || ip.isEmpty()) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    System.out.println("ip2+"+ip);
    //웹로직 서버일 경우
    if (ip == null || ip.isEmpty()) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }

    System.out.println("ip3+"+ip);
    if (ip == null || ip.isEmpty()) {
      ip = request.getRemoteAddr();
    }

    System.out.println("ip4+"+ip);
    return ip;
  }
}
