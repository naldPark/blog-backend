package me.nald.blog.adaptor;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("kubeAdaptor")
@Slf4j
@RequiredArgsConstructor
public class KubernetesAdaptor {

  private static final String K8S_NAMESPACE_PREFIX = "namespace-prefix-";
  private static final String K8S_SANDBOX_NAMESPACE = "sandbox-namespace";
  private static final String K8S_SANDBOX_DEFAULT_LABEL = "default-label";
  private static final String K8S_IMAGE_PULL_SECRET_NAME = "image-pull-secret";
  private static final String STR_FALSE = "false";

  private static Agent defaultAgent;
  private static BlogProperties blogProperties;

  @Autowired
  public void setBlogProperties(BlogProperties blogProperties) {
    KubernetesAdaptor.blogProperties = blogProperties;
  }

  public static Agent agentWith() {
    if (defaultAgent == null) {
      try {
        ApiClient apiClient = createApiClient();
        Configuration.setDefaultApiClient(apiClient);
        defaultAgent = new Agent(apiClient);
      } catch (IOException e) {
        log.error("Failed to create API client", e);
        throw new RuntimeException(e);
      }
    }
    return defaultAgent;
  }

  private static ApiClient createApiClient() throws IOException {
    ClassPathResource k8sConfig = new ClassPathResource("k8s-config");
    InputStream configStream = k8sConfig.exists() ? k8sConfig.getInputStream()
            : Files.newInputStream(Paths.get(blogProperties.getCommonPath() + "/k8s-config.yml"));

    ApiClient apiClient = Config.fromConfig(configStream);
    apiClient.setReadTimeout(60_000);
    return apiClient;
  }

  public static String convertNamespaceName(String name) {
    return K8S_NAMESPACE_PREFIX + name;
  }

  @RequiredArgsConstructor
  public static class Agent {

    private final ApiClient apiClient;

    private CoreV1Api coreV1Api() {
      return new CoreV1Api(apiClient);
    }

    private AppsV1Api appsV1Api() {
      return new AppsV1Api(apiClient);
    }

    public List<Map<String, Object>> getNodeSummary() throws IOException {
      List<Map<String, Object>> result = new ArrayList<>();
      executeCommand("kubectl get nodes", new KubectlDetail(KubectlDetail.NODE, result));
      executeCommand("kubectl top nodes", new KubectlDetail(KubectlDetail.NODE_USAGE, result));
      return result;
    }

    public V1NodeList listNode(String fieldSelector) throws ApiException {
      return coreV1Api().listNode(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
    }

    public String createNamespacedDeployment(String name) throws ApiException {
      V1DeploymentList deployments = appsV1Api()
              .listNamespacedDeployment(K8S_SANDBOX_NAMESPACE, STR_FALSE, true, "", "", "app=" + K8S_SANDBOX_DEFAULT_LABEL, null, "", null, 0, false);
      V1Deployment defaultDeployment = deployments.getItems().stream()
              .filter(d -> d.getMetadata().getName().equals(K8S_SANDBOX_DEFAULT_LABEL))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Default deployment not found"));

      Map<String, String> label = Map.of("app", name);
      List<V1LocalObjectReference> imagePullSecrets = List.of(new V1LocalObjectReference().name(K8S_IMAGE_PULL_SECRET_NAME));
      V1PodSpec podSpec = new V1PodSpec().containers(defaultDeployment.getSpec().getTemplate().getSpec().getContainers())
              .imagePullSecrets(imagePullSecrets);
      V1DeploymentSpec deploymentSpec = new V1DeploymentSpec().replicas(1)
              .selector(new V1LabelSelector().matchLabels(label))
              .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta().labels(label)).spec(podSpec));
      V1Deployment newDeployment = new V1Deployment().kind("Deployment")
              .metadata(new V1ObjectMeta().name(name))
              .spec(deploymentSpec);

      return appsV1Api().createNamespacedDeployment(K8S_SANDBOX_NAMESPACE, newDeployment, STR_FALSE, null, null)
              .getMetadata().getName();
    }

