package me.nald.blog.response;

import me.nald.blog.data.vo.PageInfo;
import me.nald.blog.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResponseObject extends HashMap<String, Object> {

  public ResponseObject() {
    super();
    this.put(Constants.STATUS_CODE, 200);
  }

  public ResponseObject(List<?> list, PageInfo pageInfo) {
    this();
    Map<String, Object> map = Map.of("list", list, "pageInfo", pageInfo);
    this.put("data", map);
  }

  private Map<String, Object> toMap(JSONObject object) {
    return object.keySet().stream()
            .collect(Collectors.toMap(
                    key -> key,
                    key -> {
                      Object value = object.opt(key);
                      if (value instanceof JSONArray) {
                        return toList((JSONArray) value);
                      } else if (value instanceof JSONObject) {
                        return toMap((JSONObject) value);
                      } else {
                        return value;
                      }
                    }
            ));
  }

  private List<Object> toList(JSONArray array) {
    return IntStream.range(0, array.length())
            .mapToObj(array::opt)
            .map(value -> {
              if (value instanceof JSONArray) {
                return toList((JSONArray) value);
              } else if (value instanceof JSONObject) {
                return toMap((JSONObject) value);
              } else {
                return value;
              }
            })
            .collect(Collectors.toList());
  }

  public void putData(Object obj) {
    if (obj instanceof JSONArray) {
      this.put("data", toList((JSONArray) obj));
    } else if (obj instanceof JSONObject) {
      this.put("data", toMap((JSONObject) obj));
    } else {
      this.put("data", obj);
    }
  }

}
