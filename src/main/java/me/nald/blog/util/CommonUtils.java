package me.nald.blog.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
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


  // 한글을 발음대로 영어로 변환하는 메서드
  public static String convertKoreanToEnglish(String fileName, HashMap<String, String> phoneticSymbol) {
    var nameToEng = new StringBuilder();
    fileName.chars()
            .mapToObj(c -> String.valueOf((char) c))
            .forEach(fileChar -> {
              if (Pattern.matches("^[0-9a-zA-Z]*$", fileChar)) {
                nameToEng.append(fileChar);
              } else if (phoneticSymbol.get(fileChar) != null) {
                nameToEng.append(phoneticSymbol.get(fileChar));
              }
            });
    return nameToEng.length() > 20 ? nameToEng.substring(0, 20) : nameToEng.toString();
  }

}
