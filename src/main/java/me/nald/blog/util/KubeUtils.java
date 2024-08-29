package me.nald.blog.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.nald.blog.util.Constants.LIST_NODE_COLUMN;

@Slf4j
public class KubeUtils {
  public static final int NODE = 0;
  public static final int NODE_USAGE = 3;
  private final List<Map<String, Object>> result;
  private final int type;
  private Map<String, List<String>> dataMap;
  private String key;

  public KubeUtils(int type, List<Map<String, Object>> result) {
    this.type = type;
    this.result = result;
  }

  public void processLine(String line) {
    if (type == NODE) {
      setDescribe(line);
    } else if (type == NODE_USAGE) {
      setListData(line);
    }
  }

  private void setDescribe(String data) {
    if (data.startsWith("Name:")) {
      dataMap = new LinkedHashMap<>();
    }
    if (dataMap != null) {
      if (!data.isEmpty()) {
        String[] splitData = data.split(":");
        if (data.charAt(0) != ' ') {
          key = splitData[0];
        }
        dataMap.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
      }
      if (data.startsWith("Events:")) {
        processEvent();
        dataMap.clear();
        dataMap = null;
      }
    }
  }

  private void setListData(String data) {
    if (!data.contains("NAME ")) {
      List<String> list = Arrays.stream(data.split(" "))
              .filter(tmp -> !tmp.isEmpty())
              .collect(Collectors.toList());

      if (list.size() == LIST_NODE_COLUMN.length) {
        Map<String, Object> dataMap = new HashMap<>();
        for (int i = 0; i < LIST_NODE_COLUMN.length; ++i) {
          dataMap.put(LIST_NODE_COLUMN[i], list.get(i));
        }
        result.add(dataMap);
      }
    }
  }

  private void processEvent() {
    Map<String, Object> detail = new LinkedHashMap<>();
    List<String> resources = List.of("cpu", "memory");

    detail.put("name", getSingleString("Name"));
    detail.put("role", getSingleString("Roles"));
    detail.putAll(getResource("Allocated resources", resources));
    detail.put("podCount", getPodCount("Non-terminated Pods"));

    String createdAt = getSingleString("CreationTimestamp");
    detail.put("age", getAge(createdAt));
    detail.put("createdAt", createdAt);
    detail.put("systemInfo", getMapData("System Info", ":"));
    detail.put("addresses", getMapData("Addresses", ":"));
    detail.put("capacity", getMapData("Capacity", ":"));

    result.add(detail);
  }

  private String getAge(String createdAt) {
    try {
      long runningTime = System.currentTimeMillis() - new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.KOREA)
              .parse(createdAt).getTime();

      if (TimeUnit.MILLISECONDS.toDays(runningTime) > 0) {
        return String.format("%dd", TimeUnit.MILLISECONDS.toDays(runningTime));
      } else if (TimeUnit.MILLISECONDS.toHours(runningTime) > 0) {
        return String.format("%dh", TimeUnit.MILLISECONDS.toHours(runningTime));
      } else if (TimeUnit.MILLISECONDS.toMinutes(runningTime) > 0) {
        return String.format("%dm", TimeUnit.MILLISECONDS.toMinutes(runningTime));
      } else {
        return String.format("%ds", TimeUnit.MILLISECONDS.toSeconds(runningTime));
      }
    } catch (Exception e) {
      log.error("Error parsing creation time", e);
      return null;
    }
  }

  private Map<String, Object> getMapData(String prefix, String separator) {
    return dataMap.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .flatMap(entry -> entry.getValue().stream())
            .map(line -> line.split(separator))
            .collect(Collectors.toMap(
                    split -> split[0].trim(),
                    split -> split.length > 1 ? split[1].trim() : ""
            ));
  }

  private Map<String, Object> getResource(String prefix, List<String> resourceTypes) {
    return resourceTypes.stream()
            .collect(Collectors.toMap(
                    resourceType -> resourceType,
                    resourceType -> dataMap.entrySet().stream()
                            .filter(entry -> entry.getKey().startsWith(prefix))
                            .flatMap(entry -> entry.getValue().stream())
                            .filter(line -> line.contains(resourceType))
                            .map(line -> line.split(":")[1].trim())
                            .findFirst()
                            .orElse("")
            ));
  }

  private int getPodCount(String prefix) {
    return dataMap.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .flatMap(entry -> entry.getValue().stream())
            .mapToInt(line -> Integer.parseInt(line.split(":")[1].trim()))
            .sum();
  }

  private String getSingleString(String key) {
    return dataMap.getOrDefault(key, List.of("")).get(0);
  }

  public static void executeCommand(String cmd, KubeUtils result) throws IOException {
    Process process = Runtime.getRuntime().exec(cmd);
    try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = br.readLine()) != null) {
        result.processLine(line);
      }
    }
  }


  public static String dataToAge(LocalDateTime date) {
    long seconds = Duration.between(date, LocalDateTime.now(ZoneId.of("Asia/Seoul"))).getSeconds();

    return switch ((seconds >= 86_400) ? 1 : (seconds >= 3_600) ? 2 : (seconds >= 60) ? 3 : 4) {
      case 1 -> "%dd".formatted(seconds / 86_400);
      case 2 -> "%dh".formatted(seconds / 3_600);
      case 3 -> "%dm".formatted(seconds / 60);
      case 4 -> "%ds".formatted(seconds);
      default -> throw new IllegalStateException("Unexpected value: " + seconds);
    };
  }
}