    public String createNamespacedService(String name) throws ApiException {
      Map<String, String> selector = Map.of("app", name);
      V1ServiceSpec spec = new V1ServiceSpec().ports(List.of(new V1ServicePort().port(8088))).selector(selector).type("ClusterIP");
      V1Service service = new V1Service().kind("Service").metadata(new V1ObjectMeta().name(name)).spec(spec);

      return coreV1Api().createNamespacedService(K8S_SANDBOX_NAMESPACE, service, STR_FALSE, null, null)
              .getMetadata().getName();
    }

    public V1PodList listPodForAllNamespaces() throws ApiException {
      return coreV1Api().listPodForAllNamespaces(null, null, "", null, null, null, null, null, null, false);
    }

    public V1PodList listNamespacePod(String namespace, String labelSelector) throws ApiException {
      return coreV1Api().listNamespacedPod(convertNamespaceName(namespace), STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
    }

    private void executeCommand(String cmd, KubectlDetail result) throws IOException {
      Process process = Runtime.getRuntime().exec(cmd);
      try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
          result.processLine(line);
        }
      }
    }
  }

  static class KubectlDetail {

    public static final int NODE = 0;
    public static final int NODE_USAGE = 3;
    private final List<Map<String, Object>> result;
    private final int type;
    private Map<String, List<String>> dataMap;
    private String key;

    private static final String[] LIST_NODE_COLUMN = {"name", "usageCpu", "percentCpu", "usageMemory", "percentMemory"};

    public KubectlDetail(int type, List<Map<String, Object>> result) {
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
          List<String> dataList = dataMap.computeIfAbsent(key, k -> new ArrayList<>());
          dataList.add(data);
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
        List<String> list = getListString(data);
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
      List<String> resources = Arrays.asList("cpu", "memory");
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
        return null;
      }
    }

    private List<String> getListString(String data) {
      List<String> list = new ArrayList<>();
      String[] tmpArray = data.split(" ");
      for (String tmp : tmpArray) {
        if (!tmp.isEmpty()) {
          list.add(tmp);
        }
      }
      return list;
    }

    private String getSingleString(String keyword) {
      String result = null;
      if (dataMap.get(keyword) != null) {
        result = dataMap.get(keyword).get(0);
        if (result.indexOf(":") + 1 < result.length()) {
          result = result.substring(result.indexOf(":") + 1).trim();
        } else {
          result = null;
        }
      }
      return result;
    }

    private Map<String, String> getMapData(String keyword, String split) {
      Map<String, String> result = new LinkedHashMap<>();
      List<String> dataList = dataMap.get(keyword);
      if (dataList != null) {
        String first = getSingleString(keyword);
        if (first != null) {
          dataList.set(0, first);
        }
        for (String data : dataList) {
          String[] tmp = data.trim().split(split);
          if (tmp.length > 1) {
            result.put(tmp[0].trim(), tmp[1].trim());
          }
        }
      }
      return result;
    }

    private int getPodCount(String keyword) {
      int result = 0;
      if (dataMap.get(keyword) != null) {
        result = dataMap.get(keyword).size() - 3;
        if (result < 0) {
          result = 0;
        }
      }
      return result;
    }

    private Map<String, Object> getResource(String keyword, List<String> keys) {
      Map<String, Object> result = new LinkedHashMap<>();
      if (dataMap.get(keyword) != null) {
        List<String> dataList = dataMap.get(keyword);
        boolean isData = false;
        for (String data : dataList) {
          data = data.trim();
          if (data.contains("--------")) {
            isData = true;
            continue;
          }
          if (isData) {
            String key = data.substring(0, data.indexOf(" "));
            if (keys.contains(key)) {
              Map<String, String> map = new HashMap<>();
              map.put("requests", data.substring(data.indexOf("  "), data.indexOf(")") + 1).trim());
              map.put("limits", data.substring(data.indexOf(")") + 1).trim());
              result.put(key, map);
            }
          }
        }
      }
      return result;
    }
  }
}
