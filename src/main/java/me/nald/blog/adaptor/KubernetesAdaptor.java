package me.nald.blog.adaptor;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.model.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.error.YAMLException;

import static me.nald.blog.util.Constants.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("kubeAdaptor")
@Slf4j
@RequiredArgsConstructor
public class KubernetesAdaptor {

    static final int CONVERT_TYPE_BYTE = 0;
    static final int CONVERT_TYPE_CORE = 1;

    private static Agent defaultAgent;
    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }


    @Value("${docker.endpoint}")
    private String dockerEndpoint;
    @Value("${docker.username}")
    private String dockerUsername;
    @Value("${docker.secret-password}")
    private String dockerSecretPassword;
    @Value("${docker.email}")
    private String dockerEmail;

    //agentwith가 3가지로 주입가능한데 나는 그냥 resource하위에있는 config로 불러오기
    public static Agent agentWith(String... k8sConfig) {
        if (Objects.nonNull(k8sConfig) && k8sConfig.length > 0 && !StringUtils.isEmpty(k8sConfig[0])) {
            defaultAgent = new Agent(k8sConfig[0], null);
            return defaultAgent;
        } else {
            return agentWith();
        }
    }

    public static Agent agentWith(Cluster cluster) {
        return Optional.ofNullable(cluster)
                .map(Cluster::getK8sConfig)
                .map(KubernetesAdaptor::agentWith)
                .orElse(KubernetesAdaptor.agentWith());
    }

    public static Agent agentWith() {
        ApiClient apiClient = null;
        if (defaultAgent == null) {
            try {
                ClassPathResource k8sConfig = new ClassPathResource("k8s-config");
                if (k8sConfig.exists()) {
                    apiClient = Config.fromConfig(new ClassPathResource("k8s-config").getInputStream());
                } else {
                    Path configPath = Paths.get(blogProperties.getCommonPath() + "/k8s-config.yml");
                    apiClient = Config.fromConfig(Files.newInputStream(configPath));
                }
                apiClient.setReadTimeout(60 * 1000);
                Configuration.setDefaultApiClient(apiClient);
                defaultAgent = new Agent("", apiClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultAgent;
    }

    public static String convertNamespaceName(String name) {
        return K8S_NAMESPACE_PREFIX + name;
    }


    private static void cmdResult(String cmd, KubectlDetail result) throws Exception {
//        System.out.println("완성된 cmd는"+ cmd);
        Process process = Runtime.getRuntime().exec(cmd);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = null;
            while ((line = br.readLine()) != null) {
//                System.out.println("★★★"+line);
                switch (result.getType()) {
                    case KubectlDetail.NODE:
                    case KubectlDetail.POD:
                    case KubectlDetail.NODE_SUMMARY:
                        result.setDescribe(line);
                        break;
                    case KubectlDetail.NODE_USAGE:
                        result.setListData(line);
                        break;
                }
            }
        }
    }


    private static long convertToLong(String value, int type) {
        long result = 0;
        try {
            switch (type) {
                case CONVERT_TYPE_BYTE:
                    if (value.contains("i")) {
                        String unit = value.substring(value.length() - 2);
                        result = Integer.parseInt(value.substring(0, value.length() - 2));
                        switch (unit) {
                            case "Ei":
                                result *= 1024;
                            case "Pi":
                                result *= 1024;
                            case "Ti":
                                result *= 1024;
                            case "Gi":
                                result *= 1024;
                            case "Mi":
                                result *= 1024;
                            case "Ki":
                                result *= 1024;
                                break;
                        }
                    }
                    break;
                case CONVERT_TYPE_CORE:
                    try {
                        result = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        result = Integer.parseInt(value.substring(0, value.length() - 1));
                    }
                    break;
            }
        } catch (Exception e) {
            result = 0;
        }
        return result;
    }


    @Data
    static class KubectlDetail {

        public static final int NODE = 0;
        public static final int POD = 1;
        public static final int NODE_SUMMARY = 2;
        public static final int NODE_USAGE = 3;

        final String[] LIST_NODE_COLUMN = {"name", "usageCpu", "percentCpu", "usageMemory", "percentMemory"};

        Map<String, List<String>> dataMap;
        String key;
        int type = -1;
        List<Map<String, Object>> list = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

        Map<String, Object> summaryCpu = new HashMap<>();
        Map<String, Object> summaryMemory = new HashMap<>();
        Map<String, Object> summaryPod = new HashMap<>();

        String namespace;

        KubectlDetail(int type) {
            this.type = type;
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            if (type == NODE_SUMMARY) {
                summaryCpu.put("usage", new Long(0));
                summaryCpu.put("requests", new Long(0));
                summaryCpu.put("limits", new Long(0));
                summaryCpu.put("capacity", new Long(0));
                summaryMemory.put("usage", new Long(0));
                summaryMemory.put("requests", new Long(0));
                summaryMemory.put("limits", new Long(0));
                summaryMemory.put("capacity", new Long(0));
                summaryPod.put("usage", new Long(0));
                summaryPod.put("capacity", new Long(0));
            }
        }


        public void setListData(String data) {
            if (data.indexOf("NAME ") == -1) {
                Map<String, Object> dataMap = new HashMap<>();
                List<String> list = getListString(data);
                String[] column = null;
                switch (type) {
                    case NODE_USAGE:
                        column = LIST_NODE_COLUMN;
                        break;
                }
                if (column != null && list.size() == column.length) {
                    for (int i = 0; i < column.length; ++i) {
                        dataMap.put(column[i], list.get(i));
                    }
                    this.list.add(dataMap);
                }
            }
        }

        public void setDescribe(String data) {

            if (data.indexOf("Name:") == 0) {
                dataMap = new LinkedHashMap<>();
            }

            if (dataMap != null) {
                if (data.length() > 0) {
                    String[] splitData = data.split(":");
                    if (data.charAt(0) != ' ') {
                        key = splitData[0];
                    }
                    List<String> dataList = dataMap.get(key);
                    if (dataList == null) {
                        dataList = new ArrayList<>();
                        dataMap.put(key, dataList);
                    }
                    dataList.add(data);
                }

                if (data.indexOf("Events:") == 0) {
                    String createdAt = null;
                    Map<String, Object> detail = new LinkedHashMap<>();
                    List<String> resources = Arrays.asList("cpu", "memory");
                    switch (type) {
                        case NODE:
                            System.out.println("안눙");
                            detail.put("name", getSingleString("Name"));
                            detail.put("role", getSingleString("Roles"));
                            detail.putAll(getResource("Allocated resources", resources));
                            detail.put("podCount", getPodCount("Non-terminated Pods"));
                            createdAt = getSingleString("CreationTimestamp");
                            detail.put("age", getAge(createdAt));
                            detail.put("createdAt", createdAt);
                            detail.put("systemInfo", getMapData("System Info", ":"));
                            detail.put("addresses", getMapData("Addresses", ":"));
                            detail.put("capacity", getMapData("Capacity", ":"));
                            list.add(detail);
                            break;
                        case POD:
                            List<Map<String, Object>> containers = getContainers("Containers");
                            int restartCount = 0;
                            int readyCount = 0;
                            String reason = null;
                            for (Map<String, Object> container : containers) {
                                restartCount += (int) container.get("restartCount");
                                if ((boolean) container.get("ready")) {
                                    readyCount++;
                                }
                                if (reason == null && !"Running".equals(container.get("state"))) {
                                    Map<String, String> subData = (Map) container.get("stateData");
                                    if (subData != null) {
                                        reason = subData.get("Reason");
                                    }
                                }
                            }
                            detail.put("name", getSingleString("Name"));
                            if (reason != null) {
                                detail.put("status", reason);
                            } else {
                                detail.put("status", getSingleString("Status"));
                            }
                            detail.put("ready", String.format("%d/%d", readyCount, containers.size()));
                            detail.put("restartCount", restartCount);
                            detail.put("namespace", getSingleString("Namespace"));
                            createdAt = getSingleString("Start Time");
                            detail.put("age", getAge(createdAt));
                            detail.put("node", getSingleString("Node"));
                            detail.put("createdAt", createdAt);
                            detail.put("label", getMapData("Labels", "="));
                            detail.put("ip", getSingleString("IP"));
                            detail.put("controllerBy", getSingleString("Controlled By"));
                            detail.put("conditions", getMapDataSpace("Conditions"));
                            detail.put("containers", containers);
                            list.add(detail);
                            break;
                        case NODE_SUMMARY:
                            summaryPod.put("usage", (long) summaryPod.get("usage") + getPodCount("Non-terminated Pods"));
                            Map<String, String> capacity = getMapData("Capacity", ":");
                            summaryCpu.put("capacity", (long) summaryCpu.get("capacity") + convertToLong(String.valueOf(capacity.get("cpu")), CONVERT_TYPE_CORE));
                            summaryMemory.put("capacity", (long) summaryMemory.get("capacity") + convertToLong(String.valueOf(capacity.get("memory")), CONVERT_TYPE_BYTE));
                            summaryPod.put("capacity", (long) summaryPod.get("capacity") + Integer.parseInt(String.valueOf(capacity.get("pods"))));
                            Map<String, Object> resourceMap = getResource("Allocated resources", resources);
                            Map<String, Object> cpuMap = (Map<String, Object>) resourceMap.get("cpu");
                            summaryCpu.put("requests", (long) summaryCpu.get("requests") + convertToLong(String.valueOf(cpuMap.get("requests")).split(" ")[0], CONVERT_TYPE_CORE));
                            summaryCpu.put("limits", (long) summaryCpu.get("limits") + convertToLong(String.valueOf(cpuMap.get("limits")).split(" ")[0], CONVERT_TYPE_CORE));
                            Map<String, Object> memoryMap = (Map<String, Object>) resourceMap.get("memory");
                            summaryMemory.put("requests", (long) summaryMemory.get("requests") + convertToLong(String.valueOf(memoryMap.get("requests")).split(" ")[0], CONVERT_TYPE_BYTE));
                            summaryMemory.put("limits", (long) summaryMemory.get("limits") + convertToLong(String.valueOf(memoryMap.get("limits")).split(" ")[0], CONVERT_TYPE_BYTE));
                            break;
                        default:
                            break;
                    }
                    dataMap.clear();
                    dataMap = null;
                }
            }
        }

        private String getAge(String createdAt) {
            String result = null;
            long runningTime = 0;
            try {
                runningTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - format.parse(createdAt).getTime();
            } catch (Exception e) {
                runningTime = 0;
            }
            if (TimeUnit.MILLISECONDS.toDays(runningTime) > 0) {
                result = String.format("%dd", TimeUnit.MILLISECONDS.toDays(runningTime));
            } else if (TimeUnit.MILLISECONDS.toHours(runningTime) > 0) {
                result = String.format("%dh", TimeUnit.MILLISECONDS.toHours(runningTime));
            } else if (TimeUnit.MILLISECONDS.toMinutes(runningTime) > 0) {
                result = String.format("%dm", TimeUnit.MILLISECONDS.toMinutes(runningTime));
            } else if (TimeUnit.MILLISECONDS.toSeconds(runningTime) > 0) {
                result = String.format("%ds", TimeUnit.MILLISECONDS.toSeconds(runningTime));
            }
            return result;
        }

        private List<Map<String, Object>> getContainers(String Keyword) {
            List<Map<String, Object>> result = new ArrayList<>();
            List<String> dataList = dataMap.get(Keyword);
            dataList.remove(0);
            List<Map<String, Object>> containers = new ArrayList<>();
            Map<String, Object> container = null;
            String key = null;
            String lastSubKey = null;
            for (String str : dataList) {
                if (str.charAt(2) != ' ' && str.indexOf(":") + 1 == str.length()) {
                    container = new LinkedHashMap<>();
                    containers.add(container);
                    container.put("Name", str.substring(0, str.indexOf(":")).trim());
                } else if (container != null) {
                    if (str.length() > 5 && str.charAt(4) != ' ') {
                        lastSubKey = null;
                        if (str.indexOf(":") + 1 == str.length()) {
                            key = str.substring(0, str.indexOf(":")).trim();
                            continue;
                        } else {
                            key = null;
                            lastSubKey = str.substring(0, str.indexOf(":")).trim();
                            container.put(lastSubKey, str.substring(str.indexOf(":") + 1).trim());
                        }
                    } else if (str.length() > 7 && str.charAt(6) != ' ' && lastSubKey != null && str.contains(":")) {
                        Map<String, String> subData = (Map) container.get(String.format("%s_sub", lastSubKey));
                        if (subData == null) {
                            subData = new LinkedHashMap<>();
                            container.put(String.format("%s_sub", lastSubKey), subData);
                        }
                        subData.put(str.substring(0, str.indexOf(":")).trim(), str.substring(str.indexOf(":") + 1).trim());
                    }
                    if (key != null && str.contains(":")) {
                        Map<String, Object> data = (Map) container.get(key);
                        if (data == null) {
                            data = new LinkedHashMap<>();
                            container.put(key, data);
                        }
                        data.put(str.substring(0, str.indexOf(":")).trim(), str.substring(str.indexOf(":") + 1).trim());
                    }
                }
            }

            for (Map<String, Object> con : containers) {
                Map<String, Object> map = new LinkedHashMap<>();
                result.add(map);
                map.put("name", con.get("Name"));
                map.put("image", con.get("Image"));
                if (con.get("Ports") != null) {
                    map.put("ports", con.get("Ports"));
                } else {
                    map.put("ports", con.get("Port"));
                }
                map.put("ready", Boolean.parseBoolean(String.valueOf(con.get("Ready"))));
                try {
                    map.put("restartCount", Integer.parseInt(String.valueOf(con.get("Restart Count"))));
                } catch (NumberFormatException e) {
                    map.put("restartCount", 0);
                }
                map.put("environment", con.get("Environment"));
                map.put("state", con.get("State"));
                map.put("stateData", con.get("State_sub"));
            }

            return result;
        }

        private List<String> getListString(String data) {
            List<String> list = new ArrayList<>();
            String tmpArray[] = data.split(" ");
            for (String tmp : tmpArray) {
                if (tmp.length() > 0) {
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

        private Map<String, String> getMapDataSpace(String keyword) {
            Map<String, String> result = new LinkedHashMap<>();
            List<String> dataList = dataMap.get(keyword);
            if (dataList != null) {
                String first = getSingleString(keyword);
                if (first != null) {
                    dataList.set(0, first);
                }
                for (String data : dataList) {
                    data = data.trim();
                    if (data.indexOf(" ") != -1) {
                        result.put(data.substring(0, data.indexOf(" ")), data.substring(data.indexOf(" ")).trim());
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
                    if (data.indexOf("--------") != -1) {
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

        public Map<String, Object> getSummary() {
            Map<String, Object> result = new HashMap<>();
            summaryCpu.put("capacity", (long) summaryCpu.get("capacity") * 1000);
            result.put("cpu", summaryCpu);
            result.put("memory", summaryMemory);
            result.put("pod", summaryPod);
            return result;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Agent {
        private final String k8sConfig;
        private final ApiClient defaultApiClient;

        private ApiClient apiClient() {
            if (Objects.nonNull(defaultApiClient)) {
                return defaultApiClient;
            } else {
                ApiClient client;
                KubeConfig kubeConfig;
                try (InputStream inputStream = new ByteArrayInputStream(k8sConfig.getBytes())) {
                    kubeConfig = KubeConfig.loadKubeConfig(new InputStreamReader(inputStream, StandardCharsets.UTF_8.name()));
                } catch (IOException | YAMLException | ClassCastException e) {
                    log.error("apiClient", e);
                    throw new RuntimeException(e);
                }
                try {
                    client = Config.fromConfig(kubeConfig);
                } catch (IOException | RuntimeException e) {
                    log.error("apiClient", e);
                    throw new RuntimeException(e);
                }
                client.setReadTimeout(60 * 1000);
                return client;
            }
        }

        private CoreV1Api coreV1Api() {
            return new CoreV1Api(apiClient());
        }

        private AppsV1Api appsV1Api() {
            return new AppsV1Api(apiClient());
        }


        public List<Map<String, Object>> getNodeSummary() throws Exception {
            String cmd = String.format("kubectl get nodes");
            KubectlDetail summary = new KubectlDetail(KubectlDetail.NODE);
            cmdResult(cmd, summary);
            cmd = String.format("kubectl top nodes");
            KubectlDetail usage = new KubectlDetail(KubectlDetail.NODE_USAGE);
            cmdResult(cmd, usage);
            List<Map<String, Object>> usageList = usage.getList();
            return usageList;
        }


        public V1NodeList listNode(String fieldSelector) throws ApiException {
            return coreV1Api().listNode(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
        }


        public String createNamespacedDeployment(String name) throws ApiException {
            System.out.println("이름은="+name);
            V1DeploymentList deployments = appsV1Api()
                    .listNamespacedDeployment(K8S_SANDBOX_NAMESPACE, "false", true, "", "", "app=" + K8S_SANDBOX_DEFAULT_LABEL, null, "", null, 0, false);
            V1Deployment defaultDeployment = deployments.getItems().stream().filter(f -> f.getMetadata().getName().equals(K8S_SANDBOX_DEFAULT_LABEL)).findAny().get();
            Map<String, String> label = new HashMap<String, String>() {{
                put("app", name);
            }};
            List<V1LocalObjectReference> imagePullSecrets = new ArrayList<>();
            imagePullSecrets.add(new V1LocalObjectReference().name(K8S_IMAGE_PULL_SECRET_NAME));
            V1PodSpec podSpec = new V1PodSpec().containers(defaultDeployment.getSpec().getTemplate().getSpec().getContainers()).imagePullSecrets(imagePullSecrets);
            V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
                    .replicas(1)
                    .selector(new V1LabelSelector().matchLabels(label))
                    .template(new V1PodTemplateSpec()
                            .metadata(new V1ObjectMeta().labels(label)).spec(podSpec));
            V1Deployment newDeployment = new V1Deployment()
                    .kind("Deployment").metadata(new V1ObjectMeta().name(name)).spec(deploymentSpec);

            V1Deployment result = appsV1Api().createNamespacedDeployment(K8S_SANDBOX_NAMESPACE, newDeployment, "false", null, null);

            return result.getMetadata().getName();
        }

        public String createNamespacedService(String name) throws ApiException {
//            V1ObjectMeta meta = new V1ObjectMeta().name(name).labels(metaLabels);

            Map<String, String> selector = new HashMap<>();
            selector.put("app", name);

//            Cluster cluster = namespace.getCluster();
            V1ServiceSpec spec = new V1ServiceSpec().ports(Arrays.asList(new V1ServicePort().port(8088))).selector(selector).type("ClusterIP");
            V1Service body = new V1Service().kind("Service").metadata(new V1ObjectMeta().name(name)).spec(spec);

            V1Service result = coreV1Api().createNamespacedService(K8S_SANDBOX_NAMESPACE, body, "false", null, null);

            return result.getMetadata().getName();
        }
        public V1NamespaceList listNamespace(String fieldSelector) throws ApiException {
            return coreV1Api().listNamespace(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
        }

        public V1PodList listPodForAllNamespaces() throws ApiException {
            return coreV1Api().listPodForAllNamespaces(null, null, "", null, null, null, null, null, null, false);
        }

        public V1PodList listNamespacePod(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedPod(convertNamespaceName(namespace), STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public Map<String, List<Float>> getClusterResource() throws ApiException, IOException {
            HashMap<String, List<Float>> result = new HashMap<>();

            V1NodeList nodeList = KubernetesAdaptor.agentWith().listNode("");
//            System.out.println("노드리스트 ="+nodeList);
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1
                        && (node.getMetadata().getLabels().containsKey(K8S_LABEL_KEY_SERVICE_TYPE)
                        && (node.getMetadata().getLabels().get(K8S_LABEL_KEY_SERVICE_TYPE).equals(K8S_CPU_NODE_SERVICE_TYPE)
                        || node.getMetadata().getLabels().get(K8S_LABEL_KEY_SERVICE_TYPE).equals(K8S_GPU_NODE_SERVICE_TYPE)))) {
                    List<Float> value = getAllResourceNode(node.getMetadata().getName(), 0);
                    result.put(node.getMetadata().getName(), value);
                }
            }
            return result;
        }

        public Map<String, List<Float>> getClusterResource(Cluster cluster) throws ApiException, IOException {
            long cId = cluster.getId();
            checkKubeconfigFile(cluster);
            HashMap<String, List<Float>> result = new HashMap<>();

            V1NodeList nodeList = KubernetesAdaptor.agentWith(cluster).listNode("");
//            System.out.println("노드리스트 ="+nodeList);
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1
                        && (node.getMetadata().getLabels().containsKey(K8S_LABEL_KEY_SERVICE_TYPE)
                        && (node.getMetadata().getLabels().get(K8S_LABEL_KEY_SERVICE_TYPE).equals(K8S_CPU_NODE_SERVICE_TYPE)
                        || node.getMetadata().getLabels().get(K8S_LABEL_KEY_SERVICE_TYPE).equals(K8S_GPU_NODE_SERVICE_TYPE)))) {
                    List<Float> value = getAllResourceNode(node.getMetadata().getName(), cId);
                    result.put(node.getMetadata().getName(), value);
                }
            }
            return result;
        }


        private String addDataToDataMap(String line, Map<String, List<String>> dataMap, String key) {
            String[] splitData = line.split(":");
            if (line.charAt(0) != ' ') {
                key = splitData[0];
            }
            List<String> dataList = dataMap.computeIfAbsent(key, k -> new ArrayList<>());
            dataList.add(line);
            return key;
        }

        private List<Float> getAllResourceNode(String nodeName, long cId) throws IOException {
            List<Float> result = new ArrayList<>();
            Map<String, List<String>> dataMap = new LinkedHashMap<>();
            String key = null;
            String fileName = getKubeConfigFileName(getKubeConfigFilePath(), cId);

            String cmd = String.format("kubectl describe node %s --kubeconfig=%s", nodeName, fileName);
            Process process = Runtime.getRuntime().exec(cmd);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("name") == 0) {
                        dataMap = new LinkedHashMap<>();
                    }
                    if (dataMap != null) {
                        if (line.length() > 0) {
                            key = addDataToDataMap(line, dataMap, key);
                        }
                        if (line.indexOf("Events:") == 0) {
                            List<Float> eventResult = processEvents(dataMap);
                            result.addAll(eventResult);
                            dataMap.clear();
                            dataMap = null;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        private List<Float> processEvents(Map<String, List<String>> dataMap) {
            List<Float> result = new ArrayList<>();
            List<String> resource = Arrays.asList(K8S_RESOURCE_TYPE_CPU, K8S_RESOURCE_TYPE_MEMORY, K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
            Map<String, String> capacity = getMapData("Allocatable", ":", dataMap);
            Map<String, Map<String, String>> allocated = getUsedData("Allocated resources", resource, dataMap);
            Map<String, String> cpuUsed = allocated.get(K8S_RESOURCE_TYPE_CPU);
            Map<String, String> memoryUsed = allocated.get(K8S_RESOURCE_TYPE_MEMORY);
            Map<String, String> gpuUsed = allocated.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
            result.add(convertToFloat(String.valueOf(cpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], true, false));
            result.add(convertToFloat(String.valueOf(capacity.get(K8S_RESOURCE_TYPE_CPU)), true, false));
            result.add(convertToFloat(String.valueOf(memoryUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], false, false));
            result.add(convertToFloat(String.valueOf(capacity.get(K8S_RESOURCE_TYPE_MEMORY)), false, false));
            if (gpuUsed != null) {
                result.add(Float.parseFloat(String.valueOf(gpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0].substring(0, 1)));
                result.add(convertToFloat(String.valueOf(capacity.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU)), true, false));
            } else {
                result.add(0f);
                result.add(0f);
            }
            return result;
        }

        private Map<String, String> getMapData(String keyword, String split, Map<String, List<String>> dataMap) {
            Map<String, String> result = new LinkedHashMap<>();
            List<String> dataList = dataMap.get(keyword);
            if (dataList != null) {
                String first = dataList.get(0);
                if (first.indexOf(":") + 1 < first.length()) {
                    first = first.substring(first.indexOf(":") + 1).trim();
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

        private Map<String, Map<String, String>> getUsedData(String keyword, List<String> keys, Map<String, List<String>> dataMap) {
            Map<String, Map<String, String>> result = new LinkedHashMap<>();
            if (dataMap.get(keyword) != null) {
                List<String> dataList = dataMap.get(keyword);
                boolean isData = false;
                for (String data : dataList) {
                    data = data.trim();
                    if (data.indexOf("--------") != -1) {
                        isData = true;
                        continue;
                    }
                    if (isData) {
                        String key = data.substring(0, data.indexOf(" "));
                        Map<String, String> map = processData(keys, key, data);
                        if (map != null) {
                            result.put(key, map);
                        }
                    }
                }
            }
            return result;
        }

        private Map<String, String> processData(List<String> keys, String key, String data) {
            Map<String, String> map = null;
            if (keys.contains(key) && (key.equals(K8S_RESOURCE_TYPE_CPU) || key.equals(K8S_RESOURCE_TYPE_MEMORY))) {
                map = new HashMap<>();
                map.put(K8S_RESOURCE_REQUESTS, data.substring(data.indexOf("   "), data.indexOf("(")).trim());
            } else if (keys.contains(key) && key.equals(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU)) {
                map = new HashMap<>();
                map.put(K8S_RESOURCE_REQUESTS, data.substring(data.indexOf("  ")).trim());
            }
            return map;
        }

        private static float convertToFloat(String value, boolean isCore, boolean isbyte) {
            if (isCore) {
                return convertToFloatForCore(value);
            } else {
                return convertToFloatForNoCore(value, isbyte);
            }
        }

        private static float convertToFloatForCore(String value) {
            String unit = value.substring(value.length() - 1);
            if (unit.equals("m")) {
                float result = Integer.parseInt(value.substring(0, value.length() - 1));
                return result / 1024;
            } else {
                return Integer.parseInt(value);
            }
        }

        private static float convertToFloatForNoCore(String value, boolean isbyte) {
            float result;
            if (value.contains("i")) {
                String unit = value.substring(value.length() - 2);
                result = Integer.parseInt(value.substring(0, value.length() - 2));
                if (isbyte) {
                    switch (unit) {
                        case "Gi":
                            result = result * 1024 * 1024 * 1024;
                            break;
                        case "Mi":
                            result = result * 1024 * 1024;
                            break;
                        case "Ki":
                            result = result * 1024;
                            break;
                        default:
                    }
                } else {
                    switch (unit) {
                        case "Gi":
                            break;
                        case "Mi":
                            result = result / 1024;
                            break;
                        case "Ki":
                            result = result / 1024 / 1024;
                            break;
                        default:
                    }
                }
            } else {
                result = Long.parseLong(value);
                if (!isbyte) {
                    result = result / 1024 / 1024 / 1024;
                }
            }
            return result;
        }

        private String getKubeConfigFilePath() {
            if (System.getProperty("os.name").contains("Windows")) {
                return KUBE_CONFIG_WINDOWS_PATH;
            } else {
                return KUBE_CONFIG_LINUX_PATH;
            }
        }

        private String getKubeConfigFileName(String filePath, long clusterId) {
            if (System.getProperty("os.name").contains("Windows")) {
                return String.format("%s\\%s_kubeconfig", filePath, String.valueOf(clusterId));
            } else {
                return String.format("%s/%s_kubeconfig", filePath, String.valueOf(clusterId));
            }
        }

        private void checkKubeconfigFile(Cluster cluster) {
            String clusterK8sConfig = cluster.getK8sConfig();
            String filePath = getKubeConfigFilePath();
            String fileName = getKubeConfigFileName(filePath, cluster.getId());

            File folder = new File(filePath);
            if (!folder.exists()) {
                try {
                    folder.mkdir();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            File file = new File(fileName);
            if (!file.exists()) {
                try {
                    file = new File(fileName);
                    try (FileWriter fw = new FileWriter(file, false)) {
                        fw.write(clusterK8sConfig);
                        fw.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                log.info("Kubeconfig file exists");
            }
        }
    }
}
