package me.nald.blog.adaptor;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.model.Cluster;
import me.nald.blog.model.JsonPatch;
import me.nald.blog.util.KubeConfigValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.error.YAMLException;

import static me.nald.blog.util.Constants.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("kubeAdaptor")
@Slf4j
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

    @Autowired
    public KubernetesAdaptor() {
        // Do nothing
    }

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
        return KUBERNETES_NAMESPACE_PREFIX + name;
    }


    private static void cmdResult(String cmd, KubectlDetail result) throws Exception {
//        System.out.println("완성된 cmd는"+ cmd);
        Process process = Runtime.getRuntime().exec(cmd);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println("★★★"+line);
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
            System.out.println("데이터+"+data);

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
                    System.out.println(dataList);
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
                                restartCount += (int)container.get("restartCount");
                                if ((boolean)container.get("ready")) {
                                    readyCount++;
                                }
                                if (reason == null && !"Running".equals(container.get("state"))) {
                                    Map<String, String> subData =  (Map)container.get("stateData");
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
                            summaryPod.put("usage", (long)summaryPod.get("usage") + getPodCount("Non-terminated Pods"));
                            Map<String, String> capacity = getMapData("Capacity", ":");
                            summaryCpu.put("capacity", (long)summaryCpu.get("capacity") + convertToLong(String.valueOf(capacity.get("cpu")), CONVERT_TYPE_CORE));
                            summaryMemory.put("capacity", (long)summaryMemory.get("capacity") + convertToLong(String.valueOf(capacity.get("memory")), CONVERT_TYPE_BYTE));
                            summaryPod.put("capacity", (long)summaryPod.get("capacity") + Integer.parseInt(String.valueOf(capacity.get("pods"))));
                            Map<String, Object> resourceMap = getResource("Allocated resources", resources);
                            Map<String, Object> cpuMap = (Map<String, Object>)resourceMap.get("cpu");
                            summaryCpu.put("requests", (long)summaryCpu.get("requests") + convertToLong(String.valueOf(cpuMap.get("requests")).split(" ")[0], CONVERT_TYPE_CORE));
                            summaryCpu.put("limits", (long)summaryCpu.get("limits") + convertToLong(String.valueOf(cpuMap.get("limits")).split(" ")[0], CONVERT_TYPE_CORE));
                            Map<String, Object> memoryMap = (Map<String, Object>)resourceMap.get("memory");
                            summaryMemory.put("requests", (long)summaryMemory.get("requests") + convertToLong(String.valueOf(memoryMap.get("requests")).split(" ")[0], CONVERT_TYPE_BYTE));
                            summaryMemory.put("limits", (long)summaryMemory.get("limits") + convertToLong(String.valueOf(memoryMap.get("limits")).split(" ")[0], CONVERT_TYPE_BYTE));
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
            if (TimeUnit.MILLISECONDS.toDays(runningTime) > 0 ) {
                result = String.format("%dd", TimeUnit.MILLISECONDS.toDays(runningTime));
            } else if (TimeUnit.MILLISECONDS.toHours(runningTime) > 0 ) {
                result = String.format("%dh", TimeUnit.MILLISECONDS.toHours(runningTime));
            } else if (TimeUnit.MILLISECONDS.toMinutes(runningTime) > 0 ) {
                result = String.format("%dm", TimeUnit.MILLISECONDS.toMinutes(runningTime));
            } else if (TimeUnit.MILLISECONDS.toSeconds(runningTime) > 0 ) {
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
                        Map<String, String> subData = (Map)container.get(String.format("%s_sub", lastSubKey));
                        if (subData == null) {
                            subData = new LinkedHashMap<>();
                            container.put(String.format("%s_sub", lastSubKey), subData);
                        }
                        subData.put(str.substring(0, str.indexOf(":")).trim(), str.substring(str.indexOf(":") + 1).trim());
                    }
                    if (key != null && str.contains(":")) {
                        Map<String, Object> data = (Map)container.get(key);
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
            summaryCpu.put("capacity", (long)summaryCpu.get("capacity") * 1000);
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
                    KubeConfigValidator.validate(kubeConfig);
                } catch (IOException | YAMLException | ClassCastException e) {
                    log.error("apiClient", e);
                    String message = String.format("K8S-Config 로드가 실패했습니다.(%s)", e.getMessage());
                    throw new RuntimeException(e);
                }
                try {
                    client = Config.fromConfig(kubeConfig);
                } catch (IOException | RuntimeException e) {
                    log.error("apiClient", e);
                    String message = String.format("Cluster의 K8S-Config가 유효하지 않은 값으로 설정되어 있습니다. Cluster Owner 에게 문의하세요.(%s)", e.getMessage());
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


        public  List<Map<String, Object>> getNodeSummary() throws Exception {
            String cmd = String.format("kubectl get nodes");
            KubectlDetail summary = new KubectlDetail(KubectlDetail.NODE);
            cmdResult(cmd, summary);
            System.out.println(summary);
//            Map<String, Object> result = summary.getSummary();
            cmd = String.format("kubectl top nodes");
            KubectlDetail usage = new KubectlDetail(KubectlDetail.NODE_USAGE);
            cmdResult(cmd, usage);
            List<Map<String, Object>> usageList = usage.getList();
            System.out.println("리스트"+ usageList);
            return usageList;
        }



        public V1NodeList listNode(String fieldSelector) throws ApiException {
            return coreV1Api().listNode(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
        }

        //        TODO sa-922
        public V1ConfigMap readNamespacedConfigMap(String configMapName, String namespace) throws ApiException {
            V1ConfigMap v1ConfigMap = coreV1Api().readNamespacedConfigMap(configMapName, namespace, "true", false, false);
            return v1ConfigMap;
        }

        public V1NamespaceList listNamespace(String fieldSelector) throws ApiException {
            return coreV1Api().listNamespace(STR_FALSE, null, null, null, fieldSelector, null, null, null, null, false);
        }

        public V1Namespace readNamespace(String namespace, boolean convert) throws ApiException {
            return coreV1Api().readNamespace(convert ? convertNamespaceName(namespace) : namespace, STR_FALSE, null, null);
        }

        public V1ResourceQuota readNamespacedResourceQuota(String name, boolean convert) throws ApiException {
            return coreV1Api().readNamespacedResourceQuota(RESOURCE_QUOTA_NAME, convert ? convertNamespaceName(name) : name, null, null, null);
        }

        public V1Namespace createNamespace(String name) throws ApiException {
            Map<String, String> labels = new HashMap<>();
            labels.put("katib-metricscollector-injection", "enabled");
            V1ObjectMeta meta = new V1ObjectMeta().labels(labels).name(convertNamespaceName(name));
            V1Namespace namespace = new V1Namespace().kind("Namespace").metadata(meta);
            return coreV1Api().createNamespace(namespace, STR_FALSE, null, null);
        }

        public ApiResponse<V1Status> deleteNamespace(String namespace) throws ApiException {
            V1DeleteOptions option = new V1DeleteOptions();
            return coreV1Api().deleteNamespaceWithHttpInfo(convertNamespaceName(namespace), STR_FALSE, null, null, null, STR_FALSE, option);
        }

        public V1ServiceAccount createNamespacedServiceAccount(String namespace, String name) throws ApiException {
            V1ServiceAccount serviceAccount = new V1ServiceAccount()
                    .kind("ServiceAccount").metadata(new V1ObjectMeta().name(name));
            return coreV1Api().createNamespacedServiceAccount(namespace, serviceAccount, STR_FALSE, null, null);
        }

        public V1ServiceAccount readNamespacedServiceAccount(String namespace, String name) throws ApiException {
            return coreV1Api().readNamespacedServiceAccount(name, namespace, STR_FALSE, null, null);
        }

        public V1ClusterRoleBinding createNamespacedClusterRoleBinding(String namespace, String name) throws ApiException {
            RbacAuthorizationV1Api rbacAuthorizationApi = rbacAuthorizationV1Api();
            V1ClusterRoleBinding body = new V1ClusterRoleBinding()
                    .kind("ClusterRoleBinding")
                    .metadata(new V1ObjectMeta().name(name))
                    .subjects(Arrays.asList(new V1Subject().kind("ServiceAccount").name("accu-modeler").namespace(namespace)))
                    .roleRef(new V1RoleRef().kind("ClusterRole").name("cluster-admin").apiGroup("rbac.authorization.k8s.io"));
            return rbacAuthorizationApi.createClusterRoleBinding(body, STR_FALSE, null, null);
        }

        private RbacAuthorizationV1Api rbacAuthorizationV1Api() {
            return new RbacAuthorizationV1Api(apiClient());
        }

        public V1Secret createSecret(String namespace, String dockerEndpoint, String dockerUsername, String dockerSecretPassword, String dockerEmail) throws ApiException, JSONException {
            Map<String, byte[]> data = new HashMap<>();
            JSONObject config = new JSONObject();
            JSONObject dockerInfo = new JSONObject()
                    .put("username", dockerUsername)
                    .put("password", dockerSecretPassword)
                    .put("email", dockerEmail);
            config.put("auths", new JSONObject().put(dockerEndpoint, dockerInfo));
            data.put(".dockerconfigjson", config.toString().getBytes());

            V1ObjectMeta metaData = new V1ObjectMeta().name("regcred").namespace(convertNamespaceName(namespace));
            V1Secret body = new V1Secret().metadata(metaData).type("kubernetes.io/dockerconfigjson").data(data);
            return coreV1Api().createNamespacedSecret(convertNamespaceName(namespace), body, STR_FALSE, null, null);
        }

        public V1SecretList listNamespaceSecret(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedSecret(namespace, STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public V1Secret readNamespaceSecret(String name, String namespace) throws ApiException {
            return coreV1Api().readNamespacedSecret(name, namespace, STR_TRUE, null, null);
        }

        public V1DeploymentList listNamespaceDeployment(String namespace) throws ApiException {
            return appsV1Api().listNamespacedDeployment(convertNamespaceName(namespace), STR_FALSE, true, "", "", "", null, "", null, 0, false);
        }

        public V1PodList listNamespacePod(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedPod(convertNamespaceName(namespace), STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public V1PodList listDefaultNamespacePod(String namespace, String labelSelector) throws ApiException {
            return coreV1Api().listNamespacedPod(namespace, STR_FALSE, true, "", "", labelSelector, null, "", null, 0, false);
        }

        public V1Pod readNamespacedPod(String namespace, String name) {
            V1Pod pod = null;
            try {
                pod = coreV1Api().readNamespacedPodStatus(name, convertNamespaceName(namespace), STR_TRUE);
            } catch (ApiException e) {
                e.printStackTrace();
                pod = readErrorPod(convertNamespaceName(namespace), name);
            }
            return pod;
        }

        public V1PodList listPodForAllNamespaces(String labelSelector) throws ApiException {
            return coreV1Api().listPodForAllNamespaces(false, null, null, labelSelector, null, null, null, null, null, false);
        }

        public V1ResourceQuotaList listNamespacedResourceQuota() throws ApiException {
            return coreV1Api().listResourceQuotaForAllNamespaces(false, null, RESOURCE_QUOTA_FIELD_SELECTOR, null, null, null, null, null, null, false);
        }

        private V1Pod readErrorPod(String namespace, String name) {
            V1Pod pod = null;
            try {
                V1PodList list = coreV1Api().listNamespacedPod(convertNamespaceName(namespace), STR_FALSE, true, "", "", null, null, "", null, 0, false);
                for (V1Pod p : list.getItems()) {
                    if (p.getMetadata().getName().startsWith(name)) {
                        pod = p;
                    }
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return pod;
        }

//        public V1EventList listNamespacedPodEvent(String namespace, String name) throws ApiException {
//            String fieldSelector = String.format("involvedObject.name=%s", name);
//            return coreV1Api().listNamespacedEvent(convertNamespaceName(namespace), STR_FALSE, null, null, fieldSelector, null, 100, null, null,10, false);
//        }

        // GPU 리소스 제한은 업데이트가 불가능 하여 디비로만 관리한다고 함, gpu 값 제거
        public V1ResourceQuota createNamespacedResourceQuota(String namespace, Float cpu, Float mem) throws ApiException {
            V1ObjectMeta meta = new V1ObjectMeta().name(RESOURCE_QUOTA_NAME);

            Map<String, Quantity> hard = new HashMap<>();
            hard.put(K8S_RESOURCE_QUOTA_LIMIT_CPU, new Quantity(cpuToCoreUnit(cpu)));
            hard.put(K8S_RESOURCE_QUOTA_LIMIT_MEMORY, new Quantity(memToCoreUnit(mem)));

            V1ResourceQuotaSpec spec = new V1ResourceQuotaSpec().hard(hard);
            V1ResourceQuota body = new V1ResourceQuota().kind("ResourceQuota").metadata(meta).spec(spec);

            return coreV1Api()
                    .createNamespacedResourceQuota(convertNamespaceName(namespace), body, STR_FALSE, null, null);
        }

        // GPU 리소스 제한은 업데이트가 불가능 하여 디비로만 관리한다고 함, gpu 값 제거
        public V1ResourceQuota patchNamespacedResourceQuota(String namespace, Float cpu, Float mem) throws ApiException {
            return coreV1Api().patchNamespacedResourceQuota(
                    RESOURCE_QUOTA_NAME,
                    convertNamespaceName(namespace),
                    new io.kubernetes.client.custom.V1Patch(
                            Arrays.asList(
                                    JsonPatch.replace("/spec/hard/limits.cpu", cpuToCoreUnit(cpu)),
                                    JsonPatch.replace("/spec/hard/limits.memory", memToCoreUnit(mem))
                            ).toString()),
                    STR_FALSE,
                    null,
                    null,
                    null
            );
        }

        /*
        private String gpuToCoreUnit(Float gpu) {
            return Math.round(gpu) + K8S_GPU_UNIT;
        }
        */

        private String cpuToCoreUnit(Float cpu) {
            return Math.round(cpu * 1000) + K8S_CPU_UNIT;
        }

        private String memToCoreUnit(Float memory) {
            return Math.round(memory * 1000) + K8S_MEMORY_UNIT;
        }

        /*
        private int cpuToCoreValue(Float cpu) {
            return Math.round(cpu * 1000);
        }

        private int memToCoreValue(Float memory) {
            return Math.round(memory * 1000);
        }

        private Float CoreUnitToCpu(String cpu) {
            if (cpu.indexOf(K8S_CPU_UNIT) != -1) {
                return Float.valueOf(cpu.replace(K8S_CPU_UNIT, "")).floatValue() / 1000;
            } else {
                return Float.valueOf(cpu).floatValue();
            }
        }

        private Float CoreUnitToMem(String mem) {
            return Float.valueOf(mem.replace(K8S_MEMORY_UNIT, "")).floatValue() / 1000;
        }
        */

        public V1Status deleteNamspacedSecret(String namespace) {
            V1DeleteOptions body = new V1DeleteOptions();
            try {
                return coreV1Api().deleteNamespacedSecret("regcred", convertNamespaceName(namespace), STR_FALSE, null, 0, false, STR_TRUE, body);
            } catch (ApiException ignore) {
                log.error("deleteNamspacedSecret {}", ignore);
                return null;
            }
        }

        public Float getMaximumGpuResourceCapacity() throws ApiException {
            Float max = 0F;

            V1NodeList nodeList = listNode("");
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1 && node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master")) {
                    continue;
                }

                Float gpu = Optional.ofNullable(node)
                        .map(V1Node::getStatus)
                        .map(V1NodeStatus::getCapacity)
                        .map(capa -> capa.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU))
                        .map(Quantity::getNumber)
                        .map(BigDecimal::floatValue)
                        .orElse(0F);

                max = (max.compareTo(gpu) > 0) ? max : gpu;
            }
            return max;
        }

        public Map<String, List<Float>> getClusterResource() throws ApiException, IOException {
            HashMap<String, List<Float>> result = new HashMap<>();

            V1NodeList nodeList = KubernetesAdaptor.agentWith().listNode("");
//            System.out.println("노드리스트 ="+nodeList);
            for (V1Node node : nodeList.getItems()) {
                if (nodeList.getItems().size() > 1
                        && (node.getMetadata().getLabels().containsKey(KUBERNETES_LABEL_KEY_SERVICE_TYPE)
                        && (node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE)
                        || node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE)))) {
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
                        && (node.getMetadata().getLabels().containsKey(KUBERNETES_LABEL_KEY_SERVICE_TYPE)
                        && (node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE)
                        || node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE)))) {
                    List<Float> value = getAllResourceNode(node.getMetadata().getName(), cId);
                    result.put(node.getMetadata().getName(), value);
                }
            }
            return result;
        }

        public Map<String, List<Object>> getUsageResource(String namespaces, Cluster cluster, String podType, String type) throws IOException {
            long cId = cluster.getId();
            checkKubeconfigFile(cluster);
            return getResourcelimitsOfPods(namespaces, cId, podType, type);
        }

        public Float getUsedNodeResource(String type, Cluster cluster) throws ApiException, IOException {
            Float result = 0F;
            long cId = cluster.getId();
            checkKubeconfigFile(cluster);
            V1NodeList nodeList = KubernetesAdaptor.agentWith(cluster).listNode("");
            // 노드가 두개 이상(마스터 제외한 노드가 있을 때)
            if (nodeList.getItems().size() > 1) {
                for (V1Node node : nodeList.getItems()) {
                    if (node.getMetadata().getLabels().containsKey(KUBERNETES_LABEL_KEY_SERVICE_TYPE) && isNodeTypeWithResource(type, node)) {
                        result += getResourcelimitsOfNode(node.getMetadata().getName(), cId, type);
                    }
                }
            }
            return result;
        }

        private boolean isNodeTypeWithResource(String type, V1Node node) {
            return (type.equals(K8S_RESOURCE_TYPE_CPU) && node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE))
                    || (type.equals(K8S_EXTENDED_RESOURCE_TYPE_GPU) && node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE))
                    || (type.equals(K8S_RESOURCE_TYPE_MEMORY) && (node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_CPU_NODE_SERVICE_TYPE)
                    || node.getMetadata().getLabels().get(KUBERNETES_LABEL_KEY_SERVICE_TYPE).equals(KUBERNETES_GPU_NODE_SERVICE_TYPE)));
        }

        private Map<String, List<Object>> getResourcelimitsOfPods(String namespace, long cId, String podyType, String type) throws IOException {
            HashMap<String, List<Object>> dataMap = new LinkedHashMap<>();
            String key = null;
            String fileName = getKubeConfigFileName(getKubeConfigFilePath(), cId);

            String cmd = String.format("kubectl top pod -n %s --kubeconfig=%s", convertNamespaceName(namespace), fileName);
            Process process = Runtime.getRuntime().exec(cmd);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.indexOf(podyType) == 0) {
                        String[] splitData = line.split("\\s+");
                        key = line.substring(0, line.indexOf(" "));
                        List<Object> dataList = dataMap.get(key);
                        if (dataList == null) {
                            dataList = new ArrayList<>();
                            dataMap.put(key, dataList);
                        }
                        if (type.equals(K8S_RESOURCE_TYPE_CPU)) {
                            dataList.add("NA");
                            dataList.add(String.valueOf(convertToFloat(splitData[1], true, false)));
                        } else if (type.equals(K8S_RESOURCE_TYPE_MEMORY)) {
                            dataList.add("NA");
                            dataList.add(String.valueOf(convertToFloat(splitData[2], false, true)));
                        } else if (type.equals(K8S_EXTENDED_RESOURCE_TYPE_GPU)) {
                            dataList.add("NA");
                            dataList.add("0");
                        } else {
                            log.error("error : Type is unknown");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return dataMap;
        }

        private Float getResourcelimitsOfNode(String nodeName, long cId, String type) throws IOException {
            Float result = 0F;
            Map<String, List<String>> dataMap = new LinkedHashMap<>();
            String key = null;
            String fileName = getKubeConfigFileName(getKubeConfigFilePath(), cId);

            String cmd = String.format("kubectl describe node %s --kubeconfig=%s", nodeName, fileName);
            Process process = Runtime.getRuntime().exec(cmd);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("name") == 0) {
                        dataMap = new LinkedHashMap<>();
                    }
                    if (dataMap != null) {
                        if (line.length() > 0) {
                            key = addDataToDataMap(line, dataMap, key);
                        }
                        if (line.indexOf("Events:") == 0) {
                            result = processEvents(type, dataMap);
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

        private String addDataToDataMap(String line, Map<String, List<String>> dataMap, String key) {
            String[] splitData = line.split(":");
            if (line.charAt(0) != ' ') {
                key = splitData[0];
            }
            List<String> dataList = dataMap.computeIfAbsent(key, k -> new ArrayList<>());
            dataList.add(line);
            return key;
        }

        private Float processEvents(String type, Map<String, List<String>> dataMap) {
            List<String> resource = Arrays.asList(K8S_RESOURCE_TYPE_CPU, K8S_RESOURCE_TYPE_MEMORY, K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
//            Map<String, String> capacity = getMapData("Capacity", ":", dataMap);
            Map<String, Map<String, String>> allocated = getUsedData("Allocated resources", resource, dataMap);
            Map<String, String> cpuUsed = allocated.get(K8S_RESOURCE_TYPE_CPU);
            Map<String, String> memoryUsed = allocated.get(K8S_RESOURCE_TYPE_MEMORY);
            Map<String, String> gpuUsed = allocated.get(K8S_EXTENDED_RESOURCE_TYPE_NVIDIA_GPU);
            if (type.equals(K8S_RESOURCE_TYPE_CPU)) {
                return convertToFloat(String.valueOf(cpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], true, false);
            } else if (type.equals(K8S_RESOURCE_TYPE_MEMORY)) {
                return convertToFloat(String.valueOf(memoryUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0], false, false);
            } else if (type.equals(K8S_EXTENDED_RESOURCE_TYPE_GPU)) {
                if (gpuUsed != null) {
                    return Float.parseFloat(String.valueOf(gpuUsed.get(K8S_RESOURCE_REQUESTS)).split(" ")[0].substring(0, 1));
                }
            } else {
                log.error("error : Type is unknown");
            }
            return 0f;
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
