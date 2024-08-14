package me.nald.blog.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CommonUtils {

  private static final Gson gson = new Gson();

  public static List<HashMap<String, Object>> stringListToHashMapList(List<String> list) {
    return list.stream()
            .map(jsonString -> gson.fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
            }))
            .collect(Collectors.toList());
  }

}
