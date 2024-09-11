package me.nald.blog.response;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.nald.blog.data.vo.PageInfo;
import me.nald.blog.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResponseObject extends HashMap<String, Object> {
  private final Gson gson = new Gson();

  public ResponseObject() {
    super();
    this.put(Constants.KEY_SUCCESS, true);
    this.put(Constants.STATUS_CODE, 200);
  }

  public ResponseObject(List<?> list, PageInfo pageInfo) {
    this();
    Map<String, Object> map = Map.of("list", list, "pageInfo", pageInfo);
    this.put("data", map);
  }

  private Map<String, Object> toMap(JsonObject object) {
    return object.entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> convertJsonElement(entry.getValue())
            ));
  }
  private List<Object> toList(JsonArray array) {
    List<Object> list = new ArrayList<>();
    for (JsonElement element : array) {
      list.add(convertJsonElement(element));
    }
    return list;
  }

  private Object convertJsonElement(JsonElement element) {
    if (element.isJsonArray()) {
      return toList(element.getAsJsonArray());
    } else if (element.isJsonObject()) {
      return toMap(element.getAsJsonObject());
    } else {
      return gson.fromJson(element, Object.class);
    }
  }

  public void putData(Object obj) {
    if (obj instanceof JsonArray) {
      this.put("data", toList((JsonArray) obj));
    } else if (obj instanceof JsonObject) {
      this.put("data", toMap((JsonObject) obj));
    } else {
      this.put("data", obj);
    }
  }
}